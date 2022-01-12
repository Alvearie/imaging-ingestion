/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package controllers

import (
	"context"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	apiErrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
)

var logger = logf.Log.WithName("controllers")

func GetEventDrivenIngestionResource(context context.Context, namespacedName types.NamespacedName, controllerClient client.Client) (*v1alpha1.DicomEventDrivenIngestion, error) {
	resource := model.EventDrivenIngestionResource()
	resourceSelector := model.EventDrivenIngestionResourceSelector(namespacedName.Name, namespacedName.Namespace)

	err := controllerClient.Get(context, resourceSelector, resource)
	if err != nil {
		// If the resource type doesn't exist on the cluster or does exist but is not found
		if meta.IsNoMatchError(err) || apiErrors.IsNotFound(err) {
			return nil, nil
		} else {
			return nil, err
		}
	} else {
		return resource.DeepCopy(), nil
	}
}

func GetWadoEndpoints(context context.Context, client client.Client, eventDrivenIngestionName string) (string, string) {
	wadoExtEndpoint := ""
	wadoIntEndpoint := ""
	webIngestions := &v1alpha1.DicomwebIngestionServiceList{}
	if err := client.List(context, webIngestions); err == nil {
		for _, wi := range webIngestions.Items {
			if wi.Spec.DicomEventDrivenIngestionName == eventDrivenIngestionName {
				wadoExtEndpoint = wi.Status.WadoServiceExternalEndpoint
				wadoIntEndpoint = wi.Status.WadoServiceInternalEndpoint
			}
		}
	}

	return wadoExtEndpoint, wadoIntEndpoint
}
