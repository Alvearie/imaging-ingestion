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
	"k8s.io/apimachinery/pkg/types"
	ksourcesv1alpha2 "knative.dev/eventing/pkg/apis/sources/v1alpha2"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DicomwebIngestionServiceState struct {
	BucketSecret    *corev1.Secret
	BucketConfig    *corev1.ConfigMap
	StowService     *kservingv1.Service
	WadoService     *kservingv1.Service
	StowSinkBinding *ksourcesv1alpha2.SinkBinding
}

func NewDicomwebIngestionServiceState() *DicomwebIngestionServiceState {
	return &DicomwebIngestionServiceState{}
}

func (i *DicomwebIngestionServiceState) IsResourcesReady(cr *v1alpha1.DicomwebIngestionService) (bool, error) {
	stowServiceReady, err := common.IsServiceReady(i.StowService)
	if err != nil {
		return false, err
	}

	wadoServiceReady, err := common.IsServiceReady(i.WadoService)
	if err != nil {
		return false, err
	}

	stowSinkBindingReady, err := common.IsSinkBindingReady(i.StowSinkBinding)
	if err != nil {
		return false, err
	}

	return stowServiceReady && wadoServiceReady && stowSinkBindingReady, nil
}

func (i *DicomwebIngestionServiceState) Read(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	err := i.readBucketSecretCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readBucketConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readStowServiceCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readWadoServiceCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readStowSinkBindingCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	return nil
}

func (i *DicomwebIngestionServiceState) readBucketSecretCurrentState(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	secret := model.BucketSecret()
	secretSelector := model.BucketSecretSelector(cr.Spec.BucketSecretName, cr.Namespace)

	err := controllerClient.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.BucketSecret = nil
		} else {
			return err
		}
	} else {
		i.BucketSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.BucketSecret.Kind, i.BucketSecret.Name)
	}

	return nil
}

func (i *DicomwebIngestionServiceState) readBucketConfigCurrentState(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	config := model.BucketConfig()
	configSelector := model.BucketConfigSelector(cr.Spec.BucketConfigName, cr.Namespace)

	err := controllerClient.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.BucketConfig = nil
		} else {
			return err
		}
	} else {
		i.BucketConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.BucketConfig.Kind, i.BucketConfig.Name)
	}

	return nil
}

func (i *DicomwebIngestionServiceState) readStowServiceCurrentState(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	service := model.StowService(cr)
	serviceSelector := model.StowServiceSelector(cr)

	err := controllerClient.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StowService = nil
		} else {
			logger.Error(err, "readStowServiceCurrentState")
			return err
		}
	} else {
		i.StowService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StowService.Kind, i.StowService.Name)
	}

	return nil
}

func (i *DicomwebIngestionServiceState) readWadoServiceCurrentState(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, controllerClient)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}

	service := model.WadoService(cr, GetEventProcessorServiceEndpoint(eventDrivenIngestionResource))
	serviceSelector := model.WadoServiceSelector(cr)

	err = controllerClient.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.WadoService = nil
		} else {
			logger.Error(err, "readWadoServiceCurrentState")
			return err
		}
	} else {
		i.WadoService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.WadoService.Kind, i.WadoService.Name)
		if i.WadoService != nil && i.WadoService.Status.Address != nil && i.WadoService.Status.Address.URL != nil {
			cr.Status.WadoServiceInternalEndpoint = i.WadoService.Status.Address.URL.String()
			cr.Status.WadoServiceExternalEndpoint = i.WadoService.Status.URL.String()
		}
	}

	return nil
}

func (i *DicomwebIngestionServiceState) readStowSinkBindingCurrentState(context context.Context, cr *v1alpha1.DicomwebIngestionService, controllerClient client.Client) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, controllerClient)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}

	binding := model.StowSinkBinding(cr, model.GetStowServiceName(cr), model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	bindingSelector := model.StowSinkBindingSelector(cr)

	err = controllerClient.Get(context, bindingSelector, binding)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.StowSinkBinding = nil
		} else {
			logger.Error(err, "readStowRSSinkBindingCurrentState")
			return err
		}
	} else {
		i.StowSinkBinding = binding.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.StowSinkBinding.Kind, i.StowSinkBinding.Name)
	}

	return nil
}

func GetEventProcessorServiceEndpoint(cr *v1alpha1.DicomEventDrivenIngestion) string {
	// TODO: Fix this to get advertised Endpoint from Status
	if se, ok := cr.Status.SecondaryResources["ServiceEndpoint"]; ok {
		if len(se) > 0 {
			return se[0]
		}
	}

	return ""
}
