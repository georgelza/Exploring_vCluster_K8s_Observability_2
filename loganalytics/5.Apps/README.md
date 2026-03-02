## Demo applications

We have <planned> 3 demo applications, doing the exact same, generating fake prometheus metrics, but in packaged in 3 different common programming languages.

- [Python](https://www.python.org)

- [Java](https://www.java.com/en/)

- [Golang](https://go.dev)

Each will expose the various types of metrics that Prometheus managers.

- info
- counter
- summary
- histogram
- guage

Each directory have a `app-build`, `dashboard` and `app-deploy` subdirectory.

- The `app-build` will build the container and push it into the local docker registry.

- The `app-deploy` contains the manifest files to deploy the application onto our Kubernetes cluster.

- The `dashboard` directory contains a JSON file (app-dash.json) that can be imported into the Grafana server to visualize the metrics being generated.

See the sub directory for the relevant `README.md` for more information

### Directory Structure

```
loganalytics/
└── Apps/
   |
   ├── PythonApp/
   |   ├── app-build/
   |   ├── dashboard/
   |   ├── k8s-deploy/
   |   └── README.md
   |
   ├── JavaApp/
   |   ├── app-build/
   |   ├── dashboard/
   |   ├── k8s-deploy/
   |   └── README.md
   |
   ├── GolangApp/
   |   ├── app-build/
   |   ├── dashboard/
   |   ├── k8s-deploy/
   |   └── README.md
   |
   └── README.md     <- This file
```

## Verifying deployment

### Forward local port 8000 to the pod

**Option 1**

```bash
# Python App as Example, see local README.md
kubectl port-forward -n prometheus-demo deployment/python-prometheus-demo 8000:8000
```

Then in a second terminal

```bash
curl http://localhost:8000/metrics
```

**Option 2** — curl directly from inside the pod

```bash
kubectl exec -n prometheus-demo deployment/python-prometheus-demo -- \
curl -s http://localhost:8000/metrics
```

**Option 3** — check logs for structured output

```bash
# Tail live
kubectl logs -n prometheus-demo deployment/python-prometheus-demo -f

# Last 20 lines
kubectl logs -n prometheus-demo deployment/python-prometheus-demo --tail=20
```

You should see JSON lines with event: startup followed by event: loop_tick every 1–5 seconds.

**Option 4** — describe the pod (confirm env vars injected from ConfigMap)

```bash
kubectl describe pod -n prometheus-demo -l app.kubernetes.io/name=python-prometheus-demo
```

Look for the `Environment Variables from` section confirming `python-prometheus-demo-config` is mounted, and `Events` at the bottom for any pull/start errors.


**What healthy output looks like from `curl`:**

```bash
# HELP random_number Random number generated each loop
# TYPE random_number gauge
random_number 7.0
# HELP loop_total_total Total number of while loop iterations executed
# TYPE loop_total_total counter
loop_total_total 4.0
# HELP loop_pct_complete Current loop count as % of MaxRun
# TYPE loop_pct_complete gauge
loop_pct_complete 0.0
# HELP loop_execution_seconds Histogram of loop execution times
# TYPE loop_execution_seconds histogram
loop_execution_seconds_bucket{le="1.0"} 0.0
loop_execution_seconds_bucket{le="2.0"} 1.0
...
```
  

### Deployment Pattern/Distribution

```
              Python                                  Go                                      Java  
App name      python-prometheus-demo                  golang-prometheus-demo                  java-prometheus-demo 
Namespace     prometheus-demo                         prometheus-demo                         prometheus-demo
Config Map    python-prometheus-demo-config           golang-prometheus-demo-config           java-prometheus-demo-config
Image         REPO_NAME/python-prometheus-demo:latest REPO_NAME/golang-prometheus-demo:latest REPO_NAME/java-prometheus-demo:latest
APP_NAME      default python-prometheus-demo          golang-prometheus-demo                  java-prometheus-demo
Dashboard UID python-prometheus-demo-v1               golang-prometheus-demo-v1               java-prometheus-demo-v1 / java-prometheus-demo-full-v1
```

### Logs 

All three are correct and consistent. The app field in every log line is sourced from the APP_NAME environment variable in all three languages, which is injected from the ConfigMap at runtime:

```
                  App field in logs                 Source
Python            record["app"] = APP_NAME          os.environ.get("APP_NAME", "python-prometheus-demo")
Go                "app": appName                    getEnv("APP_NAME", "golang-prometheus-demo")
Java              record.put("app", APP_NAME)       getEnv("APP_NAME", "java-prometheus-demo")
```