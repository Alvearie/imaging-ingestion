/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	imagingingestionv1alpha1 "github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

var _ = Describe("DimseProxy controller tests", func() {

	const (
		objectName             = "dimse-proxy"
		objectNamespace        = "default"
		bucketSecretName       = "bucket-secret"
		bucketConfigName       = "bucket-config"
		applicationEntityTitle = "title"
		targetDimseHost        = "dimse.host"
		targetDimsePort        = 1234
		natsURL                = "nats.url"
	)

	Context("When creating DimseProxy resource", func() {
		It("Should create resource successfully", func() {
			ctx := context.Background()

			By("Creating a new DimseProxy")
			obj := &imagingingestionv1alpha1.DimseProxy{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DimseProxy",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DimseProxySpec{
					ImagePullSpec: imagingingestionv1alpha1.ImagePullSpec{
						ImagePullPolicy: corev1.PullAlways,
					},
					ApplicationEntityTitle: applicationEntityTitle,
					TargetDimseHost:        targetDimseHost,
					TargetDimsePort:        targetDimsePort,
					NatsURL:                natsURL,
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			proxyLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			proxy := &imagingingestionv1alpha1.DimseProxy{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, proxyLookupKey, proxy)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(proxy.Spec.ImagePullSpec.ImagePullPolicy).Should(Equal(corev1.PullAlways))
			Expect(proxy.Spec.ApplicationEntityTitle).Should(Equal(applicationEntityTitle))
			Expect(proxy.Spec.TargetDimseHost).Should(Equal(targetDimseHost))
			Expect(proxy.Spec.TargetDimsePort).Should(Equal(targetDimsePort))
			Expect(proxy.Spec.NatsURL).Should(Equal(natsURL))

			natsConfigLookupKey := types.NamespacedName{Name: model.GetDimseProxyNatsConfigName(proxy), Namespace: objectNamespace}
			natsConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, natsConfigLookupKey, natsConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			dimseConfigLookupKey := types.NamespacedName{Name: model.GetDimseProxyConfigName(proxy.Name), Namespace: objectNamespace}
			dimseConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, dimseConfigLookupKey, dimseConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			deploymentLookupKey := types.NamespacedName{Name: model.GetDimseProxyDeploymentName(proxy), Namespace: objectNamespace}
			deployment := &appsv1.Deployment{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, deploymentLookupKey, deployment)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())
			Expect(deployment.Spec.Template.Spec.Containers[0].ImagePullPolicy).Should(Equal(corev1.PullAlways))

			svcLookupKey := types.NamespacedName{Name: model.GetDimseProxyServiceName(proxy), Namespace: objectNamespace}
			svc := &corev1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, svcLookupKey, svc)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Eventually(func() bool {
				err := k8sClient.Get(ctx, proxyLookupKey, proxy)
				if err != nil {
					return false
				}
				return proxy.Status.Ready
			}, timeout, interval).Should(BeTrue())
		})
	})
})
