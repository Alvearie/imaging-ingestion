/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"strconv"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func WadoService(cr *v1alpha1.DicomwebIngestionService, eventProcessorServiceEndpoint string) *kservingv1.Service {
	return &kservingv1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetWadoServiceName(cr),
			Namespace: cr.Namespace,
		},
		Spec: kservingv1.ServiceSpec{
			ConfigurationSpec: kservingv1.ConfigurationSpec{
				Template: kservingv1.RevisionTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Annotations: map[string]string{
							common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.WadoService.MinReplicas)),
							common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.WadoService.MaxReplicas)),
						},
					},
					Spec: kservingv1.RevisionSpec{
						PodSpec: corev1.PodSpec{
							Containers: []corev1.Container{
								{
									Image:           GetImage(cr.Spec.WadoService.Image, common.WadoServiceImage),
									ImagePullPolicy: cr.Spec.ImagePullSpec.ImagePullPolicy,
									Ports: []corev1.ContainerPort{
										{
											ContainerPort: 8080,
										},
									},
									Env: GetWadoServiceEnv(cr, []corev1.EnvVar{}, eventProcessorServiceEndpoint),
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
						ContainerConcurrency: &cr.Spec.WadoService.Concurrency,
					},
				},
			},
		},
	}
}

func WadoServiceSelector(cr *v1alpha1.DicomwebIngestionService) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetWadoServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func WadoServiceReconciled(cr *v1alpha1.DicomwebIngestionService, currentState *kservingv1.Service, eventProcessorServiceEndpoint string) *kservingv1.Service {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.ConfigurationSpec.Template.ObjectMeta.Annotations = map[string]string{
		common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.WadoService.MinReplicas)),
		common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.WadoService.MaxReplicas)),
	}
	container := reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0]
	container.Image = GetImage(cr.Spec.WadoService.Image, common.WadoServiceImage)
	container.ImagePullPolicy = cr.Spec.ImagePullSpec.ImagePullPolicy
	container.Env = GetWadoServiceEnv(cr, container.Env, eventProcessorServiceEndpoint)
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.ImagePullSecrets = cr.Spec.ImagePullSpec.ImagePullSecrets
	reconciled.Spec.ConfigurationSpec.Template.Spec.ContainerConcurrency = &cr.Spec.WadoService.Concurrency

	return reconciled
}

func GetWadoServiceName(cr *v1alpha1.DicomwebIngestionService) string {
	return cr.Name + "-wado"
}

func GetWadoServiceEnv(cr *v1alpha1.DicomwebIngestionService, existing []corev1.EnvVar, eventProcessorServiceEndpoint string) []corev1.EnvVar {
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
			Name:  "QUERY_ENDPOINT",
			Value: eventProcessorServiceEndpoint + "/query",
		},
	}
	env = MergeEnvs(existing, env)

	return env
}
