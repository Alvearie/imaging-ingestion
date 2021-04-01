/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package common

import (
	"time"

	"k8s.io/client-go/discovery"
	"sigs.k8s.io/controller-runtime/pkg/manager"
)

// Background represents a procedure that runs in the background, periodically auto-detecting features
type Background struct {
	dc     discovery.DiscoveryInterface
	ticker *time.Ticker
}

// New creates a new auto-detect runner
func NewAutoDetect(mgr manager.Manager) (*Background, error) {
	dc, err := discovery.NewDiscoveryClientForConfig(mgr.GetConfig())
	if err != nil {
		return nil, err
	}

	return &Background{dc: dc}, nil
}

// Start initializes the auto-detection process that runs in the background
func (b *Background) Start() {
	// periodically attempts to auto detect all the capabilities for this operator
	b.ticker = time.NewTicker(AutoDetectTick)

	go func() {
		b.autoDetectCapabilities()

		for range b.ticker.C {
			b.autoDetectCapabilities()
		}
	}()
}

// Stop causes the background process to stop auto detecting capabilities
func (b *Background) Stop() {
	b.ticker.Stop()
}

func (b *Background) autoDetectCapabilities() {
	b.detectOpenshift()
	b.detectKnativeServing()
	b.detectKnativeEventing()
}

func (b *Background) detectKnativeServing() {
	apiGroupVersion := "serving.knative.dev/v1"
	kind := KnativeServingKind
	stateManager := GetStateManager()
	exists, _ := ResourceExists(b.dc, apiGroupVersion, kind)
	if exists {
		stateManager.SetState(KnativeServingKind, true)
	} else {
		stateManager.SetState(KnativeServingKind, false)
	}
}

func (b *Background) detectKnativeEventing() {
	apiGroupVersion := "eventing.knative.dev/v1"
	kind := KnativeEventingKind
	stateManager := GetStateManager()
	exists, _ := ResourceExists(b.dc, apiGroupVersion, kind)
	if exists {
		stateManager.SetState(KnativeEventingKind, true)
	} else {
		stateManager.SetState(KnativeEventingKind, false)
	}
}

func (b *Background) detectOpenshift() {
	apiGroupVersion := "operator.openshift.io/v1"
	kind := OpenShiftAPIServerKind
	stateManager := GetStateManager()
	isOpenshift, _ := ResourceExists(b.dc, apiGroupVersion, kind)
	if isOpenshift {
		// Set state that its Openshift (helps to differentiate between openshift and kubernetes)
		stateManager.SetState(OpenShiftAPIServerKind, true)
	} else {
		stateManager.SetState(OpenShiftAPIServerKind, false)
	}
}

// ResourceExists returns true if the given resource kind exists
// in the given api groupversion
func ResourceExists(dc discovery.DiscoveryInterface, apiGroupVersion, kind string) (bool, error) {
	_, apiLists, err := dc.ServerGroupsAndResources()
	if err != nil {
		return false, err
	}
	for _, apiList := range apiLists {
		if apiList.GroupVersion == apiGroupVersion {
			for _, r := range apiList.APIResources {
				if r.Kind == kind {
					return true, nil
				}
			}
		}
	}
	return false, nil
}

func IsKnativeAvailable() bool {
	stateManager := GetStateManager()
	servingAvailable, _ := stateManager.GetState(KnativeServingKind).(bool)
	eventingAvailable, _ := stateManager.GetState(KnativeEventingKind).(bool)

	if servingAvailable && eventingAvailable {
		return true
	}

	return false
}
