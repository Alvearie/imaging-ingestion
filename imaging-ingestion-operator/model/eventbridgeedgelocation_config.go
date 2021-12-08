/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// EventBridgeEdgeLocationConfig is the Edge Location Config
func EventBridgeEdgeLocationConfig(cr *v1alpha1.DicomEventBridge) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventBridgeEdgeLocationConfigName(cr),
			Namespace: cr.Namespace,
		},
		Data: map[string]string{
			"EDGE_LOCATION_CONFIG": `
{
	"location_name": "edge_mailbox"
}
`,
		},
	}
}

// EventBridgeEdgeLocationConfigSelector is the  Edge Location Config Selector
func EventBridgeEdgeLocationConfigSelector(cr *v1alpha1.DicomEventBridge) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventBridgeEdgeLocationConfigName(cr),
		Namespace: cr.Namespace,
	}
}

func EventBridgeEdgeLocationConfigReconciled(cr *v1alpha1.DicomEventBridge, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()

	return reconciled
}

func GetEventBridgeEdgeLocationConfigName(cr *v1alpha1.DicomEventBridge) string {
	return cr.Name + "-edge-location-config"
}
