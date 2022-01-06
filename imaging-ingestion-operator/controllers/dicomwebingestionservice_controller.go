/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	ksourcesv1 "knative.dev/eventing/pkg/apis/sources/v1"
	kservingv1 "knative.dev/serving/pkg/apis/serving/v1"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/common"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/reconciler"
)

// DicomwebIngestionServiceReconciler reconciles a DicomwebIngestionService object
type DicomwebIngestionServiceReconciler struct {
	client.Client
	Scheme *runtime.Scheme
}

//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dicomwebingestionservices,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dicomwebingestionservices/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=imaging-ingestion.alvearie.org,namespace=system,resources=dicomwebingestionservices/finalizers,verbs=update

//+kubebuilder:rbac:groups=core,namespace=system,resources=configmaps,verbs=get;list;watch
//+kubebuilder:rbac:groups=core,namespace=system,resources=secrets,verbs=get;list;watch
//+kubebuilder:rbac:groups=serving.knative.dev,namespace=system,resources=services,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=sources.knative.dev,namespace=system,resources=sinkbindings,verbs=get;list;watch;create;update;patch;delete

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.7.2/pkg/reconcile
func (r *DicomwebIngestionServiceReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	logger := log.FromContext(ctx)
	logger.Info("Reconciling DicomwebIngestionService")

	// Check resource exists
	instance := &v1alpha1.DicomwebIngestionService{}
	if found, err := common.IsResourceFound(r.Client, ctx, req, instance); !found {
		return reconcile.Result{}, err
	}

	// Initialize state
	currentState := NewDicomwebIngestionServiceState(r.Client, r.Scheme)

	// Read current state
	if result, err := reconciler.ReadCurrentState(r.Client, ctx, instance, &instance.Status.CommonStatusSpec, currentState); err != nil {
		return result, err
	}

	// Set desired state
	desiredState := r.GetDesiredState(currentState, instance)
	return reconciler.RunDesiredStateActions(r.Client, r.Scheme, ctx, instance, &instance.Status.CommonStatusSpec, currentState, desiredState)
}

// SetupWithManager sets up the controller with the Manager.
func (r *DicomwebIngestionServiceReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&v1alpha1.DicomwebIngestionService{}).
		Owns(&corev1.ConfigMap{}).
		Owns(&corev1.Secret{}).
		Owns(&kservingv1.Service{}).
		Owns(&ksourcesv1.SinkBinding{}).
		Complete(r)
}
