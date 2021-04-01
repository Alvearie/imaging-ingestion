/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package dimseingestionservice

import (
	"context"

	"github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
)

var logger = logf.Log.WithName("dimseingestionservice")

// DimseIngestionServiceReconciler reconciles a DimseIngestionService object
type DimseIngestionServiceReconciler struct {
	client.Client
	Log    logr.Logger
	Scheme *runtime.Scheme
}

//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dimseingestionservices,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dimseingestionservices/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dimseingestionservices/finalizers,verbs=update

//+kubebuilder:rbac:groups=core,namespace=system,resources=configmaps,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=core,namespace=system,resources=secrets,verbs=get;list;watch
//+kubebuilder:rbac:groups=apps,namespace=system,resources=deployments,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=core,namespace=system,resources=pods,verbs=get;list;
//+kubebuilder:rbac:groups=serving.knative.dev,namespace=system,resources=services,verbs=get;list;watch;create;update;patch;delete

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.7.2/pkg/reconcile
func (r *DimseIngestionServiceReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	log := r.Log.WithValues("dimseingestionservice", req.NamespacedName)
	log.Info("Reconciling DimseIngestionService")

	instance := &v1alpha1.DimseIngestionService{}
	err := r.Get(ctx, req.NamespacedName, instance)
	if err != nil {
		if errors.IsNotFound(err) {
			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}
		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	currentState := NewDimseIngestionServiceState()

	// Read current state
	err = currentState.Read(ctx, instance, r.Client)
	if err != nil {
		return r.ManageError(ctx, instance, err)
	}

	desiredState := r.reconcileInternal(currentState, instance)
	// Run the actions to reach the desired state
	actionRunner := common.NewControllerActionRunner(ctx, r.Client, r.Scheme, instance)
	err = actionRunner.RunAll(desiredState)
	if err != nil {
		return r.ManageError(ctx, instance, err)
	}

	return r.ManageSuccess(ctx, instance, currentState)
}

func (r *DimseIngestionServiceReconciler) ManageError(ctx context.Context, instance *v1alpha1.DimseIngestionService, issue error) (reconcile.Result, error) {
	instance.Status.Message = issue.Error()
	instance.Status.Ready = false
	instance.Status.Phase = v1alpha1.PhaseFailing

	err := r.Client.Status().Update(ctx, instance)
	if err != nil {
		logger.Error(err, "unable to update status")
	}

	return reconcile.Result{
		RequeueAfter: common.RequeueDelayError,
		Requeue:      true,
	}, nil
}

func (r *DimseIngestionServiceReconciler) ManageSuccess(ctx context.Context, instance *v1alpha1.DimseIngestionService, currentState *DimseIngestionServiceState) (reconcile.Result, error) {
	resourcesReady, err := currentState.IsResourcesReady(instance)
	if err != nil {
		return r.ManageError(ctx, instance, err)
	}

	instance.Status.Ready = resourcesReady
	instance.Status.Message = ""

	// If resources are ready and we have not errored before now, we are in a reconciling phase
	if resourcesReady {
		instance.Status.Phase = v1alpha1.PhaseReconciling
	} else {
		instance.Status.Phase = v1alpha1.PhaseInitialising
	}

	err = r.Client.Status().Update(ctx, instance)
	if err != nil {
		logger.Error(err, "unable to update status")
		return reconcile.Result{
			RequeueAfter: common.RequeueDelayError,
			Requeue:      true,
		}, nil
	}

	return reconcile.Result{RequeueAfter: common.RequeueDelay}, nil
}

// SetupWithManager sets up the controller with the Manager.
func (r *DimseIngestionServiceReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&v1alpha1.DimseIngestionService{}).
		Owns(&corev1.ConfigMap{}).
		Owns(&corev1.Secret{}).
		Owns(&appsv1.Deployment{}).
		Complete(r)
}
