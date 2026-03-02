# python-prometheus-demo – Kubernetes Manifest Pack

## Quick start

```bash
# Apply all manifests in the folder
kubectl apply -f k8s-deploy/

# Tear down
kubectl delete -f k8s-deploy/
```

---

## Prometheus scrape annotations (pod + service)

| Annotation                  | Value        | Purpose                             |
|-----------------------------|--------------|-------------------------------------|
| `prometheus.io/scrape`      | `"true"`     | Opt-in to scraping                  |
| `prometheus.io/port`        | `"8000"`     | Port Prometheus connects to         |
| `prometheus.io/path`        | `"/metrics"` | HTTP path for the scrape endpoint   |
| `prometheus.io/scheme`      | `"http"`     | Protocol (http / https)             |

These are read by the standard `kubernetes-pods` job in `prometheus.yml`:

```yaml
- job_name: kubernetes-pods
  kubernetes_sd_configs:
    - role: pod
  relabel_configs:
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: "true"
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
    - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
      action: replace
      regex: ([^:]+)(?::\d+)?;(\d+)
      replacement: $1:$2
      target_label: __address__
```

---

## Structured log format

Every log line is a JSON object with a fixed column order:

```
app | level | ts | event | <remaining fields>
```

Example startup line:

```json
{"app": "python-prometheus-demo", "level": "INFO", "ts": "2025-01-01T10:00:00Z", "event": "startup", "metrics_port": 8000, "sleep_min": 1.0, "sleep_max": 5.0, "max_run": "unlimited"}
```

Example loop tick:

```json
{"app": "python-prometheus-demo", "level": "INFO", "ts": "2025-01-01T10:00:03Z", "event": "loop_tick", "max_run": "unlimited", "current_run": 1, "pct_complete": "n/a", "total_execution_time_s": 3.14, "loop_execution_time_s": 3.12, "sleep_s": 3.08, "random_number": 7}
```

---

## Tuning via ConfigMap

Edit `01-configmap.yaml` then roll the deployment:

| Key            | Default           | Description                              |
|----------------|-------------------|------------------------------------------|
| `APP_NAME`     | `python-prometheus-demo` | Label on all log lines & app_info metric |
| `SLEEP_MIN`    | `1`               | Minimum random sleep per loop (seconds)  |
| `SLEEP_MAX`    | `5`               | Maximum random sleep per loop (seconds)  |
| `MAX_RUN`      | `""`              | Loop cap. Empty = run forever            |
| `METRICS_PORT` | `8000`            | Prometheus scrape port                   |

```bash
kubectl apply -f k8s-deploy/01-configmap.yaml
kubectl rollout restart deployment/python-prometheus-demo -n prometheus-demo
```

---

## ServiceMonitor (Prometheus Operator)

`04-servicemonitor.yaml` is for installations using **kube-prometheus-stack** or the
standalone **Prometheus Operator**.

Check if the CRD exists before applying:

```bash
kubectl api-resources | grep servicemonitors
```

If it does, ensure the `release:` label matches your Helm release name:

```bash
helm list -n monitoring
```

Then update the label in `04-servicemonitor.yaml`:

```yaml
labels:
  release: <your-helm-release-name>
```

## Project structure

```
k8s-deploy/
  ├── 00-namespace.yaml       Dedicated namespace
  ├── 01-configmap.yaml       All runtime config (APP_NAME, SLEEP_MIN/MAX, MAX_RUN …)
  ├── 02-deployment.yaml      App pod with Prometheus scrape annotations + probes
  ├── 03-service.yaml         ClusterIP Service exposing :8000/metrics
  ├── 04-servicemonitor.yaml  Prometheus Operator ServiceMonitor (optional)
  └── README.md
```
