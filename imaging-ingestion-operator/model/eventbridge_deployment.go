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

func EventBridgeDeployment(cr *v1alpha1.DicomEventBridge, core *v1alpha1.DicomEventDrivenIngestion) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventBridgeDeploymentName(cr),
			Namespace: cr.Namespace,
		},
		Spec: appsv1.DeploymentSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": GetEventBridgeDeploymentName(cr),
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:      GetEventBridgeDeploymentName(cr),
					Namespace: cr.Namespace,
					Labels: map[string]string{
						"app": GetEventBridgeDeploymentName(cr),
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name:            "proxy",
							Image:           GetImage(cr.Spec.EventBridge.Image, common.EventBridgeImage),
							ImagePullPolicy: corev1.PullAlways,
							Ports: []corev1.ContainerPort{
								{
									ContainerPort: 8080,
								},
							},
							Env: GetEventBridgeDeploymentEnv(cr, core),
							EnvFrom: []corev1.EnvFromSource{
								{
									ConfigMapRef: &corev1.ConfigMapEnvSource{
										LocalObjectReference: corev1.LocalObjectReference{
											Name: GetEventBridgeNatsConfigName(cr),
										},
									},
								},
							},
							VolumeMounts: GetEventBridgeDeploymentVolumeMounts(cr),
						},
					},
					Volumes:          GetEventBridgeDeploymentVolumes(cr),
					ImagePullSecrets: cr.Spec.ImagePullSecrets,
				},
			},
		},
	}
}

func EventBridgeDeploymentSelector(cr *v1alpha1.DicomEventBridge) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventBridgeDeploymentName(cr),
		Namespace: cr.Namespace,
	}
}

func EventBridgeDeploymentReconciled(cr *v1alpha1.DicomEventBridge, currentState *appsv1.Deployment, core *v1alpha1.DicomEventDrivenIngestion) *appsv1.Deployment {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.Template.Spec.Containers[0].Image = GetImage(cr.Spec.EventBridge.Image, common.EventBridgeImage)
	reconciled.Spec.Template.Spec.Containers[0].Env = GetEventBridgeDeploymentEnv(cr, core)
	reconciled.Spec.Template.Spec.ImagePullSecrets = cr.Spec.ImagePullSecrets

	return reconciled
}

func GetEventBridgeDeploymentName(cr *v1alpha1.DicomEventBridge) string {
	return cr.Name + "-event-bridge"
}

func GetEventBridgeDeploymentVolumes(cr *v1alpha1.DicomEventBridge) []corev1.Volume {
	volumes := []corev1.Volume{}

	if cr.Spec.Role == string(common.BridgeRoleHub) {
		volumes = []corev1.Volume{
			{
				Name: "edge-location-config",
				VolumeSource: corev1.VolumeSource{
					ConfigMap: &corev1.ConfigMapVolumeSource{
						LocalObjectReference: corev1.LocalObjectReference{
							Name: GetEventBridgeEdgeLocationConfigName(cr),
						},
					},
				},
			},
		}
	}

	return volumes
}

func GetEventBridgeDeploymentVolumeMounts(cr *v1alpha1.DicomEventBridge) []corev1.VolumeMount {
	mounts := []corev1.VolumeMount{}

	if cr.Spec.Role == string(common.BridgeRoleHub) {
		mounts = []corev1.VolumeMount{
			{
				Name:      "edge-location-config",
				MountPath: "/etc/config",
			},
		}
	}

	return mounts
}

func GetEventBridgeDeploymentEnv(cr *v1alpha1.DicomEventBridge, core *v1alpha1.DicomEventDrivenIngestion) []corev1.EnvVar {
	env := []corev1.EnvVar{
		{
			Name:  "BRIDGE_ROLE",
			Value: cr.Spec.Role,
		},
		{
			Name:  "INSECURE_SKIP_VERIFY",
			Value: "true",
		},
		{
			Name:  "K_SINK",
			Value: core.Status.BrokerEndpoint,
		},
	}

	if cr.Spec.Role == string(common.BridgeRoleHub) {
		env = append(env, corev1.EnvVar{
			Name:  "EDGE_LOCATION_CONFIG",
			Value: "/etc/config/EDGE_LOCATION_CONFIG",
		})
	}

	if cr.Spec.Role == string(common.BridgeRoleEdge) {
		env = append(env, corev1.EnvVar{
			Name:  "EDGE_MAILBOX_ID",
			Value: cr.Spec.EdgeMailbox,
		})
	}

	if cr.Spec.NatsTokenSecretName != "" {
		env = append(env, corev1.EnvVar{
			Name: "NATS_AUTH_TOKEN",
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
