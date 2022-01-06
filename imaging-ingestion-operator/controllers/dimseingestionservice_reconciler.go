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
	"k8s.io/apimachinery/pkg/types"
)

func (r *DimseIngestionServiceReconciler) GetDesiredState(currentState *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetBucketSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetBucketConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetNatsConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetDimseConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetDimseIngestionServiceDesiredState(currentState, cr))

	return desired
}

func (i *DimseIngestionServiceReconciler) GetBucketSecretDesiredState(state *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.ControllerAction {
	if state.BucketSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Bucket Secret"),
			Msg: "Missing Bucket Secret",
		}
	}

	return nil
}

func (i *DimseIngestionServiceReconciler) GetBucketConfigDesiredState(state *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.ControllerAction {
	if state.BucketConfig == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Bucket Config"),
			Msg: "Missing Bucket Config",
		}
	}

	return nil
}

func (i *DimseIngestionServiceReconciler) GetNatsConfigDesiredState(state *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.ControllerAction {
	config := model.DimseIngestionNatsConfig(cr)
	if state.NatsConfig == nil {
		return common.GenericCreateAction{
			Ref: config,
			Msg: "Create NATS Config",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.DimseIngestionNatsConfigReconciled(cr, state.NatsConfig),
		Msg: "Update NATS Config",
	}
}

func (i *DimseIngestionServiceReconciler) GetDimseConfigDesiredState(state *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.ControllerAction {
	config := model.DimseConfig(model.GetDimseIngestionConfigName(cr.Name), cr.Namespace)
	if state.DimseConfig == nil {
		return common.GenericCreateAction{
			Ref: config,
			Msg: "Create DIMSE Config",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.DimseConfigReconciled(state.DimseConfig),
		Msg: "Update DIMSE Config",
	}
}

func (i *DimseIngestionServiceReconciler) GetDimseIngestionServiceDesiredState(state *DimseIngestionServiceState, cr *v1alpha1.DimseIngestionService) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}
	brokerEndpoint := eventDrivenIngestionResource.Status.BrokerEndpoint

	service := model.DimseIngestionDeployment(cr, brokerEndpoint)
	if state.DimseIngestionDeployment == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create DIMSE Deployment",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.DimseIngestionDeploymentReconciled(cr, state.DimseIngestionDeployment, brokerEndpoint),
		Msg: "Update DIMSE Deployment",
	}
}
