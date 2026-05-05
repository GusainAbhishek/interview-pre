Command Sequence (Run in Order)
1.	Check pod status and restart count
kubectl get pods -n default
# Look for: STATUS=CrashLoopBackOff, RESTARTS > 2

2.	Read the crash logs
kubectl logs <pod-name>
kubectl logs <pod-name> --previous   # logs from previous crashed instance
# Expected output: FATAL: Missing required env variables

3.	Describe the pod for event trail
kubectl describe pod <pod-name>
# Check Events section at the bottom:
# Warning  BackOff  Restarting failed container

4.	Inspect the running container's environment
kubectl exec -it <pod-name> -- env | grep -E 'DB_HOST|API_KEY'
# Output: (empty) — variables are missing

5.	Verify the Kubernetes deployment spec
kubectl get deployment my-node-app -o yaml | grep -A20 'env'
# Shows: no env block defined

6.	Trace back to Jenkins — what the build actually ran
# In Jenkins: Open the failed build → Console Output
# Confirm: no env vars injected during docker build or kubectl apply

****-------------- after fix the issue -------------------------*****
Run These Commands After Deploying
7.	Watch pods come up cleanly
kubectl rollout status deployment/my-node-app
# Expected: deployment "my-node-app" successfully rolled out

8.	Confirm no restarts
kubectl get pods
# Expected: STATUS=Running   RESTARTS=0

9.	Confirm env vars are available inside the pod
kubectl exec -it <pod-name> -- env | grep -E 'DB_HOST|API_KEY'
# Expected: DB_HOST=prod-db.internal  API_KEY=supersecretkey123

10.	Inspect the secret (base64 encoded — never plain text in etcd)
kubectl get secret my-app-secrets -o jsonpath='{.data.DB_HOST}' | base64 -d

11.	Check the Kubernetes events — no more BackOff
kubectl get events --sort-by='.metadata.creationTimestamp' | tail -20

