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
	ksourcesv1 "knative.dev/eventing/pkg/apis/sources/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DicomStudyBindingState struct {
	client.Client
	Scheme *runtime.Scheme

	StudyBindingSecret      *corev1.Secret
	StudyBindingConfig      *corev1.ConfigMap
	StudyBindingService     *kservingv1.Service
	StudyBindingSinkBinding *ksourcesv1.SinkBinding
	StudyBindingTrigger     *keventingv1.Trigger
}

func NewDicomStudyBindingState(client client.Client, scheme *runtime.Scheme) *DicomStudyBindingState {
	return &DicomStudyBindingState{
		Client: client,
		Scheme: scheme,
	}
}

func (i *DicomStudyBindingState) IsResourcesReady(resource client.Object) (bool, error) {
	dimseServiceReady, err := common.IsServiceReady(i.StudyBindingService)
	if err != nil {
		return false, err
	}

	studyBindingSinkBindingReady, err := common.IsSinkBindingReady(i.StudyBindingSinkBinding)
	if err != nil {
		return false, err
	}

	studyBindingTriggerReady, err := common.IsTriggerReady(i.StudyBindingTrigger)
	if err != nil {
		return false, err
	}

	return dimseServiceReady && studyBindingSinkBindingReady && studyBindingTriggerReady, nil
}

func (i *DicomStudyBindingState) Read(context context.Context, resource client.Object) error {
	cr, _ := resource.(*v1alpha1.DicomStudyBinding)

	err := i.readStudyBindingSecretCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readStudyBindingConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readStudyBindingServiceCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readStudyBindingSinkBindingCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readStudyBindingTriggerCurrentState(context, cr)
	if err != nil {
		return err
	}

	return nil
}

func (i *DicomStudyBindingState) readStudyBindingSecretCurrentState(context context.Context, cr *v1alpha1.DicomStudyBinding) error {
	secret := model.StudyBindingSecret(cr)
	secretSelector := model.StudyBindingSecretSelector(cr)

	err := i.Client.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StudyBindingSecret = nil
		} else {
			return err
		}
	} else {
		i.StudyBindingSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StudyBindingSecret.Kind, i.StudyBindingSecret.Name)
	}

	return nil
}

func (i *DicomStudyBindingState) readStudyBindingConfigCurrentState(context context.Context, cr *v1alpha1.DicomStudyBinding) error {
	config := model.StudyBindingConfig(cr)
	configSelector := model.StudyBindingConfigSelector(cr)

	err := i.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StudyBindingConfig = nil
		} else {
			return err
		}
	} else {
		i.StudyBindingConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StudyBindingConfig.Kind, i.StudyBindingConfig.Name)
	}

	return nil
}

func (i *DicomStudyBindingState) readStudyBindingServiceCurrentState(context context.Context, cr *v1alpha1.DicomStudyBinding) error {
	service := model.StudyBindingService(cr)
	serviceSelector := model.StudyBindingServiceSelector(cr)

	err := i.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StudyBindingService = nil
		} else {
			logger.Error(err, "readStudyBindingServiceCurrentState")
			return err
		}
	} else {
		i.StudyBindingService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StudyBindingService.Kind, i.StudyBindingService.Name)
	}

	return nil
}

func (i *DicomStudyBindingState) readStudyBindingSinkBindingCurrentState(context context.Context, cr *v1alpha1.DicomStudyBinding) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}

	binding := model.StudyBindingSinkBinding(cr, model.GetEventProcessorServiceName(eventDrivenIngestionResource.Name), model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	bindingSelector := model.StudyBindingSinkBindingSelector(cr)

	err = i.Client.Get(context, bindingSelector, binding)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StudyBindingSinkBinding = nil
		} else {
			logger.Error(err, "readStudyBindingSinkBindingCurrentState")
			return err
		}
	} else {
		i.StudyBindingSinkBinding = binding.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StudyBindingSinkBinding.Kind, i.StudyBindingSinkBinding.Name)
	}

	return nil
}

func (i *DicomStudyBindingState) readStudyBindingTriggerCurrentState(context context.Context, cr *v1alpha1.DicomStudyBinding) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}

	trigger := model.StudyBindingTrigger(cr, model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	triggerSelector := model.StudyBindingTriggerSelector(cr)

	err = i.Client.Get(context, triggerSelector, trigger)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StudyBindingTrigger = nil
		} else {
			logger.Error(err, "readStudyBindingCurrentState")
			return err
		}
	} else {
		i.StudyBindingTrigger = trigger.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StudyBindingTrigger.Kind, i.StudyBindingTrigger.Name)
	}

	return nil
}
