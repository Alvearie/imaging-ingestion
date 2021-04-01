/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// InstanceBindingSecret is the Bucket Secret
func InstanceBindingSecret(cr *v1alpha1.DicomInstanceBinding) *corev1.Secret {
	return &corev1.Secret{}
}

// InstanceBindingSecretSelector is the  Bucket Secret Selector
func InstanceBindingSecretSelector(cr *v1alpha1.DicomInstanceBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.BindingSecretName,
		Namespace: cr.Namespace,
	}
}

func InstanceBindingSecretReconciled(cr *v1alpha1.DicomInstanceBinding, currentState *corev1.Secret) *corev1.Secret {
	reconciled := currentState.DeepCopy()
	return reconciled
}
