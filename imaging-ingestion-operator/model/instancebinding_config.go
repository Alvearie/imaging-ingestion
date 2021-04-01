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

// InstanceBindingConfig is the Bucket Secret
func InstanceBindingConfig(cr *v1alpha1.DicomInstanceBinding) *corev1.ConfigMap {
	return &corev1.ConfigMap{}
}

// InstanceBindingConfigSelector is the  Bucket Secret Selector
func InstanceBindingConfigSelector(cr *v1alpha1.DicomInstanceBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.BindingConfigName,
		Namespace: cr.Namespace,
	}
}

func InstanceBindingConfigReconciled(cr *v1alpha1.DicomInstanceBinding, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	return reconciled
}
