# Alvearie Imaging Ingestion Operator

## Description

Each of the subcomponents of imaging ingestion are represented as individual *Kubernetes Custom Resource Definitions (CRD)*.  This extends the core *Kubernetes* API and exposes imaging subcomponents as native Kubernetes objects.  The operator allows for declarative configuration, deployment, and management of all subcomponents within the cluster.

## Deployment


```bash
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/manifests.yaml
```
