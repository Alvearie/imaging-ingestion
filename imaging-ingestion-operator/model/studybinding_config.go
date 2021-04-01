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

// StudyBindingConfig is the Bucket Secret
func StudyBindingConfig(cr *v1alpha1.DicomStudyBinding) *corev1.ConfigMap {
	return &corev1.ConfigMap{}
}

// StudyBindingConfigSelector is the  Bucket Secret Selector
func StudyBindingConfigSelector(cr *v1alpha1.DicomStudyBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.BindingConfigName,
		Namespace: cr.Namespace,
	}
}

func StudyBindingConfigReconciled(cr *v1alpha1.DicomStudyBinding, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	return reconciled
}
