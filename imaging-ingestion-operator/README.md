# Alvearie Imaging Ingestion Operator

## Description

Each of the subcomponents of imaging ingestion are represented as individual *Kubernetes Custom Resource Definitions (CRD)*.  This extends the core *Kubernetes* API and exposes imaging subcomponents as native Kubernetes objects.  The operator allows for declarative configuration, deployment, and management of all subcomponents within the cluster.

## Deployment


```bash
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dicomeventdriveningestions_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dicominstancebindings_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dicomstudybindings_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dicomwebingestionservices_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dimseingestionservices_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/crds/imaging-ingestion.alvearie.org_dimseproxies_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/01_namespace.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/02_service_account.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/03_role.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/04_role_binding.yaml
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/05_operator.yaml
```
