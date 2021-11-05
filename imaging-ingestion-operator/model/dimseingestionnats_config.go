/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"strconv"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// DimseIngestionNatsConfig is the Nats Config
func DimseIngestionNatsConfig(cr *v1alpha1.DimseIngestionService) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetDimseIngestionNatsConfigName(cr),
			Namespace: cr.Namespace,
		},
		Data: GetDimseIngestionNatsConfigData(cr),
	}
}

// DimseIngestionNatsConfigSelector is the  Nats Config Selector
func DimseIngestionNatsConfigSelector(cr *v1alpha1.DimseIngestionService) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetDimseIngestionNatsConfigName(cr),
		Namespace: cr.Namespace,
	}
}

func DimseIngestionNatsConfigReconciled(cr *v1alpha1.DimseIngestionService, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	reconciled.Data = GetDimseIngestionNatsConfigData(cr)

	return reconciled
}

func GetDimseIngestionNatsConfigName(cr *v1alpha1.DimseIngestionService) string {
	return cr.Name + "-ingestion-nats-config"
}

func GetDimseIngestionNatsConfigData(cr *v1alpha1.DimseIngestionService) map[string]string {
	config := map[string]string{
		"DIMSE_AE":               cr.Spec.ApplicationEntityTitle,
		"DIMSE_NATS_URL":         cr.Spec.NatsURL,
		"DIMSE_NATS_TLS_ENABLED": strconv.FormatBool(cr.Spec.NatsSecure),
	}

	if cr.Spec.NatsSubjectRoot != "" {
		config["DIMSE_NATS_SUBJECT_ROOT"] = cr.Spec.NatsSubjectRoot
	}

	return config
}
