/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomStudyBindingSpec defines the desired state of DicomStudyBinding
type DicomStudyBindingSpec struct {
	// Binding Config Name
	BindingConfigName string `json:"bindingConfigName,omitempty"`
	// Binding Secret Name
	BindingSecretName string `json:"bindingSecretName,omitempty"`
	// Image Pull Secrets
	// +optional
	ImagePullSecrets []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	// DICOM Event Driven Ingestion Name
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Study Binding Spec
	StudyBinding StudyBindingSpec `json:"studyBinding,omitempty"`
}

type StudyBindingSpec struct {
	// Service Spec
	ServiceSpec `json:",inline"`
}

// DicomStudyBindingStatus defines the observed state of DicomStudyBinding
type DicomStudyBindingStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

// DicomStudyBinding is the Schema for the dicomstudybindings API
type DicomStudyBinding struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DicomStudyBindingSpec   `json:"spec,omitempty"`
	Status DicomStudyBindingStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DicomStudyBindingList contains a list of DicomStudyBinding
type DicomStudyBindingList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DicomStudyBinding `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DicomStudyBinding{}, &DicomStudyBindingList{})
}

func (i *DicomStudyBinding) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
