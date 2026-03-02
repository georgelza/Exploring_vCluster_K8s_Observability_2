
## 3.traefik-ingress

## Deployment

### Prequitistes

First we need to make sure we have our required namespaces.

This can be done by executing:

```bash
kubectl apply -f loganalytics/0.namespaces.yaml
```

### Log Analytics

```bash
cd 3.traefik-ingress
kubectl apply -f .
```

```
loganalytics/
|
├── 3.traefik-ingress/
|   ├── 1.traefik-deploy.yaml
|   ├── 2.traefik-deploy-services.yaml
|   └── README.md
```

```bash
kubectl get all -n logging
```