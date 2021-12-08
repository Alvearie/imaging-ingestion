/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package reconciler

import (
	"context"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

type ResourceState interface {
	Read(context.Context, client.Object) error
	IsResourcesReady(client.Object) (bool, error)
}

func ManageError(client client.Client, ctx context.Context, instance client.Object, instanceStatus *v1alpha1.CommonStatusSpec, issue error) (reconcile.Result, error) {
	logger := log.Log.WithName("reconciler")

	instanceStatus.Message = issue.Error()
	instanceStatus.Ready = false

	if common.IsResourceNotReadyError(issue) {
		instanceStatus.Phase = v1alpha1.PhaseInitialising
	} else {
		instanceStatus.Phase = v1alpha1.PhaseFailing
	}

	err := client.Status().Update(ctx, instance)
	if err != nil {
		logger.Error(err, "unable to update status")
		return reconcile.Result{
			RequeueAfter: common.RequeueDelayError,
			Requeue:      true,
		}, err
	}

	return reconcile.Result{
		RequeueAfter: common.RequeueDelayError,
		Requeue:      true,
	}, issue
}

func ManageSuccess(client client.Client, ctx context.Context, instance client.Object, instanceStatus *v1alpha1.CommonStatusSpec, resourcesReady bool) (reconcile.Result, error) {
	logger := log.Log.WithName("reconciler")

	// If resources are ready and we have not errored before now, we are in a reconciling phase
	if resourcesReady {
		instanceStatus.Ready = true
		instanceStatus.Phase = v1alpha1.PhaseReconciling
		instanceStatus.Message = "All resource are ready"
	} else {
		instanceStatus.Ready = false
		instanceStatus.Phase = v1alpha1.PhaseInitialising
		instanceStatus.Message = "One or more resources are not ready"
	}

	err := client.Status().Update(ctx, instance)
	if err != nil {
		logger.Error(err, "unable to update status")
		return reconcile.Result{
			RequeueAfter: common.RequeueDelayError,
			Requeue:      true,
		}, err
	}

	return reconcile.Result{RequeueAfter: common.RequeueDelay}, nil
}

func RunDesiredStateActions(client client.Client, scheme *runtime.Scheme, ctx context.Context, instance client.Object, instanceStatus *v1alpha1.CommonStatusSpec, currentState ResourceState, desiredState common.DesiredResourceState) (reconcile.Result, error) {
	// Run the actions to reach the desired state
	actionRunner := common.NewControllerActionRunner(ctx, client, scheme, instance)
	err := actionRunner.RunAll(desiredState)
	if err != nil {
		return ManageError(client, ctx, instance, instanceStatus, err)
	}

	resourcesReady, err := currentState.IsResourcesReady(instance)
	if err != nil {
		return ManageError(client, ctx, instance, instanceStatus, err)
	}

	return ManageSuccess(client, ctx, instance, instanceStatus, resourcesReady)
}

func ReadCurrentState(client client.Client, ctx context.Context, instance client.Object, instanceStatus *v1alpha1.CommonStatusSpec, currentState ResourceState) (reconcile.Result, error) {
	err := currentState.Read(ctx, instance)
	if err != nil {
		return ManageError(client, ctx, instance, instanceStatus, err)
	}

	return reconcile.Result{}, nil
}
