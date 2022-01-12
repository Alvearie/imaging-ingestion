/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func EventBridgeService(cr *v1alpha1.DicomEventBridge) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventBridgeServiceName(cr),
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Selector: map[string]string{
				"app": GetEventBridgeDeploymentName(cr),
			},
			Ports: []corev1.ServicePort{
				{
					Name:       "http-bridge",
					Port:       80,
					TargetPort: intstr.FromInt(8080),
					Protocol:   corev1.ProtocolTCP,
				},
			},
		},
	}
}

func EventBridgeServiceSelector(cr *v1alpha1.DicomEventBridge) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventBridgeServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func EventBridgeServiceReconciled(cr *v1alpha1.DicomEventBridge, currentState *corev1.Service) *corev1.Service {
	reconciled := currentState.DeepCopy()

	return reconciled
}

func GetEventBridgeServiceName(cr *v1alpha1.DicomEventBridge) string {
	return cr.Name + "-event-bridge"
}
