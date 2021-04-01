# Why NATS?
  [NATS](http://nats.io) is used as the secure communication system from DICOM Message Service Element (DIMSE) messages. While it is possible to configure DIMSE to use TCP for data privacy, there are a number of challenges with using DIMSE with TLS for messaging between the enterprise imaging zone and the hybrid cloud.  
  
  **Challenge 1: DIMSE is point-to-point.** 
  
  Using TCP directly means that the IP address of the connection peer is static.  This represents a single point-of-failure (SPOF) between the DIMSE Application Entities within separate zones.  By deploying NATS into a cloud availability zone, a single address can route to a resilient group of different NATS workers distributed throughout the availability zone.  This addresses the SPOF. 
    
  **Challenge 2: DIMSE requires ingress and egress traffic management.**  
  
  In real-world deployments, the enterprise imaging zone and the cloud will always be in separate isolated network zones. With DIMSE, communication can be initiated in either zone.  TLS connections between two points means there needs to be both ingress and egress traffic policies for the connection within each zone. Enterprise security teams impose mandatory controls for ingress traffic into enterprise systems. The [DIMSE Proxy](docs/dimse-proxy/overview.md) uses NATS to remove the need for ingressing traffic to the enterprise imaging zone. All communication from the enterprise imaging zone is performed using an egress pattern.  
   
  
 **In addition to addressing challenges NATS brings...**
 
  Many more qualities of service to the deployment for resilient communication.  There are a number of patterns that NATS can be deployed with different qualities of service.  Understanding proper usage of NATS is an exercise left up to the reader.  This document is intended to both expose how NATS is used by the imaging ingestion subcomponents, as-well-as act as an accelerator for the purposes of quickly evaluating the imaging ingestion component.

## How Alvearie Imaging Ingestion leverages with NATS.
  When a new [DIMSE Ingestion Service](../../docs/dimse-ingestion-service/overview.md) or [DIMSE Proxy](../../docs/dimse-proxy/overview.md) is declared within *Kubernetes* using the provided custom resources, the [Alvearie Imaging Ingestion Operator](../../docs/operator/overview.md) uses the DIMSE Application Entity Title (AET) to configure a dedicated NATS *subject* for communication between the enterprise imaging zone and *kubernetes*.  There is nothing additional to do on the *kubernetes* zone. When the DIMSE Proxy is deployed in the enterprise imaging zone, some of the managed configuration details need to be extracted to perform configuration of the trusted peer in the enterprise imaging zone.  Details are provided on each subcomponent's documentation page. 
  
  `Usage diagram coming soon!`

  
## Installing NATS within *Kubernetes*  
  Applying the following will configure a NATS cluster with TLS enabled. This is provided to accelerate evaluation of the DIMSE sub-components.  
  
  `This is a work in progress.`
  
```bash
kubctl create namespace nats-pot
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/service_account.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/role.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/role_binding.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/operator.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/certificates.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/nats_server_tls.yaml
kubctl apply -f https://github.com/Alvearie/imaging-ingestion/examples/nats/ingress.yaml
```





