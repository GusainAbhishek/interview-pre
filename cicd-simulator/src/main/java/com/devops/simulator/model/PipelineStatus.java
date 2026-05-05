package com.devops.simulator.model;

/**
 * Status of a pipeline run or individual stage.
 */
public enum PipelineStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    ROLLED_BACK,
    SKIPPED
}
