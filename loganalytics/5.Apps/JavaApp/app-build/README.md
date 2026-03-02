## Java based prometheus-demo 

A lightweight Java application that generates Prometheus metrics and structured JSON logs.

Functionally identical to python-prometheus-demo and golang-prometheus-demo — same metrics, same log format, same ConfigMap interface — built as a Java port to compare JVM runtime characteristics side-by-side.

## What it does

Runs a continuous loop that sleeps for a random interval, then updates a set of Prometheus metrics. All behaviour is driven by a ConfigMap — no code changes needed to tune it.

## Metrics exposed on `:8000/metrics`

| Metric | Type | Description |
|---|---|---|
| `app_info` | Gauge (label-based) | App identity and config values sourced from the ConfigMap |
| `random_number` | Gauge | Random integer (1–10) generated each loop |
| `loop_pct_complete` | Gauge | Current iteration as % of `MAX_RUN` (0 when unlimited) |
| `loop_total` | Counter | Total loop iterations executed since startup |
| `loop_duration_seconds` | Summary | Elapsed time per loop — exposes `_count` and `_sum` |
| `loop_execution_seconds` | Histogram | Elapsed time per loop across 5 buckets: 1s, 2s, 3s, 4s, 5s |

> **Note:** The JVM Prometheus client also auto-exposes JVM runtime metrics under `jvm_*`
> (heap, GC, threads) via `simpleclient_hotspot`. These appear alongside the app metrics.


## Configuration (ConfigMap)

| Variable | Default | Description |
|---|---|---|
| `APP_NAME` | `java-prometheus-demo` | Appears in every log line and in `app_info` |
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
{"app": "java-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:00Z", "event": "startup", "app_name": "java-prometheus-demo", "start_time": "2025-01-01T10:00:00Z", "version": "1.0.0", "sleep_min": 1.0, "sleep_max": 5.0, "max_run": "unlimited", "metrics_port": 8000}

{"app": "java-prometheus-demo", "module":"main", "level": "INFO", "ts": "2025-01-01T10:00:04Z", "event": "loop_tick", "max_run": "unlimited", "current_run": 1, "pct_complete": "n/a", "total_execution_time_s": "3.81", "loop_execution_time_s": "3.79", "sleep_s": "3.74", "random_number": 6}
```


## Building

Requires Java 17+ and Maven 3.9+. The Docker build handles everything via multi-stage build.

```bash
# Build + push container (recommended — no local Java needed)
cp .env.example .env    # set REPO_NAME
make build-push

# Build locally (requires Java 17 + Maven)
mvn package -DskipTests
java -jar target/java-prometheus-demo-1.0.0.jar
```

## Deploying to Kubernetes

The namespace (`prometheus-demo`) is shared with the Python and Go apps.
Apply the namespace from one of those first if not already present.

```bash
make k-apply     # deploys configmap, deployment, service
make k-stop      # scale to 0
make k-start     # scale back to 1
make k-rollout   # rolling restart (pick up new image or ConfigMap)
make k-delete    # remove all resources (leaves namespace intact)
```

## Grafana dashboard

Import `../dashboard/dashboard-java.json` into Grafana and select the Thanos datasource when prompted. Identical panel structure to the Python and Go versions, filtered to `app_kubernetes_io_name="java-prometheus-demo"`.


## Project structure

```
java-prometheus-demo/
|
├── src/main/java/com/observation/prometheusdemo/
│   └── App.java           Application entry point
|
├── pom.xml                Maven build + dependencies
├── Dockerfile             Multi-stage: Maven builder → JRE runtime
├── Makefile               build, push, k-apply, k-delete, k-stop/start
├── .env.example           Copy to .env and set REPO_NAME
|
└── k8s-deploy/
    ├── 01-configmap.yaml
    ├── 02-deployment.yaml
    ├── 03-service.yaml
    └── 04-servicemonitor.yaml
```
