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

func (r *DicomwebIngestionServiceReconciler) GetDesiredState(currentState *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetBucketSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetBucketConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStowServiceDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetWadoServiceDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStowSinkBindingDesiredState(currentState, cr))

	return desired
}

func (i *DicomwebIngestionServiceReconciler) GetBucketSecretDesiredState(state *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.ControllerAction {
	if state.BucketSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Bucket Secret"),
			Msg: "Missing Bucket Secret",
		}
	}

	return nil
}

func (i *DicomwebIngestionServiceReconciler) GetBucketConfigDesiredState(state *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.ControllerAction {
	if state.BucketConfig == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Bucket Config"),
			Msg: "Missing Bucket Config",
		}
	}

	return nil
}

func (i *DicomwebIngestionServiceReconciler) GetStowServiceDesiredState(state *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.ControllerAction {
	service := model.StowService(cr)
	if state.StowService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create STOW Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.StowServiceReconciled(cr, state.StowService),
		Msg: "Update STOW Service",
	}
}

func (i *DicomwebIngestionServiceReconciler) GetWadoServiceDesiredState(state *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}

	eventProcessorServiceEndpoint := GetEventProcessorServiceEndpoint(eventDrivenIngestionResource)
	service := model.WadoService(cr, eventProcessorServiceEndpoint)
	if state.WadoService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create WADO Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.WadoServiceReconciled(cr, state.WadoService, eventProcessorServiceEndpoint),
		Msg: "Update WADO Service",
	}
}

func (i *DicomwebIngestionServiceReconciler) GetStowSinkBindingDesiredState(state *DicomwebIngestionServiceState, cr *v1alpha1.DicomwebIngestionService) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}

	binding := model.StowSinkBinding(cr, model.GetStowServiceName(cr), model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	if state.StowSinkBinding == nil {
		return common.GenericCreateAction{
			Ref: binding,
			Msg: "Create STOW SinkBinding",
		}
	}

	return nil
}
