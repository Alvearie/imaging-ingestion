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

func InstanceBindingService(cr *v1alpha1.DicomInstanceBinding) *kservingv1.Service {
	return &kservingv1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetInstanceBindingServiceName(cr),
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"serving.knative.dev/visibility": "cluster-local",
			},
		},
		Spec: kservingv1.ServiceSpec{
			ConfigurationSpec: kservingv1.ConfigurationSpec{
				Template: kservingv1.RevisionTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Annotations: map[string]string{
							common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.InstanceBinding.MinReplicas)),
							common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.InstanceBinding.MaxReplicas)),
						},
					},
					Spec: kservingv1.RevisionSpec{
						PodSpec: corev1.PodSpec{
							Containers: []corev1.Container{
								{
									Image:           GetImage(cr.Spec.InstanceBinding.Image, common.InstanceBindingServiceImage),
									ImagePullPolicy: cr.Spec.ImagePullSpec.ImagePullPolicy,
									Ports: []corev1.ContainerPort{
										{
											ContainerPort: 8080,
										},
									},
									Env: []corev1.EnvVar{
										{
											Name:  "TMPDIR",
											Value: "/",
										},
										{
											Name:  "INSECURE_SKIP_VERIFY",
											Value: "true",
										},
									},
									EnvFrom: []corev1.EnvFromSource{
										{
											ConfigMapRef: &corev1.ConfigMapEnvSource{
												LocalObjectReference: corev1.LocalObjectReference{
													Name: cr.Spec.BindingConfigName,
												},
											},
										},
										{
											SecretRef: &corev1.SecretEnvSource{
												LocalObjectReference: corev1.LocalObjectReference{
													Name: cr.Spec.BindingSecretName,
												},
											},
										},
									},
								},
							},
							ImagePullSecrets: cr.Spec.ImagePullSpec.ImagePullSecrets,
						},
						ContainerConcurrency: &cr.Spec.InstanceBinding.Concurrency,
					},
				},
			},
		},
	}
}

func InstanceBindingServiceSelector(cr *v1alpha1.DicomInstanceBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetInstanceBindingServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func InstanceBindingServiceReconciled(cr *v1alpha1.DicomInstanceBinding, currentState *kservingv1.Service) *kservingv1.Service {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.ConfigurationSpec.Template.ObjectMeta.Annotations = map[string]string{
		common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.InstanceBinding.MinReplicas)),
		common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.InstanceBinding.MaxReplicas)),
	}
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0].Image = GetImage(cr.Spec.InstanceBinding.Image, common.InstanceBindingServiceImage)
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0].ImagePullPolicy = cr.Spec.ImagePullSpec.ImagePullPolicy
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.ImagePullSecrets = cr.Spec.ImagePullSpec.ImagePullSecrets
	reconciled.Spec.ConfigurationSpec.Template.Spec.ContainerConcurrency = &cr.Spec.InstanceBinding.Concurrency

	return reconciled
}

func GetInstanceBindingServiceName(cr *v1alpha1.DicomInstanceBinding) string {
	return cr.Name + "-instance-binding"
}
