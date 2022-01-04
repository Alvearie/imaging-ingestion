/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"errors"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
)

func (r *DicomEventDrivenIngestionReconciler) GetDesiredState(currentState *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetDatabaseSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetDatabaseConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetEventProcessorServiceDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetEventBrokerDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetImageStoredTriggerDesiredState(currentState, cr))

	return desired
}

func (i *DicomEventDrivenIngestionReconciler) GetDatabaseSecretDesiredState(state *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.ControllerAction {
	if state.DatabaseSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Database Secret"),
			Msg: "Missing Database Secret",
		}
	}

	return nil
}

func (i *DicomEventDrivenIngestionReconciler) GetDatabaseConfigDesiredState(state *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.ControllerAction {
	if state.DatabaseConfig == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Database Config"),
			Msg: "Missing Database Config",
		}
	}

	return nil
}

func (i *DicomEventDrivenIngestionReconciler) GetEventProcessorServiceDesiredState(state *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.ControllerAction {
	service := model.EventProcessorService(cr)
	if state.EventProcessorService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create Event Processor Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.EventProcessorServiceReconciled(cr, state.EventProcessorService),
		Msg: "Update Event Processor Service",
	}
}

func (i *DicomEventDrivenIngestionReconciler) GetEventBrokerDesiredState(state *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.ControllerAction {
	broker := model.EventBroker(cr)
	if state.EventBroker == nil {
		return common.GenericCreateAction{
			Ref: broker,
			Msg: "Create Event Broker",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.EventBrokerReconciled(cr, state.EventBroker),
		Msg: "Update Event Broker",
	}
}

func (i *DicomEventDrivenIngestionReconciler) GetImageStoredTriggerDesiredState(state *DicomEventDrivenIngestionState, cr *v1alpha1.DicomEventDrivenIngestion) common.ControllerAction {
	trigger := model.ImageStoredTrigger(cr)
	if state.ImageStoredTrigger == nil {
		return common.GenericCreateAction{
			Ref: trigger,
			Msg: "Create Image Stored Trigger",
		}
	}

	return nil
}
