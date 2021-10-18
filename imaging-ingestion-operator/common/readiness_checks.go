/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package common

import (
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	ksourcesv1 "knative.dev/eventing/pkg/apis/sources/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
)

const (
	ConditionStatusSuccess = "True"
)

func IsDeploymentReady(deployment *appsv1.Deployment) (bool, error) {
	if deployment == nil {
		return false, nil
	}
	// A deployment has an array of conditions
	for _, condition := range deployment.Status.Conditions {
		// One failure condition exists, if this exists, return the Reason
		if condition.Type == appsv1.DeploymentReplicaFailure {
			return false, errors.Errorf(condition.Reason)
			// A successful deployment will have the progressing condition type as true
		} else if condition.Type == appsv1.DeploymentProgressing && condition.Status != ConditionStatusSuccess {
			return false, nil
		}
	}
	return true, nil
}

func IsServiceReady(service *kservingv1.Service) (bool, error) {
	if service == nil {
		return false, nil
	}

	for _, condition := range service.Status.Conditions {
		if condition.Type == kservingv1.ServiceConditionReady && condition.Status != ConditionStatusSuccess {
			return false, nil
		}
	}
	return true, nil
}

func IsBrokerReady(broker *keventingv1.Broker) (bool, error) {
	if broker == nil {
		return false, nil
	}

	for _, condition := range broker.Status.Conditions {
		if condition.Type == keventingv1.BrokerConditionReady && condition.Status != ConditionStatusSuccess {
			return false, nil
		}
	}
	return true, nil
}

func IsTriggerReady(trigger *keventingv1.Trigger) (bool, error) {
	if trigger == nil {
		return false, nil
	}

	for _, condition := range trigger.Status.Conditions {
		if condition.Type == keventingv1.TriggerConditionReady && condition.Status != ConditionStatusSuccess {
			return false, nil
		}
	}
	return true, nil
}

func IsSinkBindingReady(binding *ksourcesv1.SinkBinding) (bool, error) {
	if binding == nil {
		return false, nil
	}

	for _, condition := range binding.Status.Conditions {
		if condition.Type == ksourcesv1.SinkBindingConditionReady && condition.Status != ConditionStatusSuccess {
			return false, nil
		}
	}
	return true, nil
}
