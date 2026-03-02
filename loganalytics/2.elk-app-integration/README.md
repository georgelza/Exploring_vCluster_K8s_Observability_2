
## 2.elk-app-integration

## Deployment

### Prequitistes

First we need to make sure we have our required namespaces.

This can be done by executing:

```bash
kubectl apply -f loganalytics/0.namespaces.yaml
```

### Log Analytics

```bash
cd 2.elk-app-integration
kubectl apply -f 1.filebeat-config-full.yaml
```

```
loganalytics/
|
└── 2.elk-app-integration/
    ├── 1.filebeat-config-full.yaml
    └── README.md
```


```bash
kubectl get all -n logging
```