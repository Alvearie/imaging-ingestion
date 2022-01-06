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
	corev1 "k8s.io/api/core/v1"
	apiErrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DicomInstanceBindingState struct {
	client.Client
	Scheme *runtime.Scheme

	InstanceBindingSecret  *corev1.Secret
	InstanceBindingConfig  *corev1.ConfigMap
	InstanceBindingService *kservingv1.Service
	InstanceBindingTrigger *keventingv1.Trigger
}

func NewDicomInstanceBindingState(client client.Client, scheme *runtime.Scheme) *DicomInstanceBindingState {
	return &DicomInstanceBindingState{
		Client: client,
		Scheme: scheme,
	}
}

func (i *DicomInstanceBindingState) IsResourcesReady(resource client.Object) (bool, error) {
	dimseServiceReady, err := common.IsServiceReady(i.InstanceBindingService)
	if err != nil {
		return false, err
	}

	instanceBindingTriggerReady, err := common.IsTriggerReady(i.InstanceBindingTrigger)
	if err != nil {
		return false, err
	}

	return dimseServiceReady && instanceBindingTriggerReady, nil
}

func (i *DicomInstanceBindingState) Read(context context.Context, resource client.Object) error {
	cr, _ := resource.(*v1alpha1.DicomInstanceBinding)

	err := i.readInstanceBindingSecretCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readInstanceBindingConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readInstanceBindingServiceCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readInstanceBindingTriggerCurrentState(context, cr)
	if err != nil {
		return err
	}

	return nil
}

func (i *DicomInstanceBindingState) readInstanceBindingSecretCurrentState(context context.Context, cr *v1alpha1.DicomInstanceBinding) error {
	secret := model.InstanceBindingSecret(cr)
	secretSelector := model.InstanceBindingSecretSelector(cr)

	err := i.Client.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.InstanceBindingSecret = nil
		} else {
			return err
		}
	} else {
		i.InstanceBindingSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.InstanceBindingSecret.Kind, i.InstanceBindingSecret.Name)
	}

	return nil
}

func (i *DicomInstanceBindingState) readInstanceBindingConfigCurrentState(context context.Context, cr *v1alpha1.DicomInstanceBinding) error {
	config := model.InstanceBindingConfig(cr)
	configSelector := model.InstanceBindingConfigSelector(cr)

	err := i.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.InstanceBindingConfig = nil
		} else {
			return err
		}
	} else {
		i.InstanceBindingConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.InstanceBindingConfig.Kind, i.InstanceBindingConfig.Name)
	}

	return nil
}

func (i *DicomInstanceBindingState) readInstanceBindingServiceCurrentState(context context.Context, cr *v1alpha1.DicomInstanceBinding) error {
	service := model.InstanceBindingService(cr)
	serviceSelector := model.InstanceBindingServiceSelector(cr)

	err := i.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.InstanceBindingService = nil
		} else {
			logger.Error(err, "readInstanceBindingServiceCurrentState")
			return err
		}
	} else {
		i.InstanceBindingService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.InstanceBindingService.Kind, i.InstanceBindingService.Name)
	}

	return nil
}

func (i *DicomInstanceBindingState) readInstanceBindingTriggerCurrentState(context context.Context, cr *v1alpha1.DicomInstanceBinding) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}

	trigger := model.InstanceBindingTrigger(cr, model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	triggerSelector := model.InstanceBindingTriggerSelector(cr)

	err = i.Client.Get(context, triggerSelector, trigger)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.InstanceBindingTrigger = nil
		} else {
			logger.Error(err, "readStudyBindingCurrentState")
			return err
		}
	} else {
		i.InstanceBindingTrigger = trigger.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.InstanceBindingTrigger.Kind, i.InstanceBindingTrigger.Name)
	}

	return nil
}
