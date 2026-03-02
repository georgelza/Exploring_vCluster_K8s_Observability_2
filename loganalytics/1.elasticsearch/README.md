
## 1.elasticsearch

## Deployment

### Prequitistes

First we need to make sure we have our required namespaces.

This can be done by executing:

```bash
kubectl apply -f loganalytics/0.namespaces.yaml
```

### Log Analytics

```bash
cd 1.elasticsearch
kubectl apply -f 1.logging-storage.yaml
kubectl apply -f 2.elasticsearch.yaml
kubectl get all -n logging
# Wait for READY 1/1 
kubectl apply -f 3.kibana.yaml
```

```
loganalytics/
|
└── 1.elasticsearch/
    ├── 1.logging-storage.yaml
    ├── 2.elasticsearchyaml
    ├── 3.kibana.yaml
    └── README.md
```

```bash
kubectl get all -n logging
```