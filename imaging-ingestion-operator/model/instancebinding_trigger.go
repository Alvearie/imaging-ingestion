/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	v1 "knative.dev/pkg/apis/duck/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func InstanceBindingTrigger(cr *v1alpha1.DicomInstanceBinding, broker string) *keventingv1.Trigger {
	return &keventingv1.Trigger{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetInstanceBindingTriggerName(cr),
			Namespace: cr.Namespace,
		},

		Spec: keventingv1.TriggerSpec{
			Broker: broker,
			Subscriber: v1.Destination{
				Ref: &v1.KReference{
					APIVersion: "serving.knative.dev/v1",
					Kind:       "Service",
					Name:       GetInstanceBindingServiceName(cr),
				},
			},
			Filter: &keventingv1.TriggerFilter{
				Attributes: keventingv1.TriggerFilterAttributes{
					"type": string(common.DicomAvailableEventType),
				},
			},
		},
	}
}

func InstanceBindingTriggerSelector(cr *v1alpha1.DicomInstanceBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetInstanceBindingTriggerName(cr),
		Namespace: cr.Namespace,
	}
}

func InstanceBindingTriggerReconciled(cr *v1alpha1.DicomInstanceBinding, currentState *keventingv1.Trigger) *keventingv1.Trigger {
	reconciled := currentState.DeepCopy()
	return reconciled
}

func GetInstanceBindingTriggerName(cr *v1alpha1.DicomInstanceBinding) string {
	return cr.Name + "-instance-trigger"
}
