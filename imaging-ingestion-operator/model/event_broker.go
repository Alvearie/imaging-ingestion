/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func EventBroker(cr *v1alpha1.DicomEventDrivenIngestion) *keventingv1.Broker {
	return &keventingv1.Broker{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventBrokerName(cr.Name),
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"eventing.knative.dev/visibility": "cluster-local",
			},
		},
	}
}

func EventBrokerSelector(cr *v1alpha1.DicomEventDrivenIngestion) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventBrokerName(cr.Name),
		Namespace: cr.Namespace,
	}
}

func EventBrokerReconciled(cr *v1alpha1.DicomEventDrivenIngestion, currentState *keventingv1.Broker) *keventingv1.Broker {
	reconciled := currentState.DeepCopy()
	return reconciled
}

func GetEventBrokerName(resourceName string) string {
	return resourceName + "-broker"
}
