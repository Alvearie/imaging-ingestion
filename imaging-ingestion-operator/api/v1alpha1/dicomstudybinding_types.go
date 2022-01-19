/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DicomStudyBindingSpec defines the desired state of DicomStudyBinding
type DicomStudyBindingSpec struct {
	// Binding Config Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BindingConfigName string `json:"bindingConfigName,omitempty"`
	// Binding Secret Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	BindingSecretName string `json:"bindingSecretName,omitempty"`
	// Image Pull Spec
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	ImagePullSpec `json:",inline"`
	// DICOM Event Driven Ingestion Name
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	DicomEventDrivenIngestionName string `json:"dicomEventDrivenIngestionName"`
	// Study Binding Spec
	//+operator-sdk:csv:customresourcedefinitions:type=spec
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

//+operator-sdk:csv:customresourcedefinitions:resources={{Service,v1,serving.knative.dev},{Trigger,v1,eventing.knative.dev},{SinkBinding,v1beta1,sources.knative.dev}}
// Fan-out notification of DICOM studies (FHIRv4 ImagingStudy)
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
