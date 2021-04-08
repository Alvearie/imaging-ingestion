# Alvearie Imaging Ingestion Operator

## Description

Each of the subcomponents of imaging ingestion are represented as individual *Kubernetes Custom Resource Definitions (CRD)*.  This extends the core *Kubernetes* API and exposes imaging subcomponents as native Kubernetes objects.  The operator allows for declarative configuration, deployment, and management of all subcomponents within the cluster.

## Deployment

Apply the provided YAML to the cluster.  Without modification, this YAML will:
-  Creates a **imaging-ingestion** namespace, 
-  Adds the provided *Custom Resource Definitions* to the cluster
-  Creates the *ServiceAccount*, *Role*, and *RoleBinding* for the operator in the imaging-ingestion namespace
-  Creates a *ControllerManagerConfig* to manage the scheduling of the operator and a *ConfigMap* for operator leader election state
-  Deploys the operator to the imaging-ingestion namespace

```bash
kubectl apply -f https://raw.githubusercontent.com/Alvearie/imaging-ingestion/main/imaging-ingestion-operator/deploy/manifests.yaml
```

## Custom Resource Definitions

  Each of the provided CRDs has an example and in-line documentation with the subcomponent documentation pages.
  
- [DicomEventDrivenIngestion](../docs/event-driven-ingestion/overview.md)
- [DicomwebIngestionService](../docs/dicomweb-ingestion-service/overview.md) 
- [DicomInstanceBinding](../docs/dicom-instance-binding/overview.md) 
- [DicomStudyBinding](../docs/dicom-study-binding/overview.md)
- [DimseIngestionService](../docs/dimse-ingestion-service/overview.md)
- [DimseProxy](../docs/dimse-proxy/overview.md) 


