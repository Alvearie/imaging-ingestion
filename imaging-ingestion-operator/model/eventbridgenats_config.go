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

// EventBridgeNatsConfig is the Nats Config
func EventBridgeNatsConfig(cr *v1alpha1.DicomEventBridge) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventBridgeNatsConfigName(cr),
			Namespace: cr.Namespace,
		},
		Data: GetEventBridgeNatsConfigData(cr),
	}
}

// EventBridgeNatsConfigSelector is the  Nats Config Selector
func EventBridgeNatsConfigSelector(cr *v1alpha1.DicomEventBridge) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventBridgeNatsConfigName(cr),
		Namespace: cr.Namespace,
	}
}

func EventBridgeNatsConfigReconciled(cr *v1alpha1.DicomEventBridge, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	reconciled.Data = GetEventBridgeNatsConfigData(cr)

	return reconciled
}

func GetEventBridgeNatsConfigName(cr *v1alpha1.DicomEventBridge) string {
	return cr.Name + "-bridge-nats-config"
}

func GetEventBridgeNatsConfigData(cr *v1alpha1.DicomEventBridge) map[string]string {
	config := map[string]string{
		"NATS_URL":         cr.Spec.NatsURL,
		"NATS_TLS_ENABLED": strconv.FormatBool(cr.Spec.NatsSecure),
	}

	if cr.Spec.NatsSubjectRoot != "" {
		config["NATS_SUBJECT_ROOT"] = cr.Spec.NatsSubjectRoot
	}

	return config
}
