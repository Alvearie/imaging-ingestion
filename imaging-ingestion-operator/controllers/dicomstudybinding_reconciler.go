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

func (r *DicomStudyBindingReconciler) GetDesiredState(currentState *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetStudyBindingSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStudyBindingConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStudyBindingServiceDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStudyBindingSinkBindingDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetStudyBindingTriggerDesiredState(currentState, cr))

	return desired
}

func (i *DicomStudyBindingReconciler) GetStudyBindingSecretDesiredState(state *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.ControllerAction {
	if state.StudyBindingSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Study Binding Secret"),
			Msg: "Missing Study Binding Secret",
		}
	}

	return nil
}

func (i *DicomStudyBindingReconciler) GetStudyBindingConfigDesiredState(state *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.ControllerAction {
	if state.StudyBindingConfig == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing Study Binding Config"),
			Msg: "Missing Study Binding Config",
		}
	}

	return nil
}

func (i *DicomStudyBindingReconciler) GetStudyBindingServiceDesiredState(state *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.ControllerAction {
	service := model.StudyBindingService(cr)
	if state.StudyBindingService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create Study Binding Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.StudyBindingServiceReconciled(cr, state.StudyBindingService),
		Msg: "Update Study Binding Service",
	}
}

func (i *DicomStudyBindingReconciler) GetStudyBindingSinkBindingDesiredState(state *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}

	binding := model.StudyBindingSinkBinding(cr, model.GetEventProcessorServiceName(eventDrivenIngestionResource.Name), model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	if state.StudyBindingSinkBinding == nil {
		return common.GenericCreateAction{
			Ref: binding,
			Msg: "Create Study Binding SinkBinding",
		}
	}

	return nil
}

func (i *DicomStudyBindingReconciler) GetStudyBindingTriggerDesiredState(state *DicomStudyBindingState, cr *v1alpha1.DicomStudyBinding) common.ControllerAction {
	eventDrivenIngestionResource, err := GetEventDrivenIngestionResource(context.Background(), types.NamespacedName{Name: cr.Spec.DicomEventDrivenIngestionName, Namespace: cr.Namespace}, i.Client)
	if eventDrivenIngestionResource == nil || err != nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing DicomEventDrivenIngestion"),
			Msg: "Missing DicomEventDrivenIngestion",
		}
	}

	trigger := model.StudyBindingTrigger(cr, model.GetEventBrokerName(eventDrivenIngestionResource.Name))
	if state.StudyBindingTrigger == nil {
		return common.GenericCreateAction{
			Ref: trigger,
			Msg: "Create Study Binding Trigger",
		}
	}

	return nil
}
