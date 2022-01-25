# Alvearie Imaging Ingestion Operator

## Description

Each of the subcomponents of imaging ingestion are represented as individual *Kubernetes Custom Resource Definitions (CRD)*.  This extends the core *Kubernetes* API and exposes imaging subcomponents as native Kubernetes objects.  The operator allows for declarative configuration, deployment, and management of all subcomponents within the cluster.

## Deployment

### Compatibility Matrix

| Operator Version | Knative Version | OpenShift Version |
|------------------|---------|-----------|
| 0.0.1            | v0.16.0 - v0.22.1      | v4.5 |
| 0.0.2            | v0.19.0 - v0.26.1      | v4.6, v4.7, v4.8, v4.9 |
| 0.0.3            | v0.19.0 - v0.26.1      | v4.6, v4.7, v4.8, v4.9 |

**Option 1: Using OperatorHub.io**

  For Kubernetes clusters, such as *OpenShift*, that have Operator Lifecycle Manager (OLM) deployed, the easiest way to deploy the Alvearie Imaging Ingestion operator, is by using the community operator provided on [Operatorhub.io](https://operatorhub.io).

**Option 2: Using kubectl**

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


