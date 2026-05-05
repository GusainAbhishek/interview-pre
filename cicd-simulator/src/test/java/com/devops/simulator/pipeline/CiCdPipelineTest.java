package com.devops.simulator.pipeline;

import com.devops.simulator.model.PipelineRun;
import com.devops.simulator.model.PipelineStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pipeline Integration Tests")
class CiCdPipelineTest {

    @Test
    @DisplayName("Broken pipeline should fail with ROLLED_BACK status")
    void brokenPipelineShouldRollBack() {
        CiCdPipeline pipeline = PipelineFactory.createBrokenPipeline();
        PipelineRun result = pipeline.run(100);

        assertEquals(PipelineStatus.ROLLED_BACK, result.getOverallStatus());
        assertTrue(result.hasFailed());
    }

    @Test
    @DisplayName("Fixed pipeline should succeed with all pods Running")
    void fixedPipelineShouldSucceed() {
        CiCdPipeline pipeline = PipelineFactory.createFixedPipeline();
        PipelineRun result = pipeline.run(101);

        assertEquals(PipelineStatus.SUCCESS, result.getOverallStatus());
        assertFalse(result.hasFailed());
    }

    @Test
    @DisplayName("Fixed pipeline should execute all expected stages")
    void fixedPipelineShouldRunAllStages() {
        CiCdPipeline pipeline = PipelineFactory.createFixedPipeline();
        PipelineRun result = pipeline.run(102);

        // Checkout, Build, Test, DockerBuild, DockerPush, SecretInject, K8sDeploy, HealthCheck
        assertEquals(8, result.getStageResults().size());
    }

    @Test
    @DisplayName("Broken pipeline should have fewer successful stages due to deploy failure")
    void brokenPipelineShouldHaveRollbackStage() {
        CiCdPipeline pipeline = PipelineFactory.createBrokenPipeline();
        PipelineRun result = pipeline.run(103);

        long failedCount = result.getStageResults().stream()
            .filter(s -> s.getStatus() == PipelineStatus.FAILED)
            .count();

        assertTrue(failedCount >= 1, "At least the deploy stage should fail");
    }
}
