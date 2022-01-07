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

func (r *DicomEventBridgeReconciler) GetDesiredState(currentState *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetEventDrivenIngestionDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetNatsTokenSecretDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetNatsConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetEdgeLocationConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetEventBridgeDeploymentDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetEventBridgeServiceDesiredState(currentState, cr))

	return desired
}

func (i *DicomEventBridgeReconciler) GetEventDrivenIngestionDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	if state.EventDrivenIngestion == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing EventDrivenIngestion resource"),
			Msg: "Missing EventDrivenIngestion resource",
		}
	}

	return nil
}

func (i *DicomEventBridgeReconciler) GetNatsTokenSecretDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	if cr.Spec.NatsTokenSecretName != "" && state.NatsTokenSecret == nil {
		return common.GenericErrorAction{
			Ref: errors.New("Missing NATS Token Secret"),
			Msg: "Missing NATS Token Secret",
		}
	}

	return nil
}

func (i *DicomEventBridgeReconciler) GetNatsConfigDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	config := model.EventBridgeNatsConfig(cr)
	if state.NatsConfig == nil {
		return common.GenericCreateAction{
			Ref: config,
			Msg: "Create NATS Config",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.EventBridgeNatsConfigReconciled(cr, state.NatsConfig),
		Msg: "Update NATS Config",
	}
}

func (i *DicomEventBridgeReconciler) GetEdgeLocationConfigDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	if cr.Spec.Role != string(common.BridgeRoleHub) {
		return nil
	}

	config := model.EventBridgeEdgeLocationConfig(cr)
	if state.EdgeLocationConfig == nil {
		return common.GenericCreateAction{
			Ref: config,
			Msg: "Create Edge Location Config",
		}
	}

	return nil
}

func (i *DicomEventBridgeReconciler) GetEventBridgeDeploymentDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	deploy := model.EventBridgeDeployment(cr, state.EventDrivenIngestion)
	if state.EventBridgeDeployment == nil {
		return common.GenericCreateAction{
			Ref: deploy,
			Msg: "Create Event Bridge Deployment",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.EventBridgeDeploymentReconciled(cr, state.EventBridgeDeployment, state.EventDrivenIngestion),
		Msg: "Update Event Bridge Deployment",
	}
}

func (i *DicomEventBridgeReconciler) GetEventBridgeServiceDesiredState(state *DicomEventBridgeState, cr *v1alpha1.DicomEventBridge) common.ControllerAction {
	service := model.EventBridgeService(cr)
	if state.EventBridgeService == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create Event Bridge Service",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.EventBridgeServiceReconciled(cr, state.EventBridgeService),
		Msg: "Update Event Bridge Service",
	}
}
