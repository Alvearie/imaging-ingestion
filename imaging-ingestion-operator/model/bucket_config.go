/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// BucketConfig is the Bucket Config
func BucketConfig() *corev1.ConfigMap {
	return &corev1.ConfigMap{}
}

// BucketConfigSelector is the  Bucket Config Selector
func BucketConfigSelector(name, namespace string) client.ObjectKey {
	return client.ObjectKey{
		Name:      name,
		Namespace: namespace,
	}
}

func BucketConfigReconciled(currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	return reconciled
}
