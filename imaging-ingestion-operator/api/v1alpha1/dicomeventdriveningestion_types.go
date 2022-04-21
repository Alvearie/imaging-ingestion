/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomEventDrivenIngestionSpec defines the desired state of DicomEventDrivenIngestion
type DicomEventDrivenIngestionSpec struct {
	// Database Config Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DatabaseConfigName string `json:"databaseConfigName,omitempty"`
	// Database Secret Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DatabaseSecretName string `json:"databaseSecretName,omitempty"`
	// Image Pull Spec
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSpec `json:",inline"`
	// Event Processor Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	EventProcessor EventProcessorSpec `json:"eventProcessor,omitempty"`
	// Revisioning Delay in Seconds
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	RevisioningDelay *int `json:"revisioningDelay,omitempty"`
}

type EventProcessorSpec struct {
	// Service Spec
	ServiceSpec `json:",inline"`
}

// DicomEventDrivenIngestionStatus defines the observed state of DicomEventDrivenIngestion
type DicomEventDrivenIngestionStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
	// Broker Endpoint
	BrokerEndpoint string `json:"brokerEndpoint,omitempty"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

//+operator-sdk:csv:customresourcedefinitions:resources={{Service,v1,serving.knative.dev},{Broker,v1,eventing.knative.dev},{Trigger,v1,eventing.knative.dev}}
// Event driven manifest of all DICOM data across all associated S3 buckets
type DicomEventDrivenIngestion struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DicomEventDrivenIngestionSpec   `json:"spec,omitempty"`
	Status DicomEventDrivenIngestionStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DicomEventDrivenIngestionList contains a list of DicomEventDrivenIngestion
type DicomEventDrivenIngestionList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DicomEventDrivenIngestion `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DicomEventDrivenIngestion{}, &DicomEventDrivenIngestionList{})
}

func (i *DicomEventDrivenIngestion) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
