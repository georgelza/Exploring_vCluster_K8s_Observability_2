## Multi Node Kubernetes cluster on vCluster, single Traefik Application Proxy and seperate Ingress's Controllers

## Step 1. Create a vCluster in Docker (automatically connects)

Pre Deployment

<img src="../my-vc1/0.0.docker_ps.png" alt="Pre Deployment" width="950" height="70">


```bash
# while located in the monitoring directory, execute

sudo vcluster create my-vc1 --values vcluster.yaml
```
<img src="../my-vc1/0.1.k8s_create.png" alt="Creating my-vc1" width="950" height="200">

## Step 2. Verify it's working

```bash
kubectl get nodes
kubectl get namespaces
```
<img src="../my-vc1/0.2.get_nodes.png" alt="Kubectl-get-nodes" width="950" height="100">

<img src="../my-vc1/0.3.get_namespaces.png" alt="Kubectl-get-namespaces" width="950" height="100">


Lets Label our nodes correctly, little attention to detail

```bash
kubectl label node worker-1 worker-2 worker-3 node-role.kubernetes.io/worker=worker
```
<img src="../my-vc1/0.4.label_nodes.png" alt="Kubectl-get-namespaces" width="950" height="100">

Notice the difference.

```bash
kubectl get nodes
```
<img src="../my-vc1/0.5.get_nodes.png" alt="Kubectl-get-nodes" width="950" height="100">


## STEP 3: Install Traefik open-source HTTP Application/Reverse proxy 

```bash
echo "Installing Traefik with ClusterIP..."

helm upgrade --install traefik1 traefik \
  --repo https://helm.traefik.io/traefik \
  --namespace ingress-traefik1 --create-namespace \
  --set service.type=ClusterIP \
  --set ingressClass.name=traefik1
```
<img src="../my-vc1/0.6.helm_install_traefik.png" alt="HELM Deploy" width="950" height="250">


By using a ClusterIP it allows us to run one kubectl port-forward onto the cluster, through which we can then access all the services exposed.


## STEP 4: Deploy our Monitoring Stack.

So I'm using [Manifest](https://en.wikipedia.org/wiki/Manifest_file) files on purpose, it's easier to read and easier to follow how things are are plugged together.

Nothing stopping you from doing the below using one of the various [HELM](https://helm.sh) deploy guides. I actually include some references in `loganalytics/README.md `for deploying the stack using HELM chart deployment option.

**NOTE**: [Traefik](https://traefik.io/traefik) was deployed above as part of the cluster build using an [HELM](https://helm.sh) chart.

Enough talkng, **LETS Deploy**, Start by reading `loganalytics/Deploy_analytics.md`, after which each of the subdirectories below as show you will notice include a `README.md`.
