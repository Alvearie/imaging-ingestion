/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomwebIngestionServiceSpec defines the desired state of DicomwebIngestionService
type DicomwebIngestionServiceSpec struct {
	// Image Pull Spec
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSpec `json:",inline"`
	// DICOM Event Driven Ingestion Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Bucket Config Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BucketConfigName string `json:"bucketConfigName,omitempty"`
	// Bucket Secret Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BucketSecretName string `json:"bucketSecretName,omitempty"`
	// Provider Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ProviderName string `json:"providerName,omitempty"`
	// STOW Service Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	StowService StowServiceSpec `json:"stowService,omitempty"`
	// WADO Service Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	WadoService WadoServiceSpec `json:"wadoService,omitempty"`
}

type StowServiceSpec struct {
	// Service Spec
	ServiceSpec `json:",inline"`
}

type WadoServiceSpec struct {
	// Service Spec
	ServiceSpec `json:",inline"`
}

// DicomwebIngestionServiceStatus defines the observed state of DicomwebIngestionService
type DicomwebIngestionServiceStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
	// WADO Service Internal Endpoint
	//+operator-sdk:csv:customresourcedefinitions:type=status
	WadoServiceInternalEndpoint string `json:"wadoServiceInternalEndpoint,omitempty"`
	// WADO Service External Endpoint
	//+operator-sdk:csv:customresourcedefinitions:type=status
	WadoServiceExternalEndpoint string `json:"wadoServiceExternalEndpoint,omitempty"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

//+operator-sdk:csv:customresourcedefinitions:resources={{Service,v1,serving.knative.dev},{SinkBinding,v1beta1,sources.knative.dev}}
// Provides DICOMweb WADO-RS and STOW-RS access to a S3 bucket
type DicomwebIngestionService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DicomwebIngestionServiceSpec   `json:"spec,omitempty"`
	Status DicomwebIngestionServiceStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DicomwebIngestionServiceList contains a list of DicomwebIngestionService
type DicomwebIngestionServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DicomwebIngestionService `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DicomwebIngestionService{}, &DicomwebIngestionServiceList{})
}

func (i *DicomwebIngestionService) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
