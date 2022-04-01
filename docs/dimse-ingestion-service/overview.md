# DICOM Message Service Element (DIMSE) Ingestion Service 

## Overview
  The *DIMSE Ingestion Service* is an optional sub-component of the *Kubernetes* deployment.  This subcomponent provides the ability to C-STORE and C-FIND DICOM to a storage space using a remote *DIMSE Proxy*. The *DIMSE Proxy* and *DIMSE Ingestion Service* subcomponents are deployed as pairs, the proxy within the enterprise imaging zone, and the ingestion service within *Kubernetes*.  A separate deployment of these subcomponent pairs is required for each storage space.  When creating a storage space with *DIMSE Ingestion Service*, a *DICOMweb Ingestion Service* needs to also be provided for the storage space.  This allows downstream subcomponents to retrieve data using WADO-RS.

## Subcomponent Architecture
![DIMSE Ingestion Service](../images/dimse-ingestion-service.png)

The *DIMSE Ingestion Service* provides a single container that is bound to both a storage area and a NATS subject for communication.  

## Deployment
 
 **Custom Resource**
  
Create the subcomponent deployment

```yaml
apiVersion: imaging-ingestion.alvearie.org/v1alpha1
kind: DimseIngestionService
metadata:
  name: img-ingest
spec:
  # Reference to the same ConfigMap used with the corresponding DicomWeb Ingestion Service
  bucketConfigName: img-ingest-s3
  # Reference to the same Secret used with the corresponding DicomWeb Ingestion Service 
  bucketSecretName: img-ingest-s3
  # Reference to the event broker that was created with the DicomEventDrivenIngestion custom resource
  dicomEventDrivenIngestionName: core
  # The name of this provider, for grouping many subcomponents together.
  providerName: provider
  # The DIMSET AET the linked proxy is acting as
  applicationEntityTitle: "DICOM-INGEST"
  # The NATS service address
  natsUrl: 0a0527d6.nip.io:443
  # The root name of NATS subject where the proxy is subscribing and publishing messages
  natsSubjectRoot: DIMSE
  # Enable TLS for NATS
  natsSecure: true
  # The JWT for the NATS account that will be used to access the NATS subject
  natsTokenSecret: nats-dicom-ingest-token-lhllv
```

