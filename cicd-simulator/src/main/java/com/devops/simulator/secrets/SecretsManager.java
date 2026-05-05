package com.devops.simulator.secrets;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulates a secrets manager — the combination of:
 *   - Jenkins Credentials Store  (withCredentials block)
 *   - Kubernetes Secrets          (kubectl create secret generic)
 *
 * In production this would integrate with HashiCorp Vault,
 * AWS Secrets Manager, or the Kubernetes external-secrets operator.
 *
 * KEY INTERVIEW CONCEPT:
 *   Secrets are stored base64-encoded (not plain text in YAML).
 *   They are injected at runtime — never baked into Docker images.
 */
public class SecretsManager {

    // Simulates the Jenkins credentials store
    private final Map<String, String> jenkinsCredentials = new HashMap<>();

    // Simulates Kubernetes Secrets (name -> {key -> base64 value})
    private final Map<String, Map<String, String>> kubernetesSecrets = new HashMap<>();

    public SecretsManager() {
        // Pre-load default credentials (like Jenkins admin would configure)
        jenkinsCredentials.put("db-host",      "prod-db.internal");
        jenkinsCredentials.put("api-key",       "s3cr3t-api-k3y-xyz");
        jenkinsCredentials.put("docker-user",   "devops-bot");
        jenkinsCredentials.put("docker-pass",   "docker-hub-token-abc");
    }

    /**
     * Simulates: withCredentials([string(credentialsId: 'db-host', variable: 'DB_HOST')])
     * Returns the secret value — masked in real Jenkins logs as ****
     */
    public String getJenkinsCredential(String credentialsId) {
        String value = jenkinsCredentials.get(credentialsId);
        if (value == null) {
            throw new IllegalArgumentException(
                "Credential not found in Jenkins store: " + credentialsId);
        }
        return value;
    }

    /**
     * Simulates:
     *   kubectl create secret generic <name> \
     *     --from-literal=KEY=value \
     *     --dry-run=client -o yaml | kubectl apply -f -
     */
    public void createKubernetesSecret(String secretName, Map<String, String> data) {
        Map<String, String> encoded = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            // K8s stores secrets as base64
            encoded.put(entry.getKey(), Base64.getEncoder().encodeToString(
                entry.getValue().getBytes()));
        }
        kubernetesSecrets.put(secretName, encoded);
        System.out.println("  [SecretsManager] kubectl apply → Secret '" + secretName + "' created/updated");
    }

    /**
     * Simulates: envFrom: secretRef — pulling all keys from a K8s Secret
     * into a pod's environment.
     */
    public Map<String, String> resolveKubernetesSecret(String secretName) {
        Map<String, String> encoded = kubernetesSecrets.get(secretName);
        if (encoded == null) {
            return null; // secret does not exist → CrashLoopBackOff scenario
        }
        // Decode before injecting into environment
        Map<String, String> decoded = new HashMap<>();
        for (Map.Entry<String, String> entry : encoded.entrySet()) {
            decoded.put(entry.getKey(),
                new String(Base64.getDecoder().decode(entry.getValue())));
        }
        return decoded;
    }

    public boolean secretExists(String secretName) {
        return kubernetesSecrets.containsKey(secretName);
    }

    /** Masks a secret value for safe logging — mimics Jenkins log masking */
    public static String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(0, 2) + "*".repeat(value.length() - 2);
    }
}
