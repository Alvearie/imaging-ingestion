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
	ksourcesv1alpha2 "knative.dev/eventing/pkg/apis/sources/v1alpha2"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
)

var _ = Describe("DicomwebIngestionService controller tests", func() {

	var (
		objectName               = randstring.MustRandomString(5)
		eventDrivenIngestionName = randstring.MustRandomString(5)
		databaseSecretName       = randstring.MustRandomString(5)
		databaseConfigName       = randstring.MustRandomString(5)
		providerName             = randstring.MustRandomString(5)
		bucketSecretName         = randstring.MustRandomString(5)
		bucketConfigName         = randstring.MustRandomString(5)
	)

	Context("When creating DicomwebIngestionService resource", func() {
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

			By("Creating a new Bucket Secret")
			bucketSecret := &corev1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      bucketSecretName,
					Namespace: objectNamespace,
				},
				Data: map[string][]byte{},
			}
			Expect(k8sClient.Create(ctx, bucketSecret)).Should(Succeed())

			By("Creating a new Bucket Config")
			bucketConfig := &corev1.ConfigMap{
				ObjectMeta: metav1.ObjectMeta{
					Name:      bucketConfigName,
					Namespace: objectNamespace,
				},
				Data: map[string]string{},
			}
			Expect(k8sClient.Create(ctx, bucketConfig)).Should(Succeed())

			By("Creating a new DicomwebIngestionService")
			obj := &imagingingestionv1alpha1.DicomwebIngestionService{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomwebIngestionService",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DicomwebIngestionServiceSpec{
					DicomEventDrivenIngestionName: eventDrivenIngestionName,
					BucketSecretName:              bucketSecretName,
					BucketConfigName:              bucketConfigName,
					ProviderName:                  providerName,
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			webIngestionLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			webIngestion := &imagingingestionv1alpha1.DicomwebIngestionService{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, webIngestionLookupKey, webIngestion)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(webIngestion.Spec.DicomEventDrivenIngestionName).Should(Equal(eventDrivenIngestionName))
			Expect(webIngestion.Spec.ProviderName).Should(Equal(providerName))
			Expect(webIngestion.Spec.BucketSecretName).Should(Equal(bucketSecretName))
			Expect(webIngestion.Spec.BucketConfigName).Should(Equal(bucketConfigName))

			coreLookupKey := types.NamespacedName{Name: eventDrivenIngestionName, Namespace: objectNamespace}
			core := &imagingingestionv1alpha1.DicomEventDrivenIngestion{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, coreLookupKey, core)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			stowLookupKey := types.NamespacedName{Name: model.GetStowServiceName(webIngestion), Namespace: objectNamespace}
			stow := &kservingv1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, stowLookupKey, stow)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			wadoLookupKey := types.NamespacedName{Name: model.GetWadoServiceName(webIngestion), Namespace: objectNamespace}
			wado := &kservingv1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, wadoLookupKey, wado)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			sbLookupKey := types.NamespacedName{Name: model.GetStowSinkBindingName(webIngestion), Namespace: objectNamespace}
			sb := &ksourcesv1alpha2.SinkBinding{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, sbLookupKey, sb)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Eventually(func() bool {
				err := k8sClient.Get(ctx, webIngestionLookupKey, webIngestion)
				if err != nil {
					return false
				}
				return webIngestion.Status.Ready
			}, timeout, interval).Should(BeTrue())

			By("Deleting DicomwebIngestionService")
			Eventually(func() error {
				return k8sClient.Delete(ctx, webIngestion)
			}, timeout, interval).Should(Succeed())

			Eventually(func() error {
				cr := &imagingingestionv1alpha1.DicomStudyBinding{}
				return k8sClient.Get(ctx, webIngestionLookupKey, cr)
			}, timeout, interval).ShouldNot(Succeed())

			By("Deleting DicomEventDrivenIngestion")
			Eventually(func() error {
				return k8sClient.Delete(ctx, core)
			}, timeout, interval).Should(Succeed())
		})
	})
})
