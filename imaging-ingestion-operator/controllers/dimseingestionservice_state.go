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

type DimseIngestionServiceState struct {
	client.Client
	Scheme *runtime.Scheme

	BucketSecret             *corev1.Secret
	BucketConfig             *corev1.ConfigMap
	NatsConfig               *corev1.ConfigMap
	DimseConfig              *corev1.ConfigMap
	DimseIngestionDeployment *appsv1.Deployment
}

func NewDimseIngestionServiceState(client client.Client, scheme *runtime.Scheme) *DimseIngestionServiceState {
	return &DimseIngestionServiceState{
		Client: client,
		Scheme: scheme,
	}
}

func (i *DimseIngestionServiceState) IsResourcesReady(resource client.Object) (bool, error) {
	dimseDeploymentReady, err := common.IsDeploymentReady(i.DimseIngestionDeployment)
	if err != nil {
		return false, err
	}

	return dimseDeploymentReady, nil
}

func (i *DimseIngestionServiceState) Read(context context.Context, resource client.Object) error {
	cr, _ := resource.(*v1alpha1.DimseIngestionService)

	err := i.readBucketSecretCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readBucketConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readNatsConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readDimseConfigCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readDimseServiceCurrentState(context, cr)
	if err != nil {
		return err
	}

	return nil
}

func (i *DimseIngestionServiceState) readBucketSecretCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService) error {
	secret := model.BucketSecret()
	secretSelector := model.BucketSecretSelector(cr.Spec.BucketSecretName, cr.Namespace)

	err := i.Client.Get(context, secretSelector, secret)
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

func (i *DimseIngestionServiceState) readBucketConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService) error {
	config := model.BucketConfig()
	configSelector := model.BucketConfigSelector(cr.Spec.BucketConfigName, cr.Namespace)

	err := i.Client.Get(context, configSelector, config)
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

func (i *DimseIngestionServiceState) readNatsConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService) error {
	config := model.DimseIngestionNatsConfig(cr)
	configSelector := model.DimseIngestionNatsConfigSelector(cr)

	err := i.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.NatsConfig = nil
		} else {
			return err
		}
	} else {
		i.NatsConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.NatsConfig.Kind, i.NatsConfig.Name)
	}

	return nil
}

func (i *DimseIngestionServiceState) readDimseConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService) error {
	config := model.DimseConfig(model.GetDimseIngestionConfigName(cr.Name), cr.Namespace)
	configSelector := model.DimseConfigSelector(model.GetDimseIngestionConfigName(cr.Name), cr.Namespace)

	err := i.Client.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DimseConfig = nil
		} else {
			return err
		}
	} else {
		i.DimseConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DimseConfig.Kind, i.DimseConfig.Name)
	}

	return nil
}

func (i *DimseIngestionServiceState) readDimseServiceCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}
	brokerEndpoint := eventDrivenIngestionResource.Status.BrokerEndpoint

	service := model.DimseIngestionDeployment(cr, brokerEndpoint)
	serviceSelector := model.DimseIngestionDeploymentSelector(cr)

	err = i.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DimseIngestionDeployment = nil
		} else {
			logger.Error(err, "readDimseServiceCurrentState")
			return err
		}
	} else {
		i.DimseIngestionDeployment = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DimseIngestionDeployment.Kind, i.DimseIngestionDeployment.Name)
	}

	return nil
}
