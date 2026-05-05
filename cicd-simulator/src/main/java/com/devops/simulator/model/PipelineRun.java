package com.devops.simulator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single CI/CD pipeline run — the equivalent of
 * one Jenkins build number.
 */
public class PipelineRun {

    private final int buildNumber;
    private final String serviceName;
    private final String gitBranch;
    private final String commitHash;
    private PipelineStatus overallStatus;
    private final List<StageResult> stageResults;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    public PipelineRun(int buildNumber, String serviceName, String gitBranch, String commitHash) {
        this.buildNumber   = buildNumber;
        this.serviceName   = serviceName;
        this.gitBranch     = gitBranch;
        this.commitHash    = commitHash;
        this.overallStatus = PipelineStatus.PENDING;
        this.stageResults  = new ArrayList<>();
        this.startTime     = LocalDateTime.now();
    }

    public void addStageResult(StageResult result) {
        stageResults.add(result);
    }

    public void finish(PipelineStatus finalStatus) {
        this.overallStatus = finalStatus;
        this.endTime = LocalDateTime.now();
    }

    public boolean hasFailed() {
        return stageResults.stream()
                .anyMatch(r -> r.getStatus() == PipelineStatus.FAILED);
    }

    // ── Getters ─────────────────────────────────────────────
    public int getBuildNumber()               { return buildNumber; }
    public String getServiceName()            { return serviceName; }
    public String getGitBranch()              { return gitBranch; }
    public String getCommitHash()             { return commitHash; }
    public PipelineStatus getOverallStatus()  { return overallStatus; }
    public List<StageResult> getStageResults(){ return stageResults; }
    public LocalDateTime getStartTime()       { return startTime; }
    public LocalDateTime getEndTime()         { return endTime; }
}
