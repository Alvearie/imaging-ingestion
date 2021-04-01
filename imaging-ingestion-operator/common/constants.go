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

	EventProcessorServiceImage  = "alvearie/dicom-event-driven-ingestion:1.0.0"
	StowServiceImage            = "alvearie/dicomweb-stow-service:1.0.0"
	WadoServiceImage            = "alvearie/dicomweb-wado-service:1.0.0"
	DimseIngestionImage         = "alvearie/dicom-dimse-service:1.0.0"
	StudyBindingServiceImage    = "alvearie/dicomweb-fhir-binding:1.0.0"
	InstanceBindingServiceImage = "alvearie/dicomweb-stow-binding:1.0.0"
	DimseProxyImage             = "alvearie/dicom-dimse-proxy:1.0.0"

	MinScaleAnnotation = "autoscaling.knative.dev/minScale"
	MaxScaleAnnotation = "autoscaling.knative.dev/maxScale"

	ImageStoredEventType    EventType = "ImageStoredEvent"
	DicomAvailableEventType EventType = "DicomAvailableEvent"
	StudyRevisionEventType  EventType = "StudyRevisionEvent"
)
