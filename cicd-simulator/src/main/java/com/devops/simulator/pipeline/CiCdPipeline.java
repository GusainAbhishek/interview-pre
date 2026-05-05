package com.devops.simulator.pipeline;

import com.devops.simulator.docker.DockerService;
import com.devops.simulator.kubernetes.KubernetesService;
import com.devops.simulator.model.*;
import com.devops.simulator.secrets.SecretsManager;
import com.devops.simulator.util.Logger;
import com.devops.simulator.util.ReportPrinter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Core CI/CD Pipeline Orchestrator.
 *
 * This class represents the Jenkinsfile in Java form.
 * It executes stages in order, handles failures gracefully,
 * and demonstrates the secret injection pattern that solves
 * the CrashLoopBackOff problem.
 *
 * STAGE SEQUENCE:
 *   CHECKOUT → BUILD → TEST → DOCKER_BUILD → DOCKER_PUSH
 *   → SECRET_INJECT → K8S_DEPLOY → HEALTH_CHECK
 *
 * On deploy failure:
 *   → ROLLBACK  (simulates Jenkins post { failure { rollback() } })
 */
public class CiCdPipeline {

    private final PipelineConfig config;
    private final SecretsManager secretsManager;
    private final DockerService   dockerService;
    private final KubernetesService k8sService;

    private static final Random RANDOM = new Random();

    public CiCdPipeline(PipelineConfig config,
                         SecretsManager secretsManager,
                         DockerService dockerService,
                         KubernetesService k8sService) {
        this.config          = config;
        this.secretsManager  = secretsManager;
        this.dockerService   = dockerService;
        this.k8sService      = k8sService;
    }

    /**
     * Entry point — runs the full pipeline and returns the run result.
     * Equivalent to clicking "Build Now" in Jenkins.
     */
    public PipelineRun run(int buildNumber) {
        PipelineRun pipelineRun = new PipelineRun(
            buildNumber,
            config.getServiceName(),
            config.getGitBranch(),
            generateCommitHash()
        );

        Logger.banner("Pipeline: " + config.getServiceName() + " | Build #" + buildNumber);

        String imageTag = null;

        // ── STAGE: Checkout ──────────────────────────────────────────
        StageResult checkout = runStage(PipelineStage.CHECKOUT, () -> {
            Logger.step("git clone origin/" + config.getGitBranch());
            Logger.log("Cloning repository...");
            Logger.log("HEAD is at " + pipelineRun.getCommitHash());
            Logger.success("Checkout complete");
        });
        pipelineRun.addStageResult(checkout);
        if (checkout.getStatus() == PipelineStatus.FAILED) {
            return finishFailed(pipelineRun);
        }

        // ── STAGE: Build ─────────────────────────────────────────────
        StageResult build = runStage(PipelineStage.BUILD, () -> {
            Logger.step("mvn clean package -DskipTests=false");
            Logger.log("Compiling sources...");
            Logger.log("Building JAR: target/" + config.getServiceName() + ".jar");
            Logger.success("BUILD SUCCESS");
        });
        pipelineRun.addStageResult(build);
        if (build.getStatus() == PipelineStatus.FAILED) {
            return finishFailed(pipelineRun);
        }

        // ── STAGE: Test ───────────────────────────────────────────────
        StageResult test = runStage(PipelineStage.TEST, () -> {
            Logger.step("mvn test");
            Logger.log("Running 47 unit tests...");
            Logger.log("Tests run: 47, Failures: 0, Errors: 0, Skipped: 0");
            Logger.success("All tests passed");
        });
        pipelineRun.addStageResult(test);
        if (test.getStatus() == PipelineStatus.FAILED) {
            return finishFailed(pipelineRun);
        }

        // ── STAGE: Docker Build ───────────────────────────────────────
        final String[] imageTagHolder = new String[1];
        StageResult dockerBuild = runStage(PipelineStage.DOCKER_BUILD, () -> {
            imageTagHolder[0] = dockerService.buildImage(config, buildNumber, null);
        });
        pipelineRun.addStageResult(dockerBuild);
        imageTag = imageTagHolder[0];
        if (dockerBuild.getStatus() == PipelineStatus.FAILED) {
            return finishFailed(pipelineRun);
        }

        // ── STAGE: Docker Push ────────────────────────────────────────
        final String finalImageTag = imageTag;
        StageResult dockerPush = runStage(PipelineStage.DOCKER_PUSH, () -> {
            // Jenkins withCredentials block — credentials never appear in logs
            String dockerUser = secretsManager.getJenkinsCredential("docker-user");
            String dockerPass = secretsManager.getJenkinsCredential("docker-pass");
            Logger.step("echo **** | docker login -u " + dockerUser + " --password-stdin");
            Logger.log("Login Succeeded");
            dockerService.pushImage(finalImageTag, null);
        });
        pipelineRun.addStageResult(dockerPush);
        if (dockerPush.getStatus() == PipelineStatus.FAILED) {
            return finishFailed(pipelineRun);
        }

        // ── STAGE: Secret Injection ───────────────────────────────────
        // THIS is the fix for CrashLoopBackOff.
        // When injectSecrets=false, we simulate the broken pipeline.
        StageResult secretInject = runStage(PipelineStage.SECRET_INJECT, () -> {
            if (config.isInjectSecrets() && !config.isSecretsMissing()) {
                Logger.step("kubectl create secret generic my-app-secrets ...");

                // Pull secrets from Jenkins credentials store
                String dbHost = secretsManager.getJenkinsCredential("db-host");
                String apiKey = secretsManager.getJenkinsCredential("api-key");

                Logger.log("DB_HOST=" + SecretsManager.mask(dbHost));
                Logger.log("API_KEY=" + SecretsManager.mask(apiKey));

                // Create/update K8s secret
                Map<String, String> secretData = new HashMap<>();
                secretData.put("DB_HOST", dbHost);
                secretData.put("API_KEY", apiKey);
                secretsManager.createKubernetesSecret("my-app-secrets", secretData);

                Logger.success("Kubernetes Secret 'my-app-secrets' created/updated");
            } else {
                Logger.warn("Secret injection SKIPPED — pods will start without credentials");
                Logger.warn("(Reproducing CrashLoopBackOff scenario)");
            }
        });
        pipelineRun.addStageResult(secretInject);

        // ── STAGE: Kubernetes Deploy ──────────────────────────────────
        Map<String, String> envVars = null;
        if (config.isInjectSecrets() && !config.isSecretsMissing()) {
            envVars = secretsManager.resolveKubernetesSecret("my-app-secrets");
        }
        final Map<String, String> finalEnvVars = envVars;
        final String deployImageTag = imageTag;

        StageResult k8sDeploy = runStage(PipelineStage.K8S_DEPLOY, () -> {
            boolean success = k8sService.deploy(config, deployImageTag, finalEnvVars, null);
            if (!success) {
                throw new RuntimeException(
                    "Rollout failed — pods are in CrashLoopBackOff. " +
                    "Root cause: Missing environment variables (DB_HOST, API_KEY). " +
                    "Fix: Create K8s Secret and reference it via envFrom: secretRef in deployment.yaml");
            }
            k8sService.printPodStatus(config.getServiceName());
        });
        pipelineRun.addStageResult(k8sDeploy);

        // ── POST FAILURE: Rollback ────────────────────────────────────
        if (k8sDeploy.getStatus() == PipelineStatus.FAILED) {
            Logger.warn("Deploy failed — triggering automatic rollback (post { failure {} })");
            StageResult rollback = runStage(PipelineStage.ROLLBACK, () -> {
                k8sService.rollback(config.getServiceName(), null);
            });
            rollback.getStatus(); // already marked inside runStage
            pipelineRun.addStageResult(rollback);
            return finishRolledBack(pipelineRun);
        }

        // ── STAGE: Health Check ───────────────────────────────────────
        StageResult healthCheck = runStage(PipelineStage.HEALTH_CHECK, () -> {
            Logger.step("curl -f http://" + config.getServiceName() + "/actuator/health");
            Logger.log("HTTP 200 OK");
            Logger.log("{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"}}}");
            Logger.success("Health check passed");
        });
        pipelineRun.addStageResult(healthCheck);

        return finishSuccess(pipelineRun);
    }

    // ── Internal Stage Runner ─────────────────────────────────────────
    /**
     * Wraps stage execution with logging, timing, and error handling.
     * Equivalent to a Jenkins stage('name') { steps { ... } } block.
     */
    private StageResult runStage(PipelineStage stage, StageTask task) {
        StageResult result = new StageResult(stage);
        Logger.stage(stage.getDisplayName());
        result.markRunning();

        long start = System.currentTimeMillis();
        try {
            task.execute();
            result.setDurationMs(System.currentTimeMillis() - start);
            result.markSuccess();
        } catch (Exception e) {
            result.setDurationMs(System.currentTimeMillis() - start);
            result.markFailed(e.getMessage());
            Logger.error("Stage FAILED: " + e.getMessage());
        }

        return result;
    }

    @FunctionalInterface
    private interface StageTask {
        void execute() throws Exception;
    }

    // ── Finish Helpers ────────────────────────────────────────────────
    private PipelineRun finishSuccess(PipelineRun run) {
        run.finish(PipelineStatus.SUCCESS);
        Logger.banner("✓  PIPELINE SUCCESS — Build #" + run.getBuildNumber());
        ReportPrinter.print(run);
        return run;
    }

    private PipelineRun finishFailed(PipelineRun run) {
        run.finish(PipelineStatus.FAILED);
        Logger.banner("✗  PIPELINE FAILED — Build #" + run.getBuildNumber());
        ReportPrinter.print(run);
        return run;
    }

    private PipelineRun finishRolledBack(PipelineRun run) {
        run.finish(PipelineStatus.ROLLED_BACK);
        Logger.banner("↩  PIPELINE ROLLED BACK — Build #" + run.getBuildNumber());
        ReportPrinter.print(run);
        return run;
    }

    private String generateCommitHash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 7);
    }
}
