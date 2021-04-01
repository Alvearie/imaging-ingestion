/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package dicomeventdriveningestion

import (
	"context"
	"errors"

	corev1 "k8s.io/api/core/v1"
	apiErrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	keventingv1 "knative.dev/eventing/pkg/apis/eventing/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type DicomEventDrivenIngestionState struct {
	DatabaseSecret        *corev1.Secret
	DatabaseConfig        *corev1.ConfigMap
	EventProcessorService *kservingv1.Service
	EventBroker           *keventingv1.Broker
	ImageStoredTrigger    *keventingv1.Trigger
}

func NewDicomEventDrivenIngestionState() *DicomEventDrivenIngestionState {
	return &DicomEventDrivenIngestionState{}
}

func (i *DicomEventDrivenIngestionState) IsResourcesReady(cr *v1alpha1.DicomEventDrivenIngestion) (bool, error) {
	eventDrivenIngestionReady, err := common.IsServiceReady(i.EventProcessorService)
	if err != nil {
		return false, err
	}

	eventBrokerReady, err := common.IsBrokerReady(i.EventBroker)
	if err != nil {
		return false, err
	}

	imageStoredTriggerReady, err := common.IsTriggerReady(i.ImageStoredTrigger)
	if err != nil {
		return false, err
	}

	return eventDrivenIngestionReady && eventBrokerReady && imageStoredTriggerReady, nil
}

func (i *DicomEventDrivenIngestionState) Read(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	if !common.IsKnativeAvailable() {
		return errors.New("Knative serving or eventing not installed in cluster")
	}

	err := i.readDatabaseSecretCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readDatabaseConfigCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readEventProcessorServiceCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readEventBrokerCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	err = i.readImageStoredTriggerCurrentState(context, cr, controllerClient)
	if err != nil {
		return err
	}

	return nil
}

func (i *DicomEventDrivenIngestionState) readDatabaseSecretCurrentState(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	secret := model.DatabaseSecret(cr)
	secretSelector := model.DatabaseSecretSelector(cr)

	err := controllerClient.Get(context, secretSelector, secret)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DatabaseSecret = nil
		} else {
			return err
		}
	} else {
		i.DatabaseSecret = secret.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DatabaseSecret.Kind, i.DatabaseSecret.Name)
	}

	return nil
}

func (i *DicomEventDrivenIngestionState) readDatabaseConfigCurrentState(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	config := model.DatabaseConfig(cr)
	configSelector := model.DatabaseConfigSelector(cr)

	err := controllerClient.Get(context, configSelector, config)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.DatabaseConfig = nil
		} else {
			return err
		}
	} else {
		i.DatabaseConfig = config.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.DatabaseConfig.Kind, i.DatabaseConfig.Name)
	}

	return nil
}

func (i *DicomEventDrivenIngestionState) readEventProcessorServiceCurrentState(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	service := model.EventProcessorService(cr)
	serviceSelector := model.EventProcessorServiceSelector(cr)

	err := controllerClient.Get(context, serviceSelector, service)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.EventProcessorService = nil
		} else {
			logger.Error(err, "readEventProcessorServiceCurrentState")
			return err
		}
	} else {
		i.EventProcessorService = service.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.EventProcessorService.Kind, i.EventProcessorService.Name)
	}

	return nil
}

func (i *DicomEventDrivenIngestionState) readEventBrokerCurrentState(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	broker := model.EventBroker(cr)
	brokerSelector := model.EventBrokerSelector(cr)

	err := controllerClient.Get(context, brokerSelector, broker)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.EventBroker = nil
		} else {
			logger.Error(err, "readEventBrokerCurrentState")
			return err
		}
	} else {
		i.EventBroker = broker.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.EventBroker.Kind, i.EventBroker.Name)
		if i.EventBroker != nil && i.EventBroker.Status.Address.URL != nil {
			cr.Status.BrokerEndpoint = i.EventBroker.Status.Address.URL.String()
		}
	}

	return nil
}

func (i *DicomEventDrivenIngestionState) readImageStoredTriggerCurrentState(context context.Context, cr *v1alpha1.DicomEventDrivenIngestion, controllerClient client.Client) error {
	trigger := model.ImageStoredTrigger(cr)
	triggerSelector := model.ImageStoredTriggerSelector(cr)

	err := controllerClient.Get(context, triggerSelector, trigger)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			i.ImageStoredTrigger = nil
		} else {
			logger.Error(err, "readImageStoredCurrentState")
			return err
		}
	} else {
		i.ImageStoredTrigger = trigger.DeepCopy()
		cr.UpdateStatusSecondaryResources(i.ImageStoredTrigger.Kind, i.ImageStoredTrigger.Name)
	}

	return nil
}
