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

func StudyBindingService(cr *v1alpha1.DicomStudyBinding) *kservingv1.Service {
	return &kservingv1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetStudyBindingServiceName(cr),
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
							common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.StudyBinding.MinReplicas)),
							common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.StudyBinding.MaxReplicas)),
						},
					},
					Spec: kservingv1.RevisionSpec{
						PodSpec: corev1.PodSpec{
							Containers: []corev1.Container{
								{
									Image:           GetImage(cr.Spec.StudyBinding.Image, common.StudyBindingServiceImage),
									ImagePullPolicy: corev1.PullAlways,
									Ports: []corev1.ContainerPort{
										{
											ContainerPort: 8080,
										},
									},
									Env: []corev1.EnvVar{
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
							ImagePullSecrets: cr.Spec.ImagePullSecrets,
						},
						ContainerConcurrency: &cr.Spec.StudyBinding.Concurrency,
					},
				},
			},
		},
	}
}

func StudyBindingServiceSelector(cr *v1alpha1.DicomStudyBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetStudyBindingServiceName(cr),
		Namespace: cr.Namespace,
	}
}

func StudyBindingServiceReconciled(cr *v1alpha1.DicomStudyBinding, currentState *kservingv1.Service) *kservingv1.Service {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.ConfigurationSpec.Template.ObjectMeta.Annotations = map[string]string{
		common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.StudyBinding.MinReplicas)),
		common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.StudyBinding.MaxReplicas)),
	}
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0].Image = GetImage(cr.Spec.StudyBinding.Image, common.StudyBindingServiceImage)
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.ImagePullSecrets = cr.Spec.ImagePullSecrets
	reconciled.Spec.ConfigurationSpec.Template.Spec.ContainerConcurrency = &cr.Spec.StudyBinding.Concurrency

	return reconciled
}

func GetStudyBindingServiceName(cr *v1alpha1.DicomStudyBinding) string {
	return cr.Name + "-study-binding"
}
