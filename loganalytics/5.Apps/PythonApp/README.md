# Python based prometheus-demo 

Little fake metric data generator written in [Python](https://www.python.org)

Basic Application that generate the 5 basic types of metrics that Prometheus manages:

- info
- counter
- summary
- histogram
- guage
  
## Verify Metrics being published

```bash
kubectl port-forward -n prometheus-demo deployment/python-prometheus-demo 8000:8000
```

### Metrics

```bash
curl http://localhost:8000/metrics
```

### Logs

```bash
kubectl logs -n prometheus-demo deployment/python-prometheus-demo -f
```

## Deploy Grafana Dashboard

Once the above is confirmed working.

In Grafana navigate to `Dashboards`, top, right click on `New` dropdown, then select: `Import`, 
Now click on the top block where it says `Upload dashboard JSON file` and navigate to the `PythonApp/dashboard` directory and select `dashboard.json`.

In the `DS_PROMETHEUS`, Select `Thanos` as datasource and `Import`.


### To Stop and Start the pod we use:

```bash
# Stop
kubectl scale deployment/python-prometheus-demo -n prometheus-demo --replicas=0
# Restart
kubectl scale deployment/python-prometheus-demo -n prometheus-demo --replicas=1
```

Note: Added to make file, calling above 2 kubectl commands: 

```bash
# Stop
make k-stop
# Restart
make k-start
```