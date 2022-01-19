/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	"github.com/coderanger/controller-utils/randstring"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

var _ = Describe("DicomEventBridge controller tests", func() {

	const (
		objectName      = "event-bridge"
		objectNamespace = "default"
		targetDimseHost = "dimse.host"
		targetDimsePort = 1234
		natsURL         = "nats.url"
	)

	var (
		eventDrivenIngestionName = randstring.MustRandomString(5)
		databaseSecretName       = randstring.MustRandomString(5)
		databaseConfigName       = randstring.MustRandomString(5)
	)

	Context("When creating DicomEventBridge resource", func() {
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
			edi := &v1alpha1.DicomEventDrivenIngestion{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomEventDrivenIngestion",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      eventDrivenIngestionName,
					Namespace: objectNamespace,
				},
				Spec: v1alpha1.DicomEventDrivenIngestionSpec{
					DatabaseSecretName: databaseSecretName,
					DatabaseConfigName: databaseConfigName,
				},
			}
			Expect(k8sClient.Create(ctx, edi)).Should(Succeed())

			edi.Status.BrokerEndpoint = "broker"
			Expect(k8sClient.Status().Update(ctx, edi)).Should(Succeed())

			By("Creating a new DicomEventBridge")
			obj := &v1alpha1.DicomEventBridge{
				TypeMeta: metav1.TypeMeta{
					APIVersion: "imaging-ingestion.alvearie.org/v1alpha1",
					Kind:       "DicomEventBridge",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      objectName,
					Namespace: objectNamespace,
				},
				Spec: v1alpha1.DicomEventBridgeSpec{
					Role:                          string(common.BridgeRoleHub),
					NatsURL:                       natsURL,
					DicomEventDrivenIngestionName: eventDrivenIngestionName,
					ImagePullSpec: v1alpha1.ImagePullSpec{
						ImagePullPolicy: corev1.PullAlways,
					},
				},
			}
			Expect(k8sClient.Create(ctx, obj)).Should(Succeed())

			bridgeLookupKey := types.NamespacedName{Name: objectName, Namespace: objectNamespace}
			bridge := &v1alpha1.DicomEventBridge{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, bridgeLookupKey, bridge)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Expect(bridge.Spec.NatsURL).Should(Equal(natsURL))

			natsConfigLookupKey := types.NamespacedName{Name: model.GetEventBridgeNatsConfigName(bridge), Namespace: objectNamespace}
			natsConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, natsConfigLookupKey, natsConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			edgeLocationConfigLookupKey := types.NamespacedName{Name: model.GetEventBridgeEdgeLocationConfigName(bridge), Namespace: objectNamespace}
			edgeLocationConfig := &corev1.ConfigMap{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, edgeLocationConfigLookupKey, edgeLocationConfig)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			deploymentLookupKey := types.NamespacedName{Name: model.GetEventBridgeDeploymentName(bridge), Namespace: objectNamespace}
			deployment := &appsv1.Deployment{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, deploymentLookupKey, deployment)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())
			Expect(deployment.Spec.Template.Spec.Containers[0].ImagePullPolicy).Should(Equal(corev1.PullAlways))

			svcLookupKey := types.NamespacedName{Name: model.GetEventBridgeServiceName(bridge), Namespace: objectNamespace}
			svc := &corev1.Service{}

			Eventually(func() bool {
				err := k8sClient.Get(ctx, svcLookupKey, svc)
				if err != nil {
					return false
				}
				return true
			}, timeout, interval).Should(BeTrue())

			Eventually(func() bool {
				err := k8sClient.Get(ctx, bridgeLookupKey, bridge)
				if err != nil {
					return false
				}
				return bridge.Status.Ready
			}, timeout, interval).Should(BeTrue())
		})
	})
})
