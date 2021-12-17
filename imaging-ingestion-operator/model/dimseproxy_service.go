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

func DimseProxyService(cr *v1alpha1.DimseProxy) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetDimseProxyServiceName(cr),
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Selector: map[string]string{
				"app": GetDimseProxyDeploymentName(cr),
			},
			Ports: []corev1.ServicePort{
				{
					Name:     "tcp-proxy",
					Port:     11112,
					Protocol: corev1.ProtocolTCP,
				},
			},
		},
	}
}

func DimseProxyServiceSelector(cr *v1alpha1.DimseProxy) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetDimseProxyServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func DimseProxyServiceReconciled(cr *v1alpha1.DimseProxy, currentState *corev1.Service) *corev1.Service {
	reconciled := currentState.DeepCopy()

	return reconciled
}

func GetDimseProxyServiceName(cr *v1alpha1.DimseProxy) string {
	return cr.Name + "-dimse-proxy"
}
