# golang-prometheus-demo

A lightweight Go application that generates Prometheus metrics and structured JSON logs.

Functionally identical to python-prometheus-demo and java-prometheus-demo applications — same metrics, same log format, same ConfigMap interface — built as a Go port to compare language runtime characteristics side-by-side.

---

## What it does

Runs a continuous loop that sleeps for a random interval, then updates a set of Prometheus metrics. All behaviour is driven by a ConfigMap — no code changes needed to tune it.

---

## Metrics exposed on `:8000/metrics`

| Metric | Type | Description |
|---|---|---|
| `app_info` | Gauge (label-based) | App identity and config values sourced from the ConfigMap |
| `random_number` | Gauge | Random integer (1–10) generated each loop |
| `loop_pct_complete` | Gauge | Current iteration as % of `MAX_RUN` (0 when unlimited) |
| `loop_total` | Counter | Total loop iterations executed since startup |
| `loop_duration_seconds` | Summary | Elapsed time per loop — exposes `_count` and `_sum` |
| `loop_execution_seconds` | Histogram | Elapsed time per loop across 5 buckets: 1s, 2s, 3s, 4s, 5s |


## Configuration (ConfigMap)

| Variable | Default | Description |
|---|---|---|
| `APP_NAME` | `golang-prometheus-demo` | Appears in every log line and in `app_info` |
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
{"app": "golang-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:00Z", "event": "startup", "sleep_min": 1, "sleep_max": 5, "max_run": "unlimited", "metrics_port": 8000}

{"app": "golang-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:04Z", "event": "loop_tick", "current_run": 1, "max_run": "unlimited", "pct_complete": "n/a", "total_execution_time_s": "3.81", "loop_execution_time_s": "3.79", "sleep_s": "3.74", "random_number": 6}
```

## Building

```bash
# Fetch dependencies (requires Go 1.22+)
go mod tidy

# Build locally
go build -o golang-prometheus-demo .

# Build + push container
cp .env.example .env   # set REPO_NAME
make build-push
```

## Deploying to Kubernetes

The namespace (`prometheus-demo`) is shared with python-prometheus-demo.
The namespace manifest is not included here — apply it from the python app's k8s-deploy folder first if not already present.

```bash
make k-apply    # deploys configmap, deployment, service
make k-stop     # scale to 0
make k-start    # scale back to 1
make k-delete   # remove all resources (leaves namespace intact)
```

---

## Grafana dashboard

Import `../dashboard/dashboard.json` into Grafana and select the Thanos datasource when prompted. The dashboard is identical in structure to the Python version — all the same panels, filtered to `app_kubernetes_io_name="golang-prometheus-demo"`.


## Project structure

```
GolangApp/app-build/
    |
    ├── main.go
    ├── go.mod
    ├── Dockerfile
    ├── Makefile
    ├── .env.example
    ├── README.md
    |
Golangapp/dashboard/
    |
    ├── dashboard.json
    |
GolangApp/k8s-deploy/
    |
    ├── 01-configmap.yaml
    ├── 02-deployment.yaml
    ├── 03-service.yaml
    ├── 04-servicemonitor.yaml
    └── README.md
```