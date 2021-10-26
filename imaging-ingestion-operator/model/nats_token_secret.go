/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// NatsTokenSecret is the Nats Token Secret
func NatsTokenSecret() *corev1.Secret {
	return &corev1.Secret{}
}

// NatsTokenSecretSelector is the  Nats Token Secret Selector
func NatsTokenSecretSelector(name, namespace string) client.ObjectKey {
	return client.ObjectKey{
		Name:      name,
		Namespace: namespace,
	}
}

func NatsTokenSecretReconciled(currentState *corev1.Secret) *corev1.Secret {
	reconciled := currentState.DeepCopy()
	return reconciled
}
