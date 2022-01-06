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

func (r *DicomInstanceBindingReconciler) GetDesiredState(currentState *DicomInstanceBindingState, cr *v1alpha1.DicomInstanceBinding) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetInstanceBindingSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetInstanceBindingConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetInstanceBindingServiceDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetInstanceBindingTriggerDesiredState(currentState, cr))

	return desired
}

func (i *DicomInstanceBindingReconciler) GetInstanceBindingSecretDesiredState(state *DicomInstanceBindingState, cr *v1alpha1.DicomInstanceBinding) common.ControllerAction {
	if state.InstanceBindingSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Instance Binding Secret"),
			Msg: "Missing Instance Binding Secret",
		}
	}

	return nil
}

func (i *DicomInstanceBindingReconciler) GetInstanceBindingConfigDesiredState(state *DicomInstanceBindingState, cr *v1alpha1.DicomInstanceBinding) common.ControllerAction {
	if state.InstanceBindingConfig == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Instance Binding Config"),
			Msg: "Missing Instance Binding Config",
		}
	}

	return nil
}

func (i *DicomInstanceBindingReconciler) GetInstanceBindingServiceDesiredState(state *DicomInstanceBindingState, cr *v1alpha1.DicomInstanceBinding) common.ControllerAction {
	service := model.InstanceBindingService(cr)
	if state.InstanceBindingService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create Instance Binding Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.InstanceBindingServiceReconciled(cr, state.InstanceBindingService),
		Msg: "Update Instance Binding Service",
	}
}

func (i *DicomInstanceBindingReconciler) GetInstanceBindingTriggerDesiredState(state *DicomInstanceBindingState, cr *v1alpha1.DicomInstanceBinding) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}

	trigger := model.InstanceBindingTrigger(cr, model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	if state.InstanceBindingTrigger == nil {
		return common.GenericCreateAction{
			Ref: trigger,
			Msg: "Create Instance Binding Trigger",
		}
	}

	return nil
}
