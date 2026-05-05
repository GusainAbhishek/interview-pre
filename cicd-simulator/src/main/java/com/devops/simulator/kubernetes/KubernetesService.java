package com.devops.simulator.kubernetes;

import com.devops.simulator.model.PipelineConfig;
import com.devops.simulator.model.StageResult;
import com.devops.simulator.util.Logger;

import java.util.*;

/**
 * Simulates kubectl operations against a Kubernetes cluster.
 *
 * Covers:
 *   kubectl apply -f deployment.yaml
 *   kubectl rollout status deployment/<name>
 *   kubectl rollout undo deployment/<name>
 *   kubectl logs <pod>
 *   kubectl describe pod <pod>
 *   kubectl get pods
 *
 * INTERVIEW CONCEPT:
 *   Deployment strategy used here is RollingUpdate (default in K8s).
 *   New pods must pass health checks before old ones are terminated.
 *   If new pods crash → rollout stalls → we trigger rollback.
 */
public class KubernetesService {

    // Simulates the cluster's current state
    private final Map<String, List<Pod>> deployments = new HashMap<>();
    private final String namespace;

    // Required env vars — the app will crash without these
    private static final String[] REQUIRED_ENV_VARS = {"DB_HOST", "API_KEY"};

    public KubernetesService(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Simulates: kubectl apply -f deployment.yaml
     * Creates or updates a Deployment with the given image.
     *
     * @param config     pipeline config
     * @param imageTag   Docker image to deploy
     * @param envVars    environment variables (from K8s Secret or null)
     * @param result     stage log collector
     * @return           true if rollout succeeded, false if CrashLoopBackOff
     */
    public boolean deploy(PipelineConfig config,
                          String imageTag,
                          Map<String, String> envVars,
                          StageResult result) {

        String deploymentName = config.getServiceName();
        Logger.step("kubectl apply -f deployment.yaml");
        result.addLog("namespace: " + namespace);
        result.addLog("image: " + imageTag);
        result.addLog("replicas: " + config.getReplicaCount());

        // Check if K8s Secret exists first (gate in a real pipeline)
        if (envVars == null) {
            result.addLog("[WARNING] No environment variables injected — pods will start without secrets");
        } else {
            result.addLog("envFrom: secretRef → my-app-secrets (keys: " + envVars.keySet() + ")");
        }

        // Create pods via rolling update
        List<Pod> newPods = new ArrayList<>();
        for (int i = 0; i < config.getReplicaCount(); i++) {
            String podName = deploymentName + "-" + UUID.randomUUID().toString().substring(0, 5);
            Pod pod = new Pod(podName, imageTag);
            pod.injectEnv(envVars);

            result.addLog("Creating pod: " + podName + " ...");
            boolean started = pod.startContainer(REQUIRED_ENV_VARS);

            if (!started) {
                result.addLog("[ERROR] Pod " + podName + " → " + pod.getLastExitReason());
                result.addLog("[ERROR] Status: " + pod.getPhase().name());
                result.addLog("  kubectl logs " + podName + " --previous →");
                result.addLog("  FATAL: Missing required env variables. Exiting.");
                result.addLog("  kubectl describe pod " + podName + " →");
                result.addLog("  Warning  BackOff  Restarting failed container");
                result.addLog("  Restarts: " + pod.getRestartCount() + "  Status: CrashLoopBackOff");
            } else {
                result.addLog("Pod " + podName + " → Running ✓");
            }
            newPods.add(pod);
        }

        deployments.put(deploymentName, newPods);

        // Rollout status check — all pods must be Running
        long runningCount = newPods.stream()
                .filter(p -> p.getPhase() == Pod.PodPhase.RUNNING)
                .count();

        if (runningCount < config.getReplicaCount()) {
            result.addLog("[ROLLOUT FAILED] Only " + runningCount + "/" +
                config.getReplicaCount() + " pods are Running");
            return false;
        }

        Logger.success("Rollout complete: " + runningCount + "/" +
            config.getReplicaCount() + " pods Running");
        return true;
    }

    /**
     * Simulates: kubectl rollout undo deployment/<name>
     * Reverts the Deployment to the previous ReplicaSet.
     * This is what the Jenkins post { failure {} } block triggers.
     */
    public void rollback(String deploymentName, StageResult result) {
        Logger.step("kubectl rollout undo deployment/" + deploymentName);
        result.addLog("Reverting to previous ReplicaSet...");

        List<Pod> pods = deployments.get(deploymentName);
        if (pods != null) {
            pods.forEach(Pod::terminate);
            result.addLog("Terminated " + pods.size() + " failing pod(s)");
        }
        result.addLog("Rollback complete — previous stable version is active");
        Logger.warn("Deployment rolled back to previous version");
    }

    /**
     * Simulates: kubectl get pods -n <namespace>
     * Prints a table matching real kubectl output format.
     */
    public void printPodStatus(String deploymentName) {
        System.out.println();
        System.out.println("  kubectl get pods -n " + namespace);
        System.out.printf("  %-45s %-8s %-22s %-8s%n",
            "NAME", "READY", "STATUS", "RESTARTS");
        System.out.println("  " + "-".repeat(85));

        List<Pod> pods = deployments.getOrDefault(deploymentName, Collections.emptyList());
        for (Pod pod : pods) {
            String ready  = pod.getPhase() == Pod.PodPhase.RUNNING ? "1/1" : "0/1";
            String status = pod.getPhase().name().replace("_", " ");
            System.out.printf("  %-45s %-8s %-22s %-8d%n",
                pod.getName(), ready, status, pod.getRestartCount());
        }
        System.out.println();
    }
}
