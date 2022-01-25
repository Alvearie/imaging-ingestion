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

func EventProcessorService(cr *v1alpha1.DicomEventDrivenIngestion) *kservingv1.Service {
	return &kservingv1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetEventProcessorServiceName(cr.Name),
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
							common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.EventProcessor.MinReplicas)),
							common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.EventProcessor.MaxReplicas)),
						},
					},
					Spec: kservingv1.RevisionSpec{
						PodSpec: corev1.PodSpec{
							Containers: []corev1.Container{
								{
									Image:           GetImage(cr.Spec.EventProcessor.Image, common.EventProcessorServiceImage),
									ImagePullPolicy: cr.Spec.ImagePullSpec.ImagePullPolicy,
									Ports: []corev1.ContainerPort{
										{
											ContainerPort: 8080,
										},
									},
									Env: []corev1.EnvVar{
										{
											Name:  "EVENT_SOURCE",
											Value: fmt.Sprintf("%s.%s.svc.cluster.local", GetEventProcessorServiceName(cr.Name), cr.Namespace),
										},
									},
									EnvFrom: []corev1.EnvFromSource{
										{
											ConfigMapRef: &corev1.ConfigMapEnvSource{
												LocalObjectReference: corev1.LocalObjectReference{
													Name: cr.Spec.DatabaseConfigName,
												},
											},
										},
										{
											SecretRef: &corev1.SecretEnvSource{
												LocalObjectReference: corev1.LocalObjectReference{
													Name: cr.Spec.DatabaseSecretName,
												},
											},
										},
									},
								},
							},
							ImagePullSecrets: cr.Spec.ImagePullSpec.ImagePullSecrets,
						},
						ContainerConcurrency: &cr.Spec.EventProcessor.Concurrency,
					},
				},
			},
		},
	}
}

func EventProcessorServiceSelector(cr *v1alpha1.DicomEventDrivenIngestion) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetEventProcessorServiceName(cr.Name),
		Namespace: cr.Namespace,
	}
}

func EventProcessorServiceReconciled(cr *v1alpha1.DicomEventDrivenIngestion, currentState *kservingv1.Service) *kservingv1.Service {
	reconciled := currentState.DeepCopy()
	reconciled.Spec.ConfigurationSpec.Template.ObjectMeta.Annotations = map[string]string{
		common.MinScaleAnnotation: strconv.Itoa(int(cr.Spec.EventProcessor.MinReplicas)),
		common.MaxScaleAnnotation: strconv.Itoa(int(cr.Spec.EventProcessor.MaxReplicas)),
	}
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0].Image = GetImage(cr.Spec.EventProcessor.Image, common.EventProcessorServiceImage)
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.Containers[0].ImagePullPolicy = cr.Spec.ImagePullSpec.ImagePullPolicy
	reconciled.Spec.ConfigurationSpec.Template.Spec.PodSpec.ImagePullSecrets = cr.Spec.ImagePullSpec.ImagePullSecrets
	reconciled.Spec.ConfigurationSpec.Template.Spec.ContainerConcurrency = &cr.Spec.EventProcessor.Concurrency

	return reconciled
}

func GetEventProcessorServiceName(resourceName string) string {
	return resourceName + "-event-processor"
}
