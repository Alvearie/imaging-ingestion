/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func DimseConfig(name, namespace string) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      name,
			Namespace: namespace,
		},
		Data: map[string]string{
			"IMAGE_CUIDS":  GetDefaultCuids(),
			"IMAGE_TSUIDS": GetDefaultTsuids(),
		},
	}
}

func DimseConfigSelector(name, namespace string) client.ObjectKey {
	return client.ObjectKey{
		Name:      name,
		Namespace: namespace,
	}
}

func DimseConfigReconciled(currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	return reconciled
}

func GetDimseIngestionConfigName(resourceName string) string {
	return resourceName + "-dimse-config"
}

func GetDimseProxyConfigName(resourceName string) string {
	return resourceName + "-proxy-config"
}
