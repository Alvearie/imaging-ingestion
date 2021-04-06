/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package common

import "time"

type EventType string

const (
	OpenShiftAPIServerKind = "OpenShiftAPIServer"
	KnativeServingKind     = "Service"
	KnativeEventingKind    = "Broker"

	AutoDetectTick    = 30 * time.Second
	RequeueDelay      = 5 * time.Minute
	RequeueDelayError = 5 * time.Second

	EventProcessorServiceImage  = "alvearie/dicom-event-driven-ingestion:0.0.1"
	StowServiceImage            = "alvearie/dicomweb-stow-service:0.0.1"
	WadoServiceImage            = "alvearie/dicomweb-wado-service:0.0.1"
	DimseIngestionImage         = "alvearie/dicom-dimse-service:0.0.1"
	StudyBindingServiceImage    = "alvearie/dicomweb-fhir-binding:0.0.1"
	InstanceBindingServiceImage = "alvearie/dicomweb-stow-binding:0.0.1"
	DimseProxyImage             = "alvearie/dicom-dimse-proxy:0.0.1"

	MinScaleAnnotation = "autoscaling.knative.dev/minScale"
	MaxScaleAnnotation = "autoscaling.knative.dev/maxScale"

	ImageStoredEventType    EventType = "ImageStoredEvent"
	DicomAvailableEventType EventType = "DicomAvailableEvent"
	StudyRevisionEventType  EventType = "StudyRevisionEvent"
)
