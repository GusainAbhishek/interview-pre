package com.devops.simulator.model;

/**
 * Represents each stage of a CI/CD pipeline.
 * Maps to real Jenkins pipeline stages.
 */
public enum PipelineStage {
    CHECKOUT("Source Checkout"),
    BUILD("Maven Build"),
    TEST("Unit Tests"),
    DOCKER_BUILD("Docker Image Build"),
    DOCKER_PUSH("Docker Image Push"),
    SECRET_INJECT("Secrets Injection"),
    K8S_DEPLOY("Kubernetes Deploy"),
    HEALTH_CHECK("Health Check"),
    ROLLBACK("Rollback");

    private final String displayName;

    PipelineStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
