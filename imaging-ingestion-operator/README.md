# Alvearie Imaging Ingestion Operator 

## Description

Each of the subcomponents of imaging ingestion are represented as individual *Kubernetes Custom Resource Definitions (CRD)*.  This extends the core *Kubernetes* API and exposes imaging subcomponents as native Kubernetes objects.  The operator allows for declarative configuration, deployment, and management of all subcomponents within the cluster.

## Deployment


```bash
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/01_namespace.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/02_service_account.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/03_role.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/04_role_binding.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/05_operator.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dicomeventdriveningestions_crd.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dicominstancebindings_crd.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dicomstudybindings_crd.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dicomwebingestionservices_crd.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dimseingestionservices_crd.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/deploy/imaging-ingestion.alvearie.org_dimseproxies_crd.yaml
```