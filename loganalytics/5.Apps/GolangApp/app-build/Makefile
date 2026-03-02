# ── Load .env (must define REPO_NAME) ────────────────────────────────────────
ifneq (,$(wildcard .env))
  include .env
  export
endif

ifndef REPO_NAME
  $(error REPO_NAME is not set. Add REPO_NAME=<registry-host>/<project> to .env)
endif

# ── Image naming ──────────────────────────────────────────────────────────────
IMAGE_NAME  := golang-prometheus-demo
IMAGE_TAG   := latest
FULL_IMAGE  := $(REPO_NAME)/$(IMAGE_NAME):$(IMAGE_TAG)

# ── Kubernetes manifest folder ────────────────────────────────────────────────
K8S_DIR     := k8s-deploy
NAMESPACE   := prometheus-demo

.PHONY: build push build-push k-apply k-delete k-rollout k-stop k-start clean help

## Build the Docker image and tag for the registry
build:
	docker build -t $(FULL_IMAGE) .
	@echo "Built: $(FULL_IMAGE)"

## Push the image to the registry defined in .env
push:
	docker push $(FULL_IMAGE)
	@echo "Pushed: $(FULL_IMAGE)"

## Build then push in one step
build-push: build push

## Apply Kubernetes manifests.
## Substitutes REPO_NAME into the deployment image field at apply time.
## Skips 04-servicemonitor.yaml if Prometheus Operator CRDs are not installed.
k-apply:
	@echo "Applying manifests with image: $(FULL_IMAGE)"
	kubectl apply -f ../$(K8S_DIR)/00-namespace.yaml
	kubectl apply -f ../$(K8S_DIR)/01-configmap.yaml
	@sed 's|REPO_NAME/$(IMAGE_NAME):$(IMAGE_TAG)|$(FULL_IMAGE)|g' \
		../$(K8S_DIR)/02-deployment.yaml | kubectl apply -f -
	kubectl apply -f ../$(K8S_DIR)/03-service.yaml
	@if kubectl api-resources | grep -q servicemonitors; then \
		echo "Prometheus Operator detected — applying ServiceMonitor..."; \
		kubectl apply -f ../$(K8S_DIR)/04-servicemonitor.yaml; \
	else \
		echo "Skipping 04-servicemonitor.yaml — ServiceMonitor CRD not found."; \
		echo "Pod annotations are in place for annotation-based scraping."; \
	fi

## Tear down all Kubernetes resources
k-delete:
	@if kubectl api-resources | grep -q servicemonitors; then \
		kubectl delete -f ../$(K8S_DIR)/04-servicemonitor.yaml || true; \
	fi
	kubectl delete -f ../$(K8S_DIR)/03-service.yaml || true
	kubectl delete -f ../$(K8S_DIR)/02-deployment.yaml || true
	kubectl delete -f ../$(K8S_DIR)/01-configmap.yaml || true

## Rollout restart the deployment (pick up a new image or ConfigMap change)
k-rollout:
	kubectl rollout restart deployment/$(IMAGE_NAME) -n $(NAMESPACE)

## Stop the app (scale to 0)
k-stop:
	kubectl scale deployment/$(IMAGE_NAME) -n $(NAMESPACE) --replicas=0

## Start the app (scale to 1)
k-start:
	kubectl scale deployment/$(IMAGE_NAME) -n $(NAMESPACE) --replicas=1

## Remove the local image
clean:
	docker rmi $(FULL_IMAGE) || true

## Show this help message
help:
	@grep -E '^## ' Makefile | sed 's/^## /  /'
