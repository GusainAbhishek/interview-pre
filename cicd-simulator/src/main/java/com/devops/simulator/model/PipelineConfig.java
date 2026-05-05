package com.devops.simulator.model;

/**
 * Configuration object for a pipeline run.
 * In real Jenkins this comes from the Jenkinsfile + credentials store.
 *
 * Demonstrates the DevOps pattern of separating config from code.
 */
public class PipelineConfig {

    private String serviceName;
    private String gitBranch;
    private String dockerRegistry;
    private String kubernetesNamespace;
    private boolean injectSecrets;        // true = correct pattern; false = reproduces the bug
    private boolean secretsMissing;       // simulate missing K8s secret
    private int replicaCount;

    // Builder pattern — clean for config objects
    private PipelineConfig() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PipelineConfig config = new PipelineConfig();

        public Builder serviceName(String name)              { config.serviceName = name; return this; }
        public Builder gitBranch(String branch)              { config.gitBranch = branch; return this; }
        public Builder dockerRegistry(String registry)       { config.dockerRegistry = registry; return this; }
        public Builder kubernetesNamespace(String ns)        { config.kubernetesNamespace = ns; return this; }
        public Builder injectSecrets(boolean inject)         { config.injectSecrets = inject; return this; }
        public Builder secretsMissing(boolean missing)       { config.secretsMissing = missing; return this; }
        public Builder replicaCount(int replicas)            { config.replicaCount = replicas; return this; }

        public PipelineConfig build() {
            // sensible defaults
            if (config.kubernetesNamespace == null) config.kubernetesNamespace = "default";
            if (config.dockerRegistry == null)      config.dockerRegistry = "myregistry.io";
            if (config.replicaCount == 0)           config.replicaCount = 2;
            return config;
        }
    }

    // ── Getters ─────────────────────────────────────────────
    public String getServiceName()         { return serviceName; }
    public String getGitBranch()           { return gitBranch; }
    public String getDockerRegistry()      { return dockerRegistry; }
    public String getKubernetesNamespace() { return kubernetesNamespace; }
    public boolean isInjectSecrets()       { return injectSecrets; }
    public boolean isSecretsMissing()      { return secretsMissing; }
    public int getReplicaCount()           { return replicaCount; }
}
