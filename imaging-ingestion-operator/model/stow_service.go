/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"fmt"
	"strconv"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func StowService(cr *v1alpha1.DicomwebIngestionService) *kservingv1.Service {
	return &kservingv1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetStowServiceName(cr),
			Namespace: cr.Namespace,
		},
		Spec: kservingv1.ServiceSpec{
			ConfigurationSpec: kservingv1.ConfigurationSpec{
				Template: kservingv1.RevisionTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Annotations: map[string]string{
							common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.StowService.MinReplicas)),
							common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.StowService.MaxReplicas)),
						},
					},
					Spec: kservingv1.RevisionSpec{
						PodSpec: corev1.PodSpec{
							Containers: []corev1.Container{
								{
									Image:           GetImage(cr.Spec.StowService.Image, common.StowServiceImage),
									ImagePullPolicy: cr.Spec.ImagePullSpec.ImagePullPolicy,
									Ports: []corev1.ContainerPort{
										{
											ContainerPort: 8080,
										},
									},
									Env: GetStowServiceEnv(cr, []corev1.EnvVar{}),
									VolumeMounts: []corev1.VolumeMount{
										{
											Name:      "bucket-config-volume",
											MountPath: "/etc/bucket/config",
										},
										{
											Name:      "bucket-secret-volume",
											MountPath: "/etc/bucket/secret",
										},
									},
									Resources: GetResourceRequirements(common.DefaultKServiceMemoryRequest, common.DefaultKServiceMemoryLimit),
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
							},
							ImagePullSecrets: cr.Spec.ImagePullSpec.ImagePullSecrets,
						},
						ContainerConcurrency: &cr.Spec.StowService.Concurrency,
					},
				},
			},
		},
	}
}

func StowServiceSelector(cr *v1alpha1.DicomwebIngestionService) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetStowServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func StowServiceReconciled(cr *v1alpha1.DicomwebIngestionService, currentState *kservingv1.Service) *kservingv1.Service {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.ConfigurationSpec.Template.ObjectMeta.Annotations = map[string]string{
		common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.StowService.MinReplicas)),
		common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.StowService.MaxReplicas)),
	}
	container := reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0]
	container.Image = GetImage(cr.Spec.StowService.Image, common.StowServiceImage)
	container.ImagePullPolicy = cr.Spec.ImagePullSpec.ImagePullPolicy
	container.Env = GetStowServiceEnv(cr, container.Env)
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.ImagePullSecrets = cr.Spec.ImagePullSpec.ImagePullSecrets
	reconciled.Spec.ConfigurationSpec.Template.Spec.ContainerConcurrency = &cr.Spec.StowService.Concurrency

	return reconciled
}

func GetStowServiceName(cr *v1alpha1.DicomwebIngestionService) string {
	return cr.Name + "-stow"
}

func GetStowServiceEnv(cr *v1alpha1.DicomwebIngestionService, existing []corev1.EnvVar) []corev1.EnvVar {
	env := []corev1.EnvVar{
		{
			Name:  "BUCKET_CONFIG_PATH",
			Value: "/etc/bucket/config",
		},
		{
			Name:  "BUCKET_SECRET_PATH",
			Value: "/etc/bucket/secret",
		},
		{
			Name:  "PROVIDER_NAME",
			Value: cr.Spec.ProviderName,
		},
		{
			Name:  "WADO_INTERNAL_ENDPOINT",
			Value: cr.Status.WadoServiceInternalEndpoint + "/wado-rs",
		},
		{
			Name:  "WADO_EXTERNAL_ENDPOINT",
			Value: cr.Status.WadoServiceExternalEndpoint + "/wado-rs",
		},
		{
			Name:  "EVENT_SOURCE",
			Value: fmt.Sprintf("%s.%s.svc.cluster.local", GetStowServiceName(cr), cr.Namespace),
		},
		{
			Name:  "QUARKUS_SHUTDOWN_TIMEOUT",
			Value: "15S",
		},
	}
	env = MergeEnvs(existing, env)

	return env
}
