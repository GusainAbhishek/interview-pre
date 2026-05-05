package com.devops.simulator.kubernetes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pod Lifecycle Tests")
class PodTest {

    private static final String[] REQUIRED = {"DB_HOST", "API_KEY"};

    @Test
    @DisplayName("Pod should start successfully when all env vars are present")
    void shouldStartWhenEnvVarsPresent() {
        Pod pod = new Pod("order-service-abc12", "myregistry.io/order-service:42");
        pod.injectEnv(Map.of("DB_HOST", "prod-db.internal", "API_KEY", "s3cr3t"));

        boolean started = pod.startContainer(REQUIRED);

        assertTrue(started);
        assertEquals(Pod.PodPhase.RUNNING, pod.getPhase());
        assertEquals(0, pod.getRestartCount());
    }

    @Test
    @DisplayName("Pod should crash when env vars are missing — reproduces CrashLoopBackOff")
    void shouldCrashWhenEnvVarsMissing() {
        Pod pod = new Pod("order-service-xyz99", "myregistry.io/order-service:41");
        // No env vars injected — simulates broken pipeline

        boolean started = pod.startContainer(REQUIRED);

        assertFalse(started);
        assertNotNull(pod.getLastExitReason());
        assertTrue(pod.getLastExitReason().contains("Missing required environment variable"));
        assertTrue(pod.getRestartCount() > 0);
    }

    @Test
    @DisplayName("Pod reaches CrashLoopBackOff after multiple restarts")
    void shouldReachCrashLoopBackOffAfterRestarts() {
        Pod pod = new Pod("order-service-bad", "myregistry.io/order-service:41");

        // Simulate 3 restart attempts
        pod.startContainer(REQUIRED);
        pod.startContainer(REQUIRED);
        pod.startContainer(REQUIRED);

        assertEquals(Pod.PodPhase.CRASH_LOOP_BACK_OFF, pod.getPhase());
        assertEquals(3, pod.getRestartCount());
    }

    @Test
    @DisplayName("Pod with partial env vars should still crash")
    void shouldCrashWithPartialEnvVars() {
        Pod pod = new Pod("order-service-partial", "myregistry.io/order-service:41");
        pod.injectEnv(Map.of("DB_HOST", "prod-db.internal")); // API_KEY missing

        boolean started = pod.startContainer(REQUIRED);

        assertFalse(started);
        assertTrue(pod.getLastExitReason().contains("API_KEY"));
    }
}
