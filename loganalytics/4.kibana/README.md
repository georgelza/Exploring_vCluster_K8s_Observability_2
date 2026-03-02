
## 4.kibana

## Deployment

### Log Analytics

```bash
cd 4.kibana
kubectl apply -f .
```

```
loganalytics/
|
└── 4.kibana/
    ├── 1.kibana-bootstrap-job.yaml
    └── README.md
```

```bash
kubectl get all -n logging
```

## Filebeat

### Deploy some Example Kibana Dashboards

Filebeat ships pre-built Kibana dashboards for container/Kubernetes log exploration. You trigger the import from inside the running Filebeat pod:

```bash
# Get the Filebeat pod name
kubectl get pods -n logging -l app=filebeat
```

### Exec into it and run the setup

```bash
kubectl exec -n logging <filebeat-pod-name> -- \
  filebeat setup --dashboards \
  -E setup.kibana.host=http://kibana.logging.svc.cluster.local:5601 \
  -E setup.kibana.path=/kibana
```

This imports Filebeat's standard Kubernetes dashboards into Kibana automatically.

Now navigate to: 

Make sure to have executed `kubectl port-forward service/traefik1 -n ingress-traefik1 8080:80` first.

http://localhost:8080/kibana → Dashboards → search "Filebeat"


```bash
# List of Dashboards imported
curl -s "http://localhost:8080/kibana/api/saved_objects/_find?type=dashboard&per_page=20" | grep -o '"title":"[^"]*"'
```

```
"title":"[Filebeat System] New users and groups ECS"
"title":"[Filebeat Nginx] Ingress Controller access and error logs"
"title":"[Filebeat Nginx] Access and error logs ECS"
"title":"[Filebeat Pensando] DFW Overview"
"title":"[Filebeat AWS] ELB Access Log Overview"
"title":"[Filebeat AWS] VPC Flow Log Overview"
"title":"[Filebeat Santa] Overview ECS"
"title":"[Filebeat Azure] Alerts Overview"
"title":"[Filebeat PostgreSQL] Overview ECS"
"title":"[Filebeat HAProxy] Overview ECS"
"title":"[Filebeat System] Sudo commands ECS"
"title":"[Filebeat Icinga] Debug Log ECS"
"title":"[Filebeat CEF] Microsoft DNS Overview"
"title":"[Filebeat Azure] Cloud Overview"
"title":"[Filebeat GCP] Audit"
"title":"[Filebeat Nginx] Overview ECS"
"title":"[Filebeat Osquery] Compliance pack ECS"
"title":"[Filebeat IIS] Access and error logs ECS"
"title":"[Filebeat System] SSH login attempts ECS"
"title":"[Filebeat AWS] S3 Server Access Log Overview"
```