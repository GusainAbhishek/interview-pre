package com.devops.simulator.kubernetes;

import com.devops.simulator.model.PipelineStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates a Kubernetes Pod.
 * Tracks restart count and environment to reproduce
 * the CrashLoopBackOff scenario accurately.
 */
public class Pod {

    private final String name;
    private final String image;
    private PodPhase phase;
    private int restartCount;
    private final Map<String, String> environment;
    private String lastExitReason;

    public enum PodPhase {
        PENDING, RUNNING, CRASH_LOOP_BACK_OFF, COMPLETED, TERMINATING
    }

    public Pod(String name, String image) {
        this.name = name;
        this.image = image;
        this.phase = PodPhase.PENDING;
        this.restartCount = 0;
        this.environment = new HashMap<>();
    }

    public void injectEnv(Map<String, String> envVars) {
        if (envVars != null) {
            this.environment.putAll(envVars);
        }
    }

    /**
     * Simulates the container startup check.
     * App exits with code 1 if required env vars are absent —
     * which is exactly what triggers CrashLoopBackOff.
     */
    public boolean startContainer(String[] requiredEnvVars) {
        for (String key : requiredEnvVars) {
            if (!environment.containsKey(key) || environment.get(key).isBlank()) {
                lastExitReason = "Exit code 1 — Missing required environment variable: " + key;
                restartCount++;
                if (restartCount >= 3) {
                    phase = PodPhase.CRASH_LOOP_BACK_OFF;
                } else {
                    phase = PodPhase.PENDING; // backing off before next restart
                }
                return false;
            }
        }
        phase = PodPhase.RUNNING;
        return true;
    }

    public void terminate() {
        this.phase = PodPhase.TERMINATING;
    }

    // ── Getters ─────────────────────────────────────────────
    public String getName()              { return name; }
    public String getImage()             { return image; }
    public PodPhase getPhase()           { return phase; }
    public int getRestartCount()         { return restartCount; }
    public Map<String, String> getEnv()  { return environment; }
    public String getLastExitReason()    { return lastExitReason; }
}
