/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomInstanceBindingSpec defines the desired state of DicomInstanceBinding
type DicomInstanceBindingSpec struct {
	// Binding Config Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BindingConfigName string `json:"bindingConfigName,omitempty"`
	// Binding Secret Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BindingSecretName string `json:"bindingSecretName,omitempty"`
	// Image Pull Secrets
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSecrets []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	// DICOM Event Driven Ingestion Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Instance Binding Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	InstanceBinding InstanceBindingSpec `json:"instanceBinding,omitempty"`
}

type InstanceBindingSpec struct {
	// Service Spec
	ServiceSpec `json:",inline"`
}

// DicomInstanceBindingStatus defines the observed state of DicomInstanceBinding
type DicomInstanceBindingStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

//+operator-sdk:csv:customresourcedefinitions:resources={{Service,v1,serving.knative.dev},{Trigger,v1,eventing.knative.dev}}
// Fan-out notification of DICOM instance data (STOW-RS)
type DicomInstanceBinding struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DicomInstanceBindingSpec   `json:"spec,omitempty"`
	Status DicomInstanceBindingStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DicomInstanceBindingList contains a list of DicomInstanceBinding
type DicomInstanceBindingList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DicomInstanceBinding `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DicomInstanceBinding{}, &DicomInstanceBindingList{})
}

func (i *DicomInstanceBinding) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
