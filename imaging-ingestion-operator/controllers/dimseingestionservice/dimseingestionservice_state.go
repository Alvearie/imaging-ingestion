/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package dimseingestionservice

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
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DimseIngestionServiceState struct {
	BucketSecret             *corev1.Secret
	BucketConfig             *corev1.ConfigMap
	NatsConfig               *corev1.ConfigMap
	DimseConfig              *corev1.ConfigMap
	DimseIngestionDeployment *appsv1.Deployment
}

func NewDimseIngestionServiceState() *DimseIngestionServiceState {
	return &DimseIngestionServiceState{}
}

func (i *DimseIngestionServiceState) IsResourcesReady(cr *v1alpha1.DimseIngestionService) (bool, error) {
	dimseDeploymentReady, err := common.IsDeploymentReady(i.DimseIngestionDeployment)
	if err != nil {
		return false, err
	}

	return dimseDeploymentReady, nil
}

func (i *DimseIngestionServiceState) Read(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
	err := i.readBucketSecretCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readBucketConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readNatsConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readDimseConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readDimseServiceCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	return nil
}

func (i *DimseIngestionServiceState) readBucketSecretCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
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

func (i *DimseIngestionServiceState) readBucketConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
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

func (i *DimseIngestionServiceState) readNatsConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
	config := model.DimseIngestionNatsConfig(cr)
	configSelector := model.DimseIngestionNatsConfigSelector(cr)

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

func (i *DimseIngestionServiceState) readDimseConfigCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
	config := model.DimseConfig(model.GetDimseIngestionConfigName(cr.Name), cr.Namespace)
	configSelector := model.DimseConfigSelector(model.GetDimseIngestionConfigName(cr.Name), cr.Namespace)

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

func GetEventDrivenIngestionResource(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) (*v1alpha1.DicomEventDrivenIngestion, error) {
	resource := model.EventDrivenIngestionResource()
	resourceSelector := model.EventDrivenIngestionResourceSelector(cr.Spec.DicomEventDrivenIngestionName, cr.Namespace)

	err := controllerClient.Get(context, resourceSelector, resource)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			return nil, nil
		} else {
			return nil, err
		}
	} else {
		return resource.DeepCopy(), nil
	}
}

func (i *DimseIngestionServiceState) readDimseServiceCurrentState(context context.Context, cr *v1alpha1.DimseIngestionService, controllerClient client.Client) error {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context, cr, controllerClient)
	if eventDrivenIngestionResource == nil || err != nil {
		return errors.New("Error getting DicomEventDrivenIngestion")
	}
	brokerEndpoint := eventDrivenIngestionResource.Status.BrokerEndpoint

	service := model.DimseIngestionDeployment(cr, brokerEndpoint)
	serviceSelector := model.DimseIngestionDeploymentSelector(cr)

	err = controllerClient.Get(context, serviceSelector, service)
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
