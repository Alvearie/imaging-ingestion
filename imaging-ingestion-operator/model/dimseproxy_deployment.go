/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func DimseProxyDeployment(cr *v1alpha1.DimseProxy) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetDimseProxyDeploymentName(cr),
			Namespace: cr.Namespace,
		},
		Spec: appsv1.DeploymentSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": GetDimseProxyDeploymentName(cr),
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:      GetDimseProxyDeploymentName(cr),
					Namespace: cr.Namespace,
					Labels: map[string]string{
						"app": GetDimseProxyDeploymentName(cr),
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name:            "proxy",
							Image:           GetImage(cr.Spec.Proxy.Image, common.DimseProxyImage),
							ImagePullPolicy: cr.Spec.ImagePullSpec.ImagePullPolicy,
							Ports: []corev1.ContainerPort{
								{
									ContainerPort: 11112,
								},
							},
							Env: GetDimseProxyDeploymentEnv(cr),
							EnvFrom: []corev1.EnvFromSource{
								{
									ConfigMapRef: &corev1.ConfigMapEnvSource{
										LocalObjectReference: corev1.LocalObjectReference{
											Name: GetDimseProxyNatsConfigName(cr),
										},
									},
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "dimse-config-volume",
									MountPath: "/etc/dimse/config",
								},
							},
							Resources: GetResourceRequirements(common.DefaultDeploymentMemoryRequest, common.DefaultDeploymentMemoryLimit),
						},
					},
					Volumes: []corev1.Volume{
						{
							Name: "dimse-config-volume",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{
										Name: GetDimseProxyConfigName(cr.Name),
									},
								},
							},
						},
					},
					ImagePullSecrets: cr.Spec.ImagePullSpec.ImagePullSecrets,
				},
			},
		},
	}
}

func DimseProxyDeploymentSelector(cr *v1alpha1.DimseProxy) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetDimseProxyDeploymentName(cr),
		Namespace: cr.Namespace,
	}
}

func DimseProxyDeploymentReconciled(cr *v1alpha1.DimseProxy, currentState *appsv1.Deployment) *appsv1.Deployment {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.Template.Spec.Containers[0].Image = GetImage(cr.Spec.Proxy.Image, common.DimseProxyImage)
	reconciled.Spec.Template.Spec.Containers[0].ImagePullPolicy = cr.Spec.ImagePullSpec.ImagePullPolicy
	reconciled.Spec.Template.Spec.Containers[0].Env = GetDimseProxyDeploymentEnv(cr)
	reconciled.Spec.Template.Spec.ImagePullSecrets = cr.Spec.ImagePullSpec.ImagePullSecrets

	return reconciled
}

func GetDimseProxyDeploymentName(cr *v1alpha1.DimseProxy) string {
	return cr.Name + "-dimse-proxy"
}

func GetDimseProxyDeploymentEnv(cr *v1alpha1.DimseProxy) []corev1.EnvVar {
	env := []corev1.EnvVar{
		{
			Name:  "DIMSE_ACTOR",
			Value: "SCP",
		},
		{
			Name:  "DIMSE_CONFIG_PATH",
			Value: "/etc/dimse/config",
		},
	}

	if cr.Spec.NatsTokenSecretName != "" {
		env = append(env, corev1.EnvVar{
			Name: "DIMSE_NATS_AUTH_TOKEN",
			ValueFrom: &corev1.EnvVarSource{
				SecretKeyRef: &corev1.SecretKeySelector{
					LocalObjectReference: corev1.LocalObjectReference{
						Name: cr.Spec.NatsTokenSecretName,
					},
					Key: "token",
				},
			},
		})
	}

	return env
}
