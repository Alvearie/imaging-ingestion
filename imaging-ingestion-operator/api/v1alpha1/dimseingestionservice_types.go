/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DimseIngestionServiceSpec defines the desired state of DimseIngestionService
type DimseIngestionServiceSpec struct {
	// Image Pull Secrets
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSecrets []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	// DICOM Event Driven Ingestion Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Bucket Config Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BucketConfigName string `json:"bucketConfigName,omitempty"`
	// Bucket Secret Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BucketSecretName string `json:"bucketSecretName,omitempty"`
	// Application Entity Title
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ApplicationEntityTitle string `json:"applicationEntityTitle,omitempty"`
	// NATS URL
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsURL string `json:"natsUrl,omitempty"`
	// Make NATS Connection Secure
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsSecure bool `json:"natsSecure,omitempty"`
	// NATS Subject Root
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsSubjectRoot string `json:"natsSubjectRoot,omitempty"`
	// NATS Token Secret Name
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsTokenSecretName string `json:"natsTokenSecret,omitempty"`
	// Provider Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ProviderName string `json:"providerName,omitempty"`
	// DIMSE Service Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DimseService DimseServiceSpec `json:"dimseService,omitempty"`
}

type DimseServiceSpec struct {
	// Deployment Spec
	DeploymentSpec `json:",inline"`
}

// DimseIngestionServiceStatus defines the observed state of DimseIngestionService
type DimseIngestionServiceStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

//+operator-sdk:csv:customresourcedefinitions:resources={{Deployment,v1},{ConfigMap,v1}}
// Provides a proxied DIMSE Application Entity (AE) in the cluster for C-STORE operations to a storage space
type DimseIngestionService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DimseIngestionServiceSpec   `json:"spec,omitempty"`
	Status DimseIngestionServiceStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DimseIngestionServiceList contains a list of DimseIngestionService
type DimseIngestionServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DimseIngestionService `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DimseIngestionService{}, &DimseIngestionServiceList{})
}
func (i *DimseIngestionService) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
