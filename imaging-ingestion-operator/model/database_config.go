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

// DatabaseConfig is the Database Secret
func DatabaseConfig(cr *v1alpha1.DicomEventDrivenIngestion) *corev1.ConfigMap {
	return &corev1.ConfigMap{}
}

// DatabaseConfigSelector is the  Database Secret Selector
func DatabaseConfigSelector(cr *v1alpha1.DicomEventDrivenIngestion) client.ObjectKey {
	return client.ObjectKey{
		Name:      cr.Spec.DatabaseConfigName,
		Namespace: cr.Namespace,
	}
}

func DatabaseConfigReconciled(cr *v1alpha1.DicomEventDrivenIngestion, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	return reconciled
}
