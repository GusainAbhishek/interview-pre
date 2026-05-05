# CI/CD Pipeline Simulator — Java Edition

A Java project that simulates a **Jenkins → Docker → Kubernetes** CI/CD pipeline,
with a focus on debugging **CrashLoopBackOff** caused by missing environment variables —
a classic DevOps interview scenario.

---

## Project Structure

```
cicd-simulator/
├── pom.xml
└── src/
    ├── main/java/com/devops/simulator/
    │   ├── Main.java                        ← Entry point / interactive menu
    │   ├── model/
    │   │   ├── PipelineConfig.java          ← Builder-pattern config (like Jenkinsfile params)
    │   │   ├── PipelineRun.java             ← One build = one PipelineRun
    │   │   ├── StageResult.java             ← Per-stage logs + status
    │   │   ├── PipelineStage.java           ← Enum of stages
    │   │   └── PipelineStatus.java          ← PENDING / RUNNING / SUCCESS / FAILED / etc.
    │   ├── pipeline/
    │   │   ├── CiCdPipeline.java            ← Core orchestrator (= Jenkinsfile in Java)
    │   │   └── PipelineFactory.java         ← Wires dependencies; creates broken/fixed configs
    │   ├── docker/
    │   │   └── DockerService.java           ← Simulates docker build + push
    │   ├── kubernetes/
    │   │   ├── KubernetesService.java       ← Simulates kubectl apply / rollout / rollback
    │   │   └── Pod.java                     ← Pod lifecycle incl. CrashLoopBackOff logic
    │   ├── secrets/
    │   │   └── SecretsManager.java          ← Jenkins credentials store + K8s Secrets
    │   └── util/
    │       ├── Logger.java                  ← ANSI-colored console output
    │       └── ReportPrinter.java           ← Final pipeline summary table
    └── test/java/com/devops/simulator/
        ├── secrets/SecretsManagerTest.java  ← Unit tests for secrets logic
        ├── kubernetes/PodTest.java          ← Unit tests for pod crash behaviour
        └── pipeline/CiCdPipelineTest.java   ← Integration tests: broken vs fixed
```

---

## How to Build & Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/cicd-simulator.jar
```

Then select:
- `1` — Broken pipeline (CrashLoopBackOff + rollback)
- `2` — Fixed pipeline (full success)
- `3` — Both side-by-side

### Run Tests
```bash
mvn test
```

---

## What It Simulates

### The Bug (Mode 1)
1. Jenkins clones the repo — `.env` is gitignored, not present in CI
2. Docker image is built without any environment variables baked in
3. `kubectl apply` runs a deployment with **no `env` block**
4. Pods start → app exits with code 1 (missing `DB_HOST`, `API_KEY`)
5. K8s restarts pods → `CrashLoopBackOff` after 3 restarts
6. Jenkins `post { failure {} }` triggers `kubectl rollout undo`

### The Fix (Mode 2)
1. Jenkins `withCredentials` block pulls `DB_HOST` and `API_KEY` from the credentials store
2. `kubectl create secret generic my-app-secrets` creates/updates the K8s Secret
3. `deployment.yaml` uses `envFrom: secretRef` to inject all keys from the Secret
4. Pods start with env vars present → `Running` ✓
5. `kubectl rollout status` confirms successful rollout
6. Health check passes → pipeline SUCCESS

---

## Key Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| Builder | `PipelineConfig` | Clean config construction |
| Factory | `PipelineFactory` | Encapsulates dependency wiring |
| Strategy (via lambda) | `CiCdPipeline.runStage()` | Each stage is a `StageTask` lambda |
| Enum | `PipelineStage`, `PipelineStatus` | Type-safe stage/status values |
| Separation of Concerns | Packages per domain | Docker / K8s / Secrets / Pipeline isolated |

---

## Interview-Ready Talking Points

**Q: Why did it fail in CI but work locally?**
> Locally, developers have a `.env` file. Jenkins clones only what's in Git.
> `.env` is in `.gitignore` for security, so CI never had it.

**Q: Why not bake secrets into the Docker image?**
> They appear in `docker history` and in any image layer inspection.
> Images are often pushed to registries — credentials would be exposed.

**Q: How do K8s Secrets work?**
> Values are base64-encoded in etcd (not encrypted by default — enable encryption at rest separately).
> Pods access them via `envFrom: secretRef` or `env[].valueFrom.secretKeyRef`.

**Q: How do you do zero-downtime rollback?**
> `kubectl rollout undo deployment/<name>` reverts to the previous ReplicaSet.
> This only works if images are tagged with build numbers — never use `:latest` in production.
