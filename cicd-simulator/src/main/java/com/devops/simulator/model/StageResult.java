package com.devops.simulator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures the result of a single pipeline stage execution.
 * Mirrors what Jenkins stores per-stage in a pipeline run.
 */
public class StageResult {

    private final PipelineStage stage;
    private PipelineStatus status;
    private final List<String> logs;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    public StageResult(PipelineStage stage) {
        this.stage = stage;
        this.status = PipelineStatus.PENDING;
        this.logs = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }

    public void markRunning() {
        this.status = PipelineStatus.RUNNING;
    }

    public void markSuccess() {
        this.status = PipelineStatus.SUCCESS;
        this.endTime = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = PipelineStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.logs.add("[FATAL] " + reason);
    }

    public void addLog(String message) {
        this.logs.add(message);
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    // ── Getters ─────────────────────────────────────────────
    public PipelineStage getStage()       { return stage; }
    public PipelineStatus getStatus()     { return status; }
    public List<String> getLogs()         { return logs; }
    public LocalDateTime getStartTime()   { return startTime; }
    public LocalDateTime getEndTime()     { return endTime; }
    public long getDurationMs()           { return durationMs; }
}
