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

// StudyBindingSecret is the Bucket Secret
func StudyBindingSecret(cr *v1alpha1.DicomStudyBinding) *corev1.Secret {
	return &corev1.Secret{}
}

// StudyBindingSecretSelector is the  Bucket Secret Selector
func StudyBindingSecretSelector(cr *v1alpha1.DicomStudyBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.BindingSecretName,
		Namespace: cr.Namespace,
	}
}

func StudyBindingSecretReconciled(cr *v1alpha1.DicomStudyBinding, currentState *corev1.Secret) *corev1.Secret {
	reconciled := currentState.DeepCopy()
	return reconciled
}
