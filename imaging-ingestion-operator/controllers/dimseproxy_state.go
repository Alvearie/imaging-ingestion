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
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DimseProxyState struct {
	NatsConfig           *corev1.ConfigMap
	DimseConfig          *corev1.ConfigMap
	DimseProxyDeployment *appsv1.Deployment
}

func NewDimseProxyState() *DimseProxyState {
	return &DimseProxyState{}
}

func (i *DimseProxyState) IsResourcesReady(cr *v1alpha1.DimseProxy) (bool, error) {
	dimseDeploymentReady, err := common.IsDeploymentReady(i.DimseProxyDeployment)
	if err != nil {
		return false, err
	}

	return dimseDeploymentReady, nil
}

func (i *DimseProxyState) Read(context context.Context, cr *v1alpha1.DimseProxy, controllerClient client.Client) error {
	err := i.readNatsConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readDimseConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readDimseProxyDeploymentCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	return nil
}

func (i *DimseProxyState) readNatsConfigCurrentState(context context.Context, cr *v1alpha1.DimseProxy, controllerClient client.Client) error {
	config := model.DimseProxyNatsConfig(cr)
	configSelector := model.DimseProxyNatsConfigSelector(cr)

	err := controllerClient.Get(context, configSelector, config)
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

func (i *DimseProxyState) readDimseConfigCurrentState(context context.Context, cr *v1alpha1.DimseProxy, controllerClient client.Client) error {
	config := model.DimseConfig(model.GetDimseProxyConfigName(cr.Name), cr.Namespace)
	configSelector := model.DimseConfigSelector(model.GetDimseProxyConfigName(cr.Name), cr.Namespace)

	err := controllerClient.Get(context, configSelector, config)
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

func (i *DimseProxyState) readDimseProxyDeploymentCurrentState(context context.Context, cr *v1alpha1.DimseProxy, controllerClient client.Client) error {
	service := model.DimseProxyDeployment(cr)
	serviceSelector := model.DimseProxyDeploymentSelector(cr)

	err := controllerClient.Get(context, serviceSelector, service)
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
