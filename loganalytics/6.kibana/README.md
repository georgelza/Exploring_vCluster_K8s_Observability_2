
## 6.kibana

## ELK Stack — Verification & Exploration Guide

This below steps provides a set of commands to confirm the stack is working and walks through how to verify log
ingestion from the command line and how to explore live log data in Kibana.

The README covers three sections:
- Section 1 — CLI verification — 7 curl/kubectl commands with expected outputs, including the aggregation query that groups log counts by level which is a clean proof-of-life beyond what you already ran.

- Section 2 — Kibana walkthrough — step-by-step from confirming Data Views exist, through Discover with column and filter setup, building a Lens visualisation, and finding the pre-built Filebeat Kubernetes dashboard. Each step is written so someone unfamiliar with Kibana can follow it independently.

- Section 3 — Summary table — documents the four problems encountered during deployment, their root causes, and the fixes applied. Useful for anyone deploying this stack from scratch on a future cluster.

---

## Prerequisites

Two port-forwards are needed in separate terminals before running any of the
commands below.

**Terminal 1 — Elasticsearch:**
```bash
kubectl port-forward service/elasticsearch -n logging 9200:9200
```

**Terminal 2 — Traefik (Kibana access):**
```bash
kubectl port-forward service/traefik1 -n ingress-traefik1 8080:80
```

---

## 1. Command-Line Verification

### 1.1 Confirm Filebeat pods are running on every node

```bash
kubectl get pods -n logging -l app=filebeat -o wide
```

Expected: one pod per node, all `Running`, zero restarts.

### 1.2 Confirm indices exist and are growing

```bash
curl -s "http://localhost:9200/_cat/indices?v" | grep python
```

Expected output (doc counts will differ):

```
yellow open filebeat-python-prometheus-demo-2026.03.01   ...   7745   0   3.2mb
yellow open filebeat-python-prometheus-demo-2026.02.28   ...  17370   0   7.2mb
yellow open filebeat-python-prometheus-demo-2026.02.27   ...   5119   0   2.2mb
```

Run it twice 30 seconds apart — the today index doc count should increase,
confirming live ingestion.

### 1.3 Confirm all filebeat indices (generic catch-all + app-specific)

```bash
curl -s "http://localhost:9200/_cat/indices?v" | grep filebeat
```

You should see both `filebeat-generic-*` (cluster-wide logs) and
`filebeat-python-prometheus-demo-*` (app-specific routed logs).

### 1.4 Confirm a parsed document has the correct fields

```bash
curl -s "http://localhost:9200/filebeat-python-prometheus-demo-*/_search?pretty&size=1" \
  | python3 -m json.tool \
  | grep -E '"@timestamp"|"app_module"|"log.level"|"event.action"|"message"'
```

Expected: `app_module` is `python-prometheus-demo`, `log.level` is uppercased
(`INFO`, `WARN`, `ERROR`), and `@timestamp` is populated from the application
log rather than the Filebeat ingestion time.

### 1.5 Count logs by log level

```bash
curl -s "http://localhost:9200/filebeat-python-prometheus-demo-*/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "by_level": {
        "terms": { "field": "log.level.keyword" }
      }
    }
  }' | python3 -m json.tool | grep -A 3 '"buckets"' | grep "key\|doc_count"
```

Expected: buckets for `INFO`, `WARN`, `ERROR` with their respective counts.

### 1.6 Confirm the index template is in place

```bash
curl -s "http://localhost:9200/_cat/templates?v"
```

Expected: one row for `filebeat` with priority `500`.

### 1.7 Confirm Filebeat on the correct node is harvesting the app

```bash
kubectl logs -n logging \
  $(kubectl get pod -n logging -l app=filebeat \
    --field-selector spec.nodeName=worker-1 \
    -o jsonpath='{.items[0].metadata.name}') \
  --since=5m | grep -i "harvester\|python"
```

Expected: a `Harvester started` line referencing the python pod container ID.

---

## 2. Kibana — Step-by-Step Exploration

Open Kibana in your browser:
```
http://localhost:8080/kibana
```

---

### 2.1 Confirm Data Views exist

1. Click the **hamburger menu** (☰) top-left
2. Go to **Stack Management** → **Data Views**
3. You should see:
   - `filebeat-python-prometheus-demo-*`
   - `filebeat-java-prometheus-demo-*`
   - `filebeat-golang-prometheus-demo-*`
   - `filebeat-generic-*`

If any are missing, re-run the bootstrap job:
```bash
kubectl delete job kibana-bootstrap -n logging
kubectl apply -f 6_kibana-bootstrap-job.yaml
```

---

### 2.2 View live logs in Discover

1. Click **hamburger menu** → **Discover**
2. In the top-left dropdown, select the data view:
   **`filebeat-python-prometheus-demo-*`**
3. Set the time picker (top-right) to **Last 15 minutes**
4. You should see log events streaming in

**Useful fields to add as columns:**

Click **Add field as column** on the left-hand field list for each of:
- `log.level`
- `module`
- `event.action`
- `app_module`

This gives a clean tabular view of structured log data.

**Filter to errors only:**

In the search bar type:
```
log.level : "ERROR"
```

Then press Enter. The view will filter to error-level events only.

---

### 2.3 Build a quick visualisation

1. Click **hamburger menu** → **Visualize Library** → **Create visualization**
2. Select **Lens**
3. In the Data View dropdown select `filebeat-python-prometheus-demo-*`
4. Set time range to **Last 24 hours**
5. Drag `log.level` from the left field list to the **Break down by** zone
6. The chart will show log volume broken down by level over time
7. Click **Save** and name it `Python Demo — Log Levels`

---

### 2.4 View the pre-built Filebeat Kubernetes dashboard

1. Click **hamburger menu** → **Dashboards**
2. Search for **Filebeat**
3. Open **[Filebeat Kubernetes] Overview**
4. Set time to **Last 1 hour**
5. Use the **Namespace** filter dropdown and select `prometheus-demo`

This shows pod log volume, top containers, and namespace breakdown across all
nodes for your demo apps.

---

### 2.5 Verify log field parsing is correct

1. In **Discover**, open any log document by clicking the **>** expand arrow
2. Confirm the following fields are present and correctly populated:

| Field | Expected value |
|---|---|
| `app_module` | `python-prometheus-demo` |
| `log.level` | `INFO`, `WARN`, or `ERROR` (uppercased) |
| `event.action` | e.g. `startup`, `running`, `sleep` |
| `module` | e.g. `main` |
| `@timestamp` | Application log time (not Filebeat ingest time) |
| `log_stream` | `application` |

If `message` still contains raw JSON and the parsed fields are missing, the
`decode_json_fields` processor did not run — check the Filebeat ConfigMap and
restart the DaemonSet.

---

## 3. Summary of What Was Fixed During Deployment

| Problem | Root Cause | Fix Applied |
|---|---|---|
| Logs landing in `filebeat-generic-*` | Autodiscovery condition used `kubernetes.pod.labels.app_kubernetes_io/name` — the `/` in the key name is treated as a path separator by Filebeat's condition evaluator | Changed condition to `contains: kubernetes.pod.name` |
| Filebeat crash-looping with 400 errors | Filebeat 8.x unconditionally attempts to register a data stream on connect; no matching index template existed | Added `3_elasticsearch-index-template.yaml` Job to pre-create the template at priority 500 before Filebeat starts |
| Template being overwritten on restart | `setup.template.overwrite: true` allowed Filebeat to replace our template with its own (which lacked `data_stream: {}`) | Set `setup.template.enabled: false` and `setup.ilm.enabled: false` |
| Noisy cloud metadata timeout errors | `add_cloud_metadata` processor probes cloud provider endpoints that don't exist on bare-metal | Removed `add_cloud_metadata` from global processors |