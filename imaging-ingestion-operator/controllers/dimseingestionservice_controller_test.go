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
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

var _ = Describe("DimseIngestionService controller tests", func() {

	var (
		objectName               = randstring.MustRandomString(5)
		eventDrivenIngestionName = randstring.MustRandomString(5)
		databaseSecretName       = randstring.MustRandomString(5)
		databaseConfigName       = randstring.MustRandomString(5)
		bucketSecretName         = randstring.MustRandomString(5)
		bucketConfigName         = randstring.MustRandomString(5)
	)

	const (
		applicationEntityTitle = "title"
		natsURL                = "nats.url"
	)

	Context("When creating DimseIngestionService resource", func() {
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

			By("Creating a new DimseIngestionService")
			obj := &imagingingestionv1alpha1.DimseIngestionService{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DimseIngestionService",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: imagingingestionv1alpha1.DimseIngestionServiceSpec{
					DicomEventDrivenIngestionName: eventDrivenIngestionName,
					BucketSecretName:              bucketSecretName,
					BucketConfigName:              bucketConfigName,
					ApplicationEntityTitle:        applicationEntityTitle,
					NatsURL:                       natsURL,
					ImagePullSpec: imagingingestionv1alpha1.ImagePullSpec{
						ImagePullPolicy: corev1.PullAlways,
					},
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			dimseIngestionLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			dimseIngestion := &imagingingestionv1alpha1.DimseIngestionService{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, dimseIngestionLookupKey, dimseIngestion)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(dimseIngestion.Spec.DicomEventDrivenIngestionName).Should(Equal(eventDrivenIngestionName))
			Expect(dimseIngestion.Spec.BucketSecretName).Should(Equal(bucketSecretName))
			Expect(dimseIngestion.Spec.BucketConfigName).Should(Equal(bucketConfigName))
			Expect(dimseIngestion.Spec.ApplicationEntityTitle).Should(Equal(applicationEntityTitle))
			Expect(dimseIngestion.Spec.NatsURL).Should(Equal(natsURL))

			coreLookupKey := types.NamespacedName{Name: eventDrivenIngestionName, Namespace: objectNamespace}
			core := &imagingingestionv1alpha1.DicomEventDrivenIngestion{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, coreLookupKey, core)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			natsConfigLookupKey := types.NamespacedName{Name: model.GetDimseIngestionNatsConfigName(dimseIngestion), Namespace: objectNamespace}
			natsConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, natsConfigLookupKey, natsConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			dimseConfigLookupKey := types.NamespacedName{Name: model.GetDimseIngestionConfigName(dimseIngestion.Name), Namespace: objectNamespace}
			dimseConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, dimseConfigLookupKey, dimseConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			deploymentLookupKey := types.NamespacedName{Name: model.GetDimseIngestionDeploymentName(dimseIngestion), Namespace: objectNamespace}
			deployment := &appsv1.Deployment{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, deploymentLookupKey, deployment)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())
			Expect(deployment.Spec.Template.Spec.Containers[0].ImagePullPolicy).Should(Equal(corev1.PullAlways))

			Eventually(func() bool {
				err := k8sClient.Get(ctx, dimseIngestionLookupKey, dimseIngestion)
				if err != nil {
					return false
				}
				return dimseIngestion.Status.Ready
			}, timeout, interval).Should(BeTrue())
		})
	})
})
