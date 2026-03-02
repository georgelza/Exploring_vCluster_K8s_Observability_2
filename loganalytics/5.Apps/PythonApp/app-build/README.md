## Python based prometheus-demo 

A lightweight Python app that generates Prometheus metrics and structured JSON logs.
Built as a learning/demo workload — runs in Kubernetes, scrapes cleanly into Prometheus and Grafana.


## What it does

Runs a continuous loop that sleeps for a random interval, then updates a set of Prometheus metrics.
All behaviour is driven by a ConfigMap — no code changes needed to tune it.


## Metrics exposed on `:8000/metrics`

| Metric | Type | Description |
|---|---|---|
| `app_info` | Info | App identity and config values sourced from the ConfigMap |
| `random_number` | Gauge | Random integer (1–10) generated each loop |
| `loop_pct_complete` | Gauge | Current iteration as % of `MAX_RUN` (0 when unlimited) |
| `loop_total` | Counter | Total loop iterations executed since startup |
| `loop_duration_seconds` | Summary | Elapsed time per loop — exposes `_count` and `_sum` |
| `loop_execution_seconds` | Histogram | Elapsed time per loop across 5 buckets: 1s, 2s, 3s, 4s, 5s |


## Configuration (ConfigMap)

| Variable | Default | Description |
|---|---|---|
| `APP_NAME` | `python-prometheus-demo` | Appears in every log line and in `app_info` |
| `SLEEP_MIN` | `1` | Lower bound of random sleep per loop (seconds) |
| `SLEEP_MAX` | `5` | Upper bound of random sleep per loop (seconds) |
| `MAX_RUN` | `` (empty) | Cap on total iterations. Empty = run forever |
| `METRICS_PORT` | `8000` | Port the Prometheus scrape endpoint listens on |


## Structured logs

Every line is JSON with a fixed column order:

```
app | module | level | ts | event | <fields>
```

```json
{"app": "python-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:00Z", "event": "startup", "sleep_min": 1.0, "sleep_max": 5.0, "max_run": "unlimited"}

{"app": "python-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:04Z", "event": "loop_tick", "current_run": 1, "max_run": "unlimited", "pct_complete": "n/a", "total_execution_time_s": 3.81, "loop_execution_time_s": 3.79, "sleep_s": 3.74, "random_number": 6}

{"app": "python-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:07Z", "event": "max_run_reached", "max_run": 20, "current_run": 20, "total_execution_time_s": 58.3}
```

## Running locally (Docker)

```bash
make run                        # unlimited run, default sleep 1–5s
make run MAX_RUN=50             # stop after 50 iterations
make run SLEEP_MIN=2 SLEEP_MAX=8
make logs                       # tail output
make stop                       # stop and remove container
```

## Deploying to Kubernetes

```bash
# 1. Build the image (make it available to your cluster)
make build

# 2. Apply the manifests
kubectl apply -f k8s-deploy/
```

To change config without rebuilding the image:
```bash
# Edit k8s-deploy/01-configmap.yaml, then:
kubectl apply -f k8s-deploy/01-configmap.yaml
kubectl rollout restart deployment/python-prometheus-demo -n prometheus-demo
```

## Prometheus scrape

The pod and service carry standard annotations picked up by the `kubernetes-pods` scrape job:

```yaml
prometheus.io/scrape: "true"
prometheus.io/port:   "8000"
prometheus.io/path:   "/metrics"
prometheus.io/scheme: "http"
```

If you are running **kube-prometheus-stack**, apply `04-servicemonitor.yaml` and set the
`release:` label to match your Helm release name.


## Grafana dashboard

Import `../dashboard/dashboard.json` into Grafana and select the Thanos datasource when prompted. Identical panel structure to the Python and Go versions, filtered to `app_kubernetes_io_name="python-prometheus-demo"`.


## Project structure

```
PythonApp/app-build/
    |
    ├── app.py
    ├── requirements.txt
    ├── Dockerfile
    ├── Makefile
    ├── .env.example
    ├── README.md
    |
PythonApp/dashboard/
    |
    ├── dashboard.json
    |
PythonApp/k8s-deploy/
    |
    ├── 01-configmap.yaml
    ├── 02-deployment.yaml
    ├── 03-service.yaml
    ├── 04-servicemonitor.yaml
    └── README.md
```