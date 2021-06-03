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
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
)

var _ = Describe("DicomEventDrivenIngestion controller tests", func() {

	var (
		objectName         = randstring.MustRandomString(5)
		databaseSecretName = randstring.MustRandomString(5)
		databaseConfigName = randstring.MustRandomString(5)
	)

	Context("When creating DicomEventDrivenIngestion resource", func() {
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
			obj := &imagingingestionv1alpha1.DicomEventDrivenIngestion{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomEventDrivenIngestion",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DicomEventDrivenIngestionSpec{
					DatabaseSecretName: databaseSecretName,
					DatabaseConfigName: databaseConfigName,
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			coreLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			core := &imagingingestionv1alpha1.DicomEventDrivenIngestion{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, coreLookupKey, core)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(core.Spec.DatabaseSecretName).Should(Equal(databaseSecretName))
			Expect(core.Spec.DatabaseConfigName).Should(Equal(databaseConfigName))

			eventProcessorLookupKey := types.NamespacedName{Name: model.GetEventProcessorServiceName(core.Name), Namespace: objectNamespace}
			eventProcessor := &kservingv1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, eventProcessorLookupKey, eventProcessor)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			eventBrokerLookupKey := types.NamespacedName{Name: model.GetEventBrokerName(core.Name), Namespace: objectNamespace}
			eventBroker := &keventingv1.Broker{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, eventBrokerLookupKey, eventBroker)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			triggerLookupKey := types.NamespacedName{Name: model.GetImageStoredTriggerName(core), Namespace: objectNamespace}
			trigger := &keventingv1.Trigger{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, triggerLookupKey, trigger)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Eventually(func() bool {
				err := k8sClient.Get(ctx, coreLookupKey, core)
				if err != nil {
					return false
				}
				return core.Status.Ready
			}, timeout, interval).Should(BeTrue())

			By("Deleting DicomEventDrivenIngestion")
			Eventually(func() error {
				return k8sClient.Delete(ctx, core)
			}, timeout, interval).Should(Succeed())

			Eventually(func() error {
				cr := &imagingingestionv1alpha1.DicomEventDrivenIngestion{}
				return k8sClient.Get(ctx, coreLookupKey, cr)
			}, timeout, interval).ShouldNot(Succeed())
		})
	})
})
