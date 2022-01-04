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
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	apiErrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DimseProxyState struct {
	client.Client
	Scheme *runtime.Scheme

	NatsTokenSecret      *corev1.Secret
	NatsConfig           *corev1.ConfigMap
	DimseConfig          *corev1.ConfigMap
	DimseProxyDeployment *appsv1.Deployment
	DimseProxyService    *corev1.Service
}

func NewDimseProxyState(client client.Client, scheme *runtime.Scheme) *DimseProxyState {
	return &DimseProxyState{
		Client: client,
		Scheme: scheme,
	}
}

func (i *DimseProxyState) IsResourcesReady(resource client.Object) (bool, error) {
	dimseDeploymentReady, err := common.IsDeploymentReady(i.DimseProxyDeployment)
	if err != nil {
		return false, err
	}

	return dimseDeploymentReady, nil
}

func (i *DimseProxyState) Read(context context.Context, resource client.Object) error {
	cr, _ := resource.(*v1alpha1.DimseProxy)

	err := i.readNatsTokenSecretCurrentState(context, cr)
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

	err = i.readDimseProxyDeploymentCurrentState(context, cr)
	if err != nil {
		return err
	}

	err = i.readDimseProxyServiceCurrentState(context, cr)
	if err != nil {
		return err
	}

	return nil
}

func (i *DimseProxyState) readNatsTokenSecretCurrentState(context context.Context, cr *v1alpha1.DimseProxy) error {
	if cr.Spec.NatsTokenSecretName == "" {
		return nil
	}

	secret := model.NatsTokenSecret()
	secretSelector := model.NatsTokenSecretSelector(cr.Spec.NatsTokenSecretName, cr.Namespace)

	err := i.Client.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.NatsTokenSecret = nil
		} else {
			return err
		}
	} else {
		i.NatsTokenSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.NatsTokenSecret.Kind, i.NatsTokenSecret.Name)
	}

	return nil
}

func (i *DimseProxyState) readNatsConfigCurrentState(context context.Context, cr *v1alpha1.DimseProxy) error {
	config := model.DimseProxyNatsConfig(cr)
	configSelector := model.DimseProxyNatsConfigSelector(cr)

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

func (i *DimseProxyState) readDimseConfigCurrentState(context context.Context, cr *v1alpha1.DimseProxy) error {
	config := model.DimseConfig(model.GetDimseProxyConfigName(cr.Name), cr.Namespace)
	configSelector := model.DimseConfigSelector(model.GetDimseProxyConfigName(cr.Name), cr.Namespace)

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

func (i *DimseProxyState) readDimseProxyDeploymentCurrentState(context context.Context, cr *v1alpha1.DimseProxy) error {
	service := model.DimseProxyDeployment(cr)
	serviceSelector := model.DimseProxyDeploymentSelector(cr)

	err := i.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DimseProxyDeployment = nil
		} else {
			logger.Error(err, "readDimseProxyDeploymentCurrentState")
			return err
		}
	} else {
		i.DimseProxyDeployment = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DimseProxyDeployment.Kind, i.DimseProxyDeployment.Name)
	}

	return nil
}

func (i *DimseProxyState) readDimseProxyServiceCurrentState(context context.Context, cr *v1alpha1.DimseProxy) error {
	service := model.DimseProxyService(cr)
	serviceSelector := model.DimseProxyServiceSelector(cr)

	err := i.Client.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DimseProxyService = nil
		} else {
			logger.Error(err, "readDimseProxyServiceCurrentState")
			return err
		}
	} else {
		i.DimseProxyService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DimseProxyService.Kind, i.DimseProxyService.Name)
	}

	return nil
}
