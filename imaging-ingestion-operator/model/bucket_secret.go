/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// BucketSecret is the Bucket Secret
func BucketSecret() *corev1.Secret {
	return &corev1.Secret{}
}

// BucketSecretSelector is the  Bucket Secret Selector
func BucketSecretSelector(name, namespace string) client.ObjectKey {
	return client.ObjectKey{
		Name:      name,
		Namespace: namespace,
	}
}

func BucketSecretReconciled(currentState *corev1.Secret) *corev1.Secret {
	reconciled := currentState.DeepCopy()
	return reconciled
}
