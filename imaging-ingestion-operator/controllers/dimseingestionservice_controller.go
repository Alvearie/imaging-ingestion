/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/reconciler"
)

// DimseIngestionServiceReconciler reconciles a DimseIngestionService object
type DimseIngestionServiceReconciler struct {
	client.Client
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
	logger := log.FromContext(ctx)
	logger.Info("Reconciling DimseIngestionService")

	// Check resource exists
	instance := &v1alpha1.DimseIngestionService{}
	if found, err := common.IsResourceFound(r.Client, ctx, req, instance); !found {
		return reconcile.Result{}, err
	}

	// Initialize state
	currentState := NewDimseIngestionServiceState(r.Client, r.Scheme)

	// Read current state
	if result, err := reconciler.ReadCurrentState(r.Client, ctx, instance, &instance.Status.CommonStatusSpec, currentState); err != nil {
		return result, err
	}

	// Set desired state
	desiredState := r.GetDesiredState(currentState, instance)
	return reconciler.RunDesiredStateActions(r.Client, r.Scheme, ctx, instance, &instance.Status.CommonStatusSpec, currentState, desiredState)
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
