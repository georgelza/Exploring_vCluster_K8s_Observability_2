# Golang based prometheus-demo 


Little fake metric data generator written in [Golang](https://go.dev)

Basic Application that generate the 5 basic types of metrics that Prometheus manages:

- info
- counter
- summary
- histogram
- guage

## Building Application

Because we don't expose, give the container access to the internet, it requires us to first do a go mod tidy in the app-build directory, to pull all the library files required. These are then used by make build and make push when building/compiling the application, creating our container image.

```bash
cd app-build
go mod tidy
make build
make push
```

## Deploying Application/manifest stack

```bash
make k-apply
```

## Verify Metrics being published

```bash
kubectl port-forward -n prometheus-demo deployment/golang-prometheus-demo 8000:8000
```

### Metrics

```bash
curl http://localhost:8000/metrics
```

### Logs

```bash
kubectl logs -n prometheus-demo deployment/golang-prometheus-demo -f
```

## Deploy Grafana Dashboard

Once the above is confirmed working.

In Grafana navigate to `Dashboards`, top, right click on `New` dropdown, then select: `Import`, 
Now click on the top block where it says `Upload dashboard JSON file` and navigate to the `GolangApp/dashboard` directory and select `dashboard.json`.

In the `DS_PROMETHEUS`, Select `Thanos` as datasource and `Import`.

### To Stop and Start the pod we use:

```bash
# Stop
kubectl scale deployment/golang-prometheus-demo -n prometheus-demo --replicas=0
# Restart
kubectl scale deployment/golang-prometheus-demo -n prometheus-demo --replicas=1
```

Note: Added to make file, calling above 2 kubectl commands: 

```bash
# Stop
make k-stop
# Restart
make k-start
```