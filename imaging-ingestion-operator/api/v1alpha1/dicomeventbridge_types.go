/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomEventBridgeSpec defines the desired state of DicomEventBridge
type DicomEventBridgeSpec struct {
	// Image Pull Spec
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSpec `json:",inline"`
	// NATS URL
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsURL string `json:"natsUrl,omitempty"`
	// Make NATS Connection Secure
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsSecure bool `json:"natsSecure,omitempty"`
	// NATS Token Secret Name
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsTokenSecretName string `json:"natsTokenSecret,omitempty"`
	// NATS Subject Root
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	NatsSubjectRoot string `json:"natsSubjectRoot,omitempty"`
	// Event Bridge Role
	//+operator-sdk:csv:customresourcedefinitions:type=spec,xDescriptors={"urn:alm:descriptor:com.tectonic.ui:select:hub","urn:alm:descriptor:com.tectonic.ui:select:edge"}
	Role string `json:"role"`
	// Event Bridge Edge Mailbox. Required when Role is edge.
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	EdgeMailbox string `json:"edgeMailbox,omitempty"`
	// DICOM Event Driven Ingestion Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Event Bridge Deployment Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	EventBridge EventBridgeSpec `json:"eventBridge,omitempty"`
}

type EventBridgeSpec struct {
	// Deployment Spec
	DeploymentSpec `json:",inline"`
}

// DicomEventBridgeStatus defines the observed state of DicomEventBridge
type DicomEventBridgeStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

//+operator-sdk:csv:customresourcedefinitions:resources={{Deployment,v1},{ConfigMap,v1},{Service,v1}}
// DicomEventBridge is the Schema for the dicomeventbridges API
type DicomEventBridge struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DicomEventBridgeSpec   `json:"spec,omitempty"`
	Status DicomEventBridgeStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DicomEventBridgeList contains a list of DicomEventBridge
type DicomEventBridgeList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DicomEventBridge `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DicomEventBridge{}, &DicomEventBridgeList{})
}

func (i *DicomEventBridge) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
