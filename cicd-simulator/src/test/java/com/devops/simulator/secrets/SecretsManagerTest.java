package com.devops.simulator.secrets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecretsManager Tests")
class SecretsManagerTest {

    private SecretsManager secretsManager;

    @BeforeEach
    void setUp() {
        secretsManager = new SecretsManager();
    }

    @Test
    @DisplayName("Should retrieve valid Jenkins credential")
    void shouldRetrieveJenkinsCredential() {
        String dbHost = secretsManager.getJenkinsCredential("db-host");
        assertNotNull(dbHost);
        assertFalse(dbHost.isBlank());
    }

    @Test
    @DisplayName("Should throw for missing Jenkins credential")
    void shouldThrowForMissingCredential() {
        assertThrows(IllegalArgumentException.class, () ->
            secretsManager.getJenkinsCredential("non-existent-cred"));
    }

    @Test
    @DisplayName("Should create and resolve Kubernetes Secret")
    void shouldCreateAndResolveKubernetesSecret() {
        Map<String, String> data = Map.of(
            "DB_HOST", "prod-db.internal",
            "API_KEY", "s3cr3t"
        );

        secretsManager.createKubernetesSecret("my-app-secrets", data);
        assertTrue(secretsManager.secretExists("my-app-secrets"));

        Map<String, String> resolved = secretsManager.resolveKubernetesSecret("my-app-secrets");
        assertNotNull(resolved);
        assertEquals("prod-db.internal", resolved.get("DB_HOST"));
        assertEquals("s3cr3t", resolved.get("API_KEY"));
    }

    @Test
    @DisplayName("Should return null for non-existent K8s Secret (causes CrashLoopBackOff)")
    void shouldReturnNullForMissingK8sSecret() {
        Map<String, String> result = secretsManager.resolveKubernetesSecret("missing-secret");
        assertNull(result); // null → pod starts without env vars → crash
    }

    @Test
    @DisplayName("Mask should hide most of the secret value")
    void shouldMaskSecretValue() {
        String masked = SecretsManager.mask("supersecret");
        assertTrue(masked.contains("*"));
        assertFalse(masked.equals("supersecret"));
    }
}
