/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package v1alpha1

type ServiceSpec struct {
	// Image
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	Image string `json:"image,omitempty"`
	// Container Concurrency
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	Concurrency int64 `json:"concurrency,omitempty"`
	// Min Replicas
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	MinReplicas int32 `json:"minReplicas,omitempty"`
	// Max Replicas
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	MaxReplicas int32 `json:"maxReplicas,omitempty"`
}

type DeploymentSpec struct {
	// Image
	// +optional
	//+operator-sdk:csv:customresourcedefinitions:type=spec
	Image string `json:"image,omitempty"`
}

// CommonStatusSpec defines the Common Status Spec
type CommonStatusSpec struct {
	// Current phase of the operator
	//+operator-sdk:csv:customresourcedefinitions:type=status
	Phase StatusPhase `json:"phase"`
	// Human-readable message indicating details about current operator phase or error
	//+operator-sdk:csv:customresourcedefinitions:type=status
	Message string `json:"message"`
	// True if all resources are in a ready state and all work is done
	//+operator-sdk:csv:customresourcedefinitions:type=status
	Ready bool `json:"ready"`
	// A map of all the secondary resources types and names created for this CR. e.g "Deployment": [ "DeploymentName1", "DeploymentName2" ]
	//+operator-sdk:csv:customresourcedefinitions:type=status
	SecondaryResources map[string][]string `json:"secondaryResources,omitempty"`
}

type StatusPhase string

var (
	NoPhase           StatusPhase
	PhaseReconciling  StatusPhase = "Reconciling"
	PhaseFailing      StatusPhase = "Failing"
	PhaseInitialising StatusPhase = "Initializing"
)
