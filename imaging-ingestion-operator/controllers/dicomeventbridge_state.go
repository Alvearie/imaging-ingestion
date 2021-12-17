/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"
	"errors"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	apiErrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DicomEventBridgeState struct {
	client.Client
	Scheme *runtime.Scheme

	EventDrivenIngestion  *v1alpha1.DicomEventDrivenIngestion
	NatsTokenSecret       *corev1.Secret
	NatsConfig            *corev1.ConfigMap
	EdgeLocationConfig    *corev1.ConfigMap
	EventBridgeDeployment *appsv1.Deployment
	EventBridgeService    *corev1.Service
}

func NewDicomEventBridgeState(client client.Client, scheme *runtime.Scheme) *DicomEventBridgeState {
	return &DicomEventBridgeState{
		Client: client,
		Scheme: scheme,
	}
}

func (i *DicomEventBridgeState) IsResourcesReady(resource client.Object) (bool, error) {
	bridgeDeploymentReady, err := common.IsDeploymentReady(i.EventBridgeDeployment)
	if err != nil {
		return false, err
	}

	return bridgeDeploymentReady, nil
}

func (i *DicomEventBridgeState) Read(context context.Context, resource client.Object) error {
	cr, _ := resource.(*v1alpha1.DicomEventBridge)

	err := i.readEventDrivenIngestionCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readNatsTokenSecretCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readNatsConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readEdgeLocationConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readEventBridgeDeploymentCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readEventBridgeServiceCurrentState(context, cr)
	if err != nil {
		return err
	}

	return nil
}

func (s *DicomEventBridgeState) readEventDrivenIngestionCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	resource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, s.Client)
	if resource == nil || err != nil {
		s.EventDrivenIngestion = nil
		return errors.New("Error getting DicomEventDrivenIngestion")
	} else {
		s.EventDrivenIngestion = resource.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.EventDrivenIngestion.Kind, s.EventDrivenIngestion.Name)
	}

	if resource.Status.BrokerEndpoint == "" {
		return nil //&common.ResourceNotReadyError{PartialObject: resource}
	}

	return nil
}

func (s *DicomEventBridgeState) readNatsTokenSecretCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	if cr.Spec.NatsTokenSecretName == "" {
		return nil
	}

	secret := model.NatsTokenSecret()
	secretSelector := model.NatsTokenSecretSelector(cr.Spec.NatsTokenSecretName, cr.Namespace)

	err := s.Client.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			s.NatsTokenSecret = nil
		} else {
			return err
		}
	} else {
		s.NatsTokenSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.NatsTokenSecret.Kind, s.NatsTokenSecret.Name)
	}

	return nil
}

func (s *DicomEventBridgeState) readNatsConfigCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	config := model.EventBridgeNatsConfig(cr)
	configSelector := model.EventBridgeNatsConfigSelector(cr)

	err := s.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			s.NatsConfig = nil
		} else {
			return err
		}
	} else {
		s.NatsConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.NatsConfig.Kind, s.NatsConfig.Name)
	}

	return nil
}

func (s *DicomEventBridgeState) readEdgeLocationConfigCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	config := model.EventBridgeEdgeLocationConfig(cr)
	configSelector := model.EventBridgeEdgeLocationConfigSelector(cr)

	err := s.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			s.EdgeLocationConfig = nil
		} else {
			return err
		}
	} else {
		s.EdgeLocationConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.EdgeLocationConfig.Kind, s.EdgeLocationConfig.Name)
	}

	return nil
}

func (s *DicomEventBridgeState) readEventBridgeDeploymentCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	deploy := model.EventBridgeDeployment(cr, s.EventDrivenIngestion)
	deploySelector := model.EventBridgeDeploymentSelector(cr)

	err := s.Client.Get(context, deploySelector, deploy)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			s.EventBridgeDeployment = nil
		} else {
			logger.Error(err, "readEventBridgeDeploymentCurrentState")
			return err
		}
	} else {
		s.EventBridgeDeployment = deploy.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.EventBridgeDeployment.Kind, s.EventBridgeDeployment.Name)
	}

	return nil
}

func (s *DicomEventBridgeState) readEventBridgeServiceCurrentState(context context.Context, cr *v1alpha1.DicomEventBridge) error {
	service := model.EventBridgeService(cr)
	serviceSelector := model.EventBridgeServiceSelector(cr)

	err := s.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			s.EventBridgeService = nil
		} else {
			logger.Error(err, "readEventBridgeServiceCurrentState")
			return err
		}
	} else {
		s.EventBridgeService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(s.EventBridgeService.Kind, s.EventBridgeService.Name)
	}

	return nil
}
