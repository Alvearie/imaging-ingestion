/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	imagingingestionv1alpha1 "github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	"github.com/coderanger/controller-utils/randstring"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	ksourcesv1 "knative.dev/eventing/pkg/apis/sources/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
)

var _ = Describe("DicomStudyBinding controller tests", func() {

	var (
		objectName               = randstring.MustRandomString(5)
		eventDrivenIngestionName = randstring.MustRandomString(5)
		bindingSecretName        = randstring.MustRandomString(5)
		bindingConfigName        = randstring.MustRandomString(5)
		databaseSecretName       = randstring.MustRandomString(5)
		databaseConfigName       = randstring.MustRandomString(5)
	)

	Context("When creating DicomStudyBinding resource", func() {
		It("Should create resource successfully", func() {
			ctx := context.Background()

			By("Creating a new Database Secret")
			dbSecret := &corev1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      databaseSecretName,
					Namespace: objectNamespace,
				},
				Data: map[string][]byte{},
			}
			Expect(k8sClient.Create(ctx, dbSecret)).Should(Succeed())

			By("Creating a new Database Config")
			dbConfig := &corev1.ConfigMap{
				ObjectMeta: metav1.ObjectMeta{
					Name:      databaseConfigName,
					Namespace: objectNamespace,
				},
				Data: map[string]string{},
			}
			Expect(k8sClient.Create(ctx, dbConfig)).Should(Succeed())

			By("Creating a new DicomEventDrivenIngestion")
			edi := &imagingingestionv1alpha1.DicomEventDrivenIngestion{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomEventDrivenIngestion",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      eventDrivenIngestionName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DicomEventDrivenIngestionSpec{
					DatabaseSecretName: databaseSecretName,
					DatabaseConfigName: databaseConfigName,
				},
			}
			Expect(k8sClient.Create(ctx, edi)).Should(Succeed())

			By("Creating a new Binding Secret")
			bucketSecret := &corev1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      bindingSecretName,
					Namespace: objectNamespace,
				},
				Data: map[string][]byte{},
			}
			Expect(k8sClient.Create(ctx, bucketSecret)).Should(Succeed())

			By("Creating a new Binding Config")
			bucketConfig := &corev1.ConfigMap{
				ObjectMeta: metav1.ObjectMeta{
					Name:      bindingConfigName,
					Namespace: objectNamespace,
				},
				Data: map[string]string{},
			}
			Expect(k8sClient.Create(ctx, bucketConfig)).Should(Succeed())

			By("Creating a new DicomStudyBinding")
			obj := &imagingingestionv1alpha1.DicomStudyBinding{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomStudyBinding",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DicomStudyBindingSpec{
					DicomEventDrivenIngestionName: eventDrivenIngestionName,
					BindingSecretName:             bindingSecretName,
					BindingConfigName:             bindingConfigName,
					ImagePullSpec: imagingingestionv1alpha1.ImagePullSpec{
						ImagePullPolicy: corev1.PullAlways,
					},
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			bindingLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			binding := &imagingingestionv1alpha1.DicomStudyBinding{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, bindingLookupKey, binding)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(binding.Spec.DicomEventDrivenIngestionName).Should(Equal(eventDrivenIngestionName))
			Expect(binding.Spec.BindingSecretName).Should(Equal(bindingSecretName))
			Expect(binding.Spec.BindingConfigName).Should(Equal(bindingConfigName))

			coreLookupKey := types.NamespacedName{Name: eventDrivenIngestionName, Namespace: objectNamespace}
			core := &imagingingestionv1alpha1.DicomEventDrivenIngestion{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, coreLookupKey, core)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			serviceLookupKey := types.NamespacedName{Name: model.GetStudyBindingServiceName(binding), Namespace: objectNamespace}
			service := &kservingv1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, serviceLookupKey, service)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())
			Expect(service.Spec.Template.Spec.Containers[0].ImagePullPolicy).Should(Equal(corev1.PullAlways))

			sbLookupKey := types.NamespacedName{Name: model.GetStudyBindingSinkBindingName(binding), Namespace: objectNamespace}
			sb := &ksourcesv1.SinkBinding{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, sbLookupKey, sb)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			triggerLookupKey := types.NamespacedName{Name: model.GetStudyBindingTriggerName(binding), Namespace: objectNamespace}
			trigger := &keventingv1.Trigger{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, triggerLookupKey, trigger)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Eventually(func() bool {
				err := k8sClient.Get(ctx, bindingLookupKey, binding)
				if err != nil {
					return false
				}
				return binding.Status.Ready
			}, timeout, interval).Should(BeTrue())

			By("Deleting DicomStudyBinding")
			Eventually(func() error {
				return k8sClient.Delete(ctx, binding)
			}, timeout, interval).Should(Succeed())

			Eventually(func() error {
				cr := &imagingingestionv1alpha1.DicomStudyBinding{}
				return k8sClient.Get(ctx, bindingLookupKey, cr)
			}, timeout, interval).ShouldNot(Succeed())

			By("Deleting DicomEventDrivenIngestion")
			Eventually(func() error {
				return k8sClient.Delete(ctx, core)
			}, timeout, interval).Should(Succeed())
		})
	})
})
