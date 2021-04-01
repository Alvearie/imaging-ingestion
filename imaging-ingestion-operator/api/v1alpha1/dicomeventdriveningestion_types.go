/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomEventDrivenIngestionSpec defines the desired state of DicomEventDrivenIngestion
type DicomEventDrivenIngestionSpec struct {
	// Database Config Name
	DatabaseConfigName string `json:"databaseConfigName,omitempty"`
	// Database Secret Name
	DatabaseSecretName string `json:"databaseSecretName,omitempty"`
	// Image Pull Secrets
	// +optional
	ImagePullSecrets []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	// Event Processor Spec
	EventProcessor EventProcessorSpec `json:"eventProcessor,omitempty"`
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

// DicomEventDrivenIngestion is the Schema for the dicomeventdriveningestions API
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
