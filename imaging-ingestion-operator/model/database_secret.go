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

// DatabaseSecret is the Database Secret
func DatabaseSecret(cr *v1alpha1.DicomEventDrivenIngestion) *corev1.Secret {
	return &corev1.Secret{}
}

// DatabaseSecretSelector is the  Database Secret Selector
func DatabaseSecretSelector(cr *v1alpha1.DicomEventDrivenIngestion) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.DatabaseSecretName,
		Namespace: cr.Namespace,
	}
}

func DatabaseSecretReconciled(cr *v1alpha1.DicomEventDrivenIngestion, currentState *corev1.Secret) *corev1.Secret {
	reconciled := currentState.DeepCopy()
	return reconciled
}
