## Deploying our monitor stack

### Monitor stack Structure

First, below is a depiction of all the manifest files involved.
See below the layout for more detail with regard to the deployment steps.

As you will see, each directory is numbered, defining the deployment order. Each sub directory also includes an additional `README.md` file.

NOTE: we deploy the filebeat injesting pipeline before we deploy the demo apps. This is so that we catch all logs as the applications start, even if they fail, we would have those logs in the Elasticsearch store.
See 4.kibana/README.md for some post deployment verification notes.


```
loganalytics/
|
├── 0.namespaces.yaml
├── Dashboards.md
├── Deploy_core.md
├── Deploy_analytics.md     <- This file
├── README.md     
|
├── 1.elasticsearch/
│   ├── 1.logging-storage.yaml
│   ├── 2.elasticsearchyaml
│   ├── 3.kibana.yaml
│   └── README.md
|
├── 2.elk-app-integration/
|   ├── 1.filebeat-config-full.yaml
|   └── README.md
|
├── 3.traefik-ingress/
|   ├── 1.traefik-deploy.yaml
|   ├── 2.traefik-deploy-services.yaml
|   └── README.md
|
├── 4.kibana/
|   ├── 1.kibana-bootstrap-job.yaml
|   └── README.md
|
└── 5.Apps/
    ├── PythonApp/
    |   ├── app-build/
    |   ├── dashboard/
    |   ├── k8s-deploy/
    |   └── README.md
    ├── JavaApp/
    |   ├── app-build/
    |   ├── dashboard/
    |   ├── k8s-deploy/
    |   └── README.md
    ├── GolangApp/
    |   ├── app-build/
    |   ├── dashboard/
    |   ├── k8s-deploy/
    |   └── README.md
    └── README.md
```

### Lets go Deploying...

1. Deploy monitoring/0.namespaces.yaml

```bash
# in monitoring directory
kubectl apply 0.namespaces.yaml
kubectl get namespaces
```

1. Deploy loganalytics/1.elasticsearch

>The only step that genuinely needs a human pause is after step 1.elasticsearch/2.elasticsearch.yaml — wait for Elasticsearch to become Ready before continuing, since the bootstrap job's initContainer will handle waiting for Kibana itself.


```bash
cd loganalytics/1.elasticsearch
kubectl apply -f 1.logging-storage.yaml
kubectl apply -f 2.elasticsearch.yaml
kubectl rollout status deployment/elasticsearch -n logging --timeout=180s 
# Wait for stack to be READY 1/1
kubectl apply -f 3.kibana.yaml
kubectl get all -n logging -o wide
```


2. Deploy loganalytics/3.elk-app-integration

This part of the filebeat-config is critical, and took some time to figure out to make sure filebeat read the data correctly and then submitted to elasticsearch correctly,

Pay attention to the pod names / condition / *lines 88-90*

```yaml
    - condition:
        contains:
            kubernetes.pod.name: "python-prometheus-demo"                  
        config:
        - type: container
            paths:
            - /var/log/containers/*${data.kubernetes.container.id}.log
            tags: ["python-prometheus-demo"]
```
and our catch all *lines 233-244*

```yaml
    - condition:
        and:
            - not:
                contains:
                kubernetes.pod.name: "python-prometheus-demo"

```
Also the 2 specific settings in the output.elasticsearch section, see, l*ine 268-271*.

```yaml
    setup.template.enabled: false
    setup.ilm.enabled: false
```

```bash
cd loganalytics/3.elk-app-integration
kubectl apply -f .
kubectl get all -n logging -o wide
```

1. Deploy loganalytics/4.traefik-ingress

```bash
cd loganalytics/4.traefik-ingress
kubectl apply -f .
kubectl get all -n logging -o wide
```

4. Deploy loganalytics/5.kibana

```bash
cd loganalytics/5.kibana
kubectl apply -f .
kubectl get all -n logging -o wide
```

5. Deploy Various Demo Prometheus metric generating apps, see loganalytics/2.Apps

Note, these are direct copies from our previous blog where it was contained in monitoring/7.Apps

```bash
# Python Demo App
cd loganalytics/2.Apps
cd PythonApp/app-build
make build
make push
make k-apply

# Golang Demo App
cd GolangApp/app-build
go mod tidy
make build
make push
make k-apply

# Java Demo App
cd JavaApp/app-build
make build
make push
make k-apply
```

```bash
kubectl get all -n prometheus-demo -o wide
```

## Example Grafana Dashboards

### Example Kurbernetes Dashboards

- 18283
- 15661

### Example node_exporter Dashboards

- 1860