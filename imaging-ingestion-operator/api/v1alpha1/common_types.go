/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

type ServiceSpec struct {
	// Image
	// +optional
	Image string `json:"image,omitempty"`
	// Container Concurrency
	Concurrency int64 `json:"concurrency,omitempty"`
	// Min Replicas
	MinReplicas int32 `json:"minReplicas,omitempty"`
	// Max Replicas
	MaxReplicas int32 `json:"maxReplicas,omitempty"`
}

type DeploymentSpec struct {
	// Image
	// +optional
	Image string `json:"image,omitempty"`
}

// CommonStatusSpec defines the Common Status Spec
type CommonStatusSpec struct {
	// Current phase of the operator.
	Phase StatusPhase `json:"phase"`
	// Human-readable message indicating details about current operator phase or error.
	Message string `json:"message"`
	// True if all resources are in a ready state and all work is done.
	Ready bool `json:"ready"`
	// A map of all the secondary resources types and names created for this CR. e.g "Deployment": [ "DeploymentName1", "DeploymentName2" ]
	SecondaryResources map[string][]string `json:"secondaryResources,omitempty"`
}

type StatusPhase string

var (
	NoPhase           StatusPhase
	PhaseReconciling  StatusPhase = "Reconciling"
	PhaseFailing      StatusPhase = "Failing"
	PhaseInitialising StatusPhase = "Initializing"
)
