# Roadmap
  Below are the additional features that are being considered and planned for development.  Generally, these will be prioritized first based upon improving consumability; we want to illustrate the art of the possible for medical imaging.  Second for capability; we want to provide implementations for a full spectrum of medical imaging standards. Lastly for operationalization of capability; we aim to achieve highest standard of cloud native operating models with hyper-automation for day 0, day 1, and day 2 operations.

## Planned

-  Improve the scope of support in the DIMSE Proxy for bidirectional use-cases.  Current capability is limited to unidirectional ingestion.


-  Expand automated testing to include public DICOM data sets


-  Provide a streamlined and lightweight example for rapid evaluation on a small *Kubernetes* deployment such as *Minikube* or *Docker Desktop*.


-  Implement UPS-RS as a second binding implementation for *DicomStudyBinding* custom resources. This implementation will add/update a target imaging work list with work items when study revisions are created. Currently only the FHIRv4 ImagingStudy resource fan-out is available.


-  Implement a DIMSE C-STORE as a second binding implementation for *DicomInstanceBinding* custom resources. Currently only DICOMweb STOW-RS fan-out is available.


-  Document security patterns.  There is no direct enforcement of access tokens in the provided ingestion services.  The preferred approach is to provide authentication and authorization is through *Istio* with a *Gateway*, *VirtualService*, and *RequestAuthentication* custom resources.  These can provide a policy for authentication and authorization that is provided by any *OIDC-Connect* offering party.  


-  Expand adoption of best practices for a cloud-native operating model.  One example: completely specify resource requirements (CPU, memory, etc) for every container and the expected throughput.  This effectively standardizes the deployment into small 'lego blocks' for incremental scaling behavior.


## Candidate

-  Add DIMSE C-GET capability to the *dimse-ingestion-service* container.  This could allow the *DimseProxy* to retrieve DICOM from the services storage space. A DIMSE C-FIND may also be of interest.



-  Allow updating the location of a DICOM resource in the *DicomEventDrivenIngestion* manifest.  This would allow DICOM resources to be moved from the ingestion storage space to an alternative external storage location (ie. vender neutral archive or long term archive).  The ingestion DICOMweb services would HTTP 301 to the new storage location WADO-RS. 


-  Integration with the Alvearie data lineage services.


-  Explore Knative activation from a NATS subject.  This would allow scaling the DIMSE components to zero when idle and introduce new serving patterns.  For example, dedicated pods for handling an DIMSE association.


-  Evaluate if NATS Streaming would be helpful for some composite DIMSE operations.


-  Evaluate adding support to the operator to generate *NatsServiceRole* custom resources for NATS subject access control.