/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// DimseProxySpec defines the desired state of DimseProxy
type DimseProxySpec struct {
	// Image Pull Secrets
	// +optional
	ImagePullSecrets []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	// Application Entity Title
	ApplicationEntityTitle string `json:"applicationEntityTitle,omitempty"`
	// Target Dimse Host
	TargetDimseHost string `json:"targetDimseHost,omitempty"`
	// Target Dimse Port
	TargetDimsePort int `json:"targetDimsePort,omitempty"`
	// NATS URL
	NatsURL string `json:"natsUrl,omitempty"`
	// Make NATS Connection Secure
	NatsSecure bool `json:"natsSecure,omitempty"`
	// NATS Token Secret Name
	// +optional
	NatsTokenSecretName string `json:"natsTokenSecret,omitempty"`
	// DIMSE Proxy Spec
	Proxy ProxySpec `json:"proxy,omitempty"`
}

type ProxySpec struct {
	// Deployment Spec
	DeploymentSpec `json:",inline"`
}

// DimseProxyStatus defines the observed state of DimseProxy
type DimseProxyStatus struct {
	// Common Status Spec
	CommonStatusSpec `json:",inline"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
//+kubebuilder:printcolumn:name="Ready",type="string",JSONPath=".status.ready"

// DimseProxy is the Schema for the dimseproxies API
type DimseProxy struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   DimseProxySpec   `json:"spec,omitempty"`
	Status DimseProxyStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// DimseProxyList contains a list of DimseProxy
type DimseProxyList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []DimseProxy `json:"items"`
}

func init() {
	SchemeBuilder.Register(&DimseProxy{}, &DimseProxyList{})
}

func (i *DimseProxy) UpdateStatusSecondaryResources(kind string, resourceName string) {
	i.Status.SecondaryResources = UpdateStatusSecondaryResources(i.Status.SecondaryResources, kind, resourceName)
}
