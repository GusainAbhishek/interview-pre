package com.devops.simulator.pipeline;

import com.devops.simulator.docker.DockerService;
import com.devops.simulator.kubernetes.KubernetesService;
import com.devops.simulator.model.PipelineConfig;
import com.devops.simulator.secrets.SecretsManager;

/**
 * Factory that wires together all pipeline dependencies.
 * Simple dependency injection without a framework.
 *
 * In production, Spring Boot / Guice would handle this.
 */
public class PipelineFactory {

    public static CiCdPipeline createBrokenPipeline() {
        PipelineConfig config = PipelineConfig.builder()
            .serviceName("order-service")
            .gitBranch("main")
            .dockerRegistry("myregistry.io")
            .kubernetesNamespace("production")
            .replicaCount(2)
            .injectSecrets(false)        // ← BUG: no secrets injected
            .secretsMissing(false)
            .build();

        return wire(config);
    }

    public static CiCdPipeline createFixedPipeline() {
        PipelineConfig config = PipelineConfig.builder()
            .serviceName("order-service")
            .gitBranch("main")
            .dockerRegistry("myregistry.io")
            .kubernetesNamespace("production")
            .replicaCount(2)
            .injectSecrets(true)         // ← FIX: secrets injected from Jenkins + K8s Secret
            .secretsMissing(false)
            .build();

        return wire(config);
    }

    private static CiCdPipeline wire(PipelineConfig config) {
        SecretsManager secrets = new SecretsManager();
        DockerService docker   = new DockerService(config.getDockerRegistry());
        KubernetesService k8s  = new KubernetesService(config.getKubernetesNamespace());
        return new CiCdPipeline(config, secrets, docker, k8s);
    }
}
