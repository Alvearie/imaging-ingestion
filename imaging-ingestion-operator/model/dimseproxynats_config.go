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

// DimseProxyNatsConfig is the Nats Config
func DimseProxyNatsConfig(cr *v1alpha1.DimseProxy) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetDimseProxyNatsConfigName(cr),
			Namespace: cr.Namespace,
		},
		Data: GetDimseProxyNatsConfigData(cr),
	}
}

// DimseProxyNatsConfigSelector is the  Nats Config Selector
func DimseProxyNatsConfigSelector(cr *v1alpha1.DimseProxy) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetDimseProxyNatsConfigName(cr),
		Namespace: cr.Namespace,
	}
}

func DimseProxyNatsConfigReconciled(cr *v1alpha1.DimseProxy, currentState *corev1.ConfigMap) *corev1.ConfigMap {
	reconciled := currentState.DeepCopy()
	reconciled.Data = GetDimseProxyNatsConfigData(cr)

	return reconciled
}

func GetDimseProxyNatsConfigName(cr *v1alpha1.DimseProxy) string {
	return cr.Name + "-proxy-nats-config"
}

func GetDimseProxyNatsConfigData(cr *v1alpha1.DimseProxy) map[string]string {
	config := map[string]string{
		"DIMSE_CALLED_AET":       cr.Spec.ApplicationEntityTitle,
		"DIMSE_CALLED_HOST":      cr.Spec.TargetDimseHost,
		"DIMSE_CALLED_PORT":      strconv.Itoa(cr.Spec.TargetDimsePort),
		"DIMSE_NATS_URL":         cr.Spec.NatsURL,
		"DIMSE_NATS_TLS_ENABLED": strconv.FormatBool(cr.Spec.NatsSecure),
	}

	if cr.Spec.NatsSubjectRoot != "" {
		config["DIMSE_NATS_SUBJECT_ROOT"] = cr.Spec.NatsSubjectRoot
	}

	return config
}
