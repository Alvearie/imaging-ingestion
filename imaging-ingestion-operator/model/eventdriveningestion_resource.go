/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// EventDrivenIngestionResource is the Event Driven Ingestion Resource
func EventDrivenIngestionResource() *v1alpha1.DicomEventDrivenIngestion {
	return &v1alpha1.DicomEventDrivenIngestion{}
}

// EventDrivenIngestionResourceSelector is the  Event Driven Ingestion Resource Selector
func EventDrivenIngestionResourceSelector(name, namespace string) client.ObjectKey {
	return client.ObjectKey{
		Name:      name,
		Namespace: namespace,
	}
}

func EventDrivenIngestionResourceReconciled(currentState *v1alpha1.DicomEventDrivenIngestion) *v1alpha1.DicomEventDrivenIngestion {
	reconciled := currentState.DeepCopy()
	return reconciled
}
