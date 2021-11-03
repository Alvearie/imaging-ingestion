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

func DimseIngestionDeployment(cr *v1alpha1.DimseIngestionService, sink string) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetDimseIngestionDeploymentName(cr),
			Namespace: cr.Namespace,
		},
		Spec: appsv1.DeploymentSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": GetDimseIngestionDeploymentName(cr),
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:      GetDimseIngestionDeploymentName(cr),
					Namespace: cr.Namespace,
					Labels: map[string]string{
						"app": GetDimseIngestionDeploymentName(cr),
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name:            "ingestion",
							Image:           GetImage(cr.Spec.DimseService.Image, common.DimseIngestionImage),
							ImagePullPolicy: corev1.PullAlways,
							Env:             GetDimseIngestionDeploymentEnv(cr, sink),
							EnvFrom: []corev1.EnvFromSource{
								{
									ConfigMapRef: &corev1.ConfigMapEnvSource{
										LocalObjectReference: corev1.LocalObjectReference{
											Name: GetDimseIngestionNatsConfigName(cr),
										},
									},
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "bucket-config-volume",
									MountPath: "/etc/bucket/config",
								},
								{
									Name:      "bucket-secret-volume",
									MountPath: "/etc/bucket/secret",
								},
								{
									Name:      "dimse-config-volume",
									MountPath: "/etc/dimse/config",
								},
							},
						},
					},
					Volumes: []corev1.Volume{
						{
							Name: "bucket-config-volume",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{
										Name: cr.Spec.BucketConfigName,
									},
								},
							},
						},
						{
							Name: "bucket-secret-volume",
							VolumeSource: corev1.VolumeSource{
								Secret: &corev1.SecretVolumeSource{
									SecretName: cr.Spec.BucketSecretName,
								},
							},
						},
						{
							Name: "dimse-config-volume",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{
										Name: GetDimseIngestionConfigName(cr.Name),
									},
								},
							},
						},
					},
					ImagePullSecrets: cr.Spec.ImagePullSecrets,
				},
			},
		},
	}
}

func DimseIngestionDeploymentSelector(cr *v1alpha1.DimseIngestionService) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetDimseIngestionDeploymentName(cr),
		Namespace: cr.Namespace,
	}
}

func DimseIngestionDeploymentReconciled(cr *v1alpha1.DimseIngestionService, currentState *appsv1.Deployment, sink string) *appsv1.Deployment {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.Template.Spec.Containers[0].Image = GetImage(cr.Spec.DimseService.Image, common.DimseIngestionImage)
	reconciled.Spec.Template.Spec.Containers[0].Env = GetDimseIngestionDeploymentEnv(cr, sink)
	reconciled.Spec.Template.Spec.ImagePullSecrets = cr.Spec.ImagePullSecrets

	return reconciled
}

func GetDimseIngestionDeploymentName(cr *v1alpha1.DimseIngestionService) string {
	return cr.Name + "-dimse"
}

func GetDimseIngestionDeploymentEnv(cr *v1alpha1.DimseIngestionService, sink string) []corev1.EnvVar {
	env := []corev1.EnvVar{
		{
			Name:  "K_SINK",
			Value: sink,
		},
		{
			Name:  "BUCKET_CONFIG_PATH",
			Value: "/etc/bucket/config",
		},
		{
			Name:  "BUCKET_SECRET_PATH",
			Value: "/etc/bucket/secret",
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
