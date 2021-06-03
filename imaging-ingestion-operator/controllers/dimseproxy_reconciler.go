/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
)

func (r *DimseProxyReconciler) reconcileInternal(currentState *DimseProxyState, cr *v1alpha1.DimseProxy) common.DesiredResourceState {
	desired := common.DesiredResourceState{}
	desired = desired.AddAction(r.GetNatsConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetDimseConfigDesiredState(currentState, cr))
	desired = desired.AddAction(r.GetDimseProxyDeploymentDesiredState(currentState, cr))

	return desired
}

func (i *DimseProxyReconciler) GetNatsConfigDesiredState(state *DimseProxyState, cr *v1alpha1.DimseProxy) common.ControllerAction {
	config := model.DimseProxyNatsConfig(cr)
	if state.NatsConfig == nil {
		return common.GenericCreateAction{
			Ref: config,
			Msg: "Create NATS Config",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.DimseProxyNatsConfigReconciled(cr, state.NatsConfig),
		Msg: "Update NATS Config",
	}
}

func (i *DimseProxyReconciler) GetDimseConfigDesiredState(state *DimseProxyState, cr *v1alpha1.DimseProxy) common.ControllerAction {
	config := model.DimseConfig(model.GetDimseProxyConfigName(cr.Name), cr.Namespace)
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

func (i *DimseProxyReconciler) GetDimseProxyDeploymentDesiredState(state *DimseProxyState, cr *v1alpha1.DimseProxy) common.ControllerAction {
	service := model.DimseProxyDeployment(cr)
	if state.DimseProxyDeployment == nil {
		return common.GenericCreateAction{
			Ref: service,
			Msg: "Create DIMSE Proxy Deployment",
		}
	}

	return common.GenericUpdateAction{
		Ref: model.DimseProxyDeploymentReconciled(cr, state.DimseProxyDeployment),
		Msg: "Update DIMSE Proxy Deployment",
	}
}
