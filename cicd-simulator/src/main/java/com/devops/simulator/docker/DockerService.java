package com.devops.simulator.docker;

import com.devops.simulator.model.PipelineConfig;
import com.devops.simulator.model.StageResult;
import com.devops.simulator.util.Logger;

import java.util.UUID;

/**
 * Simulates Docker CLI operations performed in a Jenkins pipeline:
 *   docker build -t registry/image:tag .
 *   docker push registry/image:tag
 *
 * INTERVIEW CONCEPT:
 *   - Images are tagged with the build number for traceability
 *   - :latest is avoided in production (no rollback capability)
 *   - Secrets are NEVER passed as --build-arg (they appear in docker history)
 */
public class DockerService {

    private final String registryUrl;

    public DockerService(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    /**
     * Simulates: docker build -t <registry>/<service>:<buildNum> .
     *
     * @param config       pipeline config
     * @param buildNumber  Jenkins build number used as image tag
     * @param result       stage result to write logs into
     * @return             the full image tag produced
     */
    public String buildImage(PipelineConfig config, int buildNumber, StageResult result) {
        String imageTag = buildImageTag(config.getServiceName(), buildNumber);

        Logger.step("docker build -t " + imageTag + " .");
        result.addLog("Reading Dockerfile...");
        result.addLog("Step 1/5 : FROM eclipse-temurin:17-jre-alpine");
        result.addLog("Step 2/5 : WORKDIR /app");
        result.addLog("Step 3/5 : COPY target/*.jar app.jar");
        result.addLog("Step 4/5 : EXPOSE 8080");
        result.addLog("Step 5/5 : ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]");

        // Simulate build ID (like a Docker image SHA)
        String imageId = "sha256:" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        result.addLog("Successfully built " + imageId);
        result.addLog("Successfully tagged " + imageTag);

        Logger.success("Image built: " + imageTag);
        return imageTag;
    }

    /**
     * Simulates: docker push <registry>/<service>:<buildNum>
     * Requires prior docker login (handled by SecretsManager credentials).
     */
    public void pushImage(String imageTag, StageResult result) {
        Logger.step("docker push " + imageTag);
        result.addLog("Pushing to registry: " + registryUrl);
        result.addLog("Layer 1/3: Pushing base image layers...");
        result.addLog("Layer 2/3: Pushing application jar...");
        result.addLog("Layer 3/3: Pushing metadata...");
        result.addLog("Digest: sha256:" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        result.addLog(imageTag + ": digest pushed successfully");
        Logger.success("Image pushed to registry");
    }

    /**
     * Produces the canonical image tag format:
     *   registry/service-name:buildNumber
     *
     * NEVER use :latest in production — you lose the ability to
     * do a clean rollback (kubectl rollout undo).
     */
    public String buildImageTag(String serviceName, int buildNumber) {
        return registryUrl + "/" + serviceName + ":" + buildNumber;
    }
}
