/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package fhir

// ImagingStudy is minimal version of ImagingStudy FHIR resource
type ImagingStudy struct {
	ResourceIdentifier `json:",inline"`
	Contained          []interface{}       `json:"contained"`
	Identifier         []Identifier        `json:"identifier"`
	Status             string              `json:"status"`
	Subject            PatientReference    `json:"subject"`
	Endpoint           []EndpointReference `json:"endpoint"`
	Series             []ImagingSeries     `json:"series"`
}

// Identifier is Identifier for the whole study
type Identifier struct {
	System string `json:"system"`
	Value  string `json:"value"`
}

// Coding is representation of a defined concept
type Coding struct {
	System string `json:"system"`
	Code   string `json:"code"`
}

// ImagingSeries is Series of a Study
type ImagingSeries struct {
	UID               string            `json:"uid"`
	Number            int               `json:"number"`
	Modality          Coding            `json:"modality"`
	NumberOfInstances int               `json:"numberOfInstances"`
	Instance          []ImagingInstance `json:"instance"`
}

// ImagingInstance is Instance of a Series
type ImagingInstance struct {
	UID      string `json:"uid"`
	SopClass Coding `json:"sopClass"`
	Number   int    `json:"number"`
}

// PatientReference is Patient Reference
type PatientReference ResourceReference

// EndpointReference is Endpoint Reference
type EndpointReference ResourceReference

// ResourceReference is reference of a Patent or Endpoint
type ResourceReference struct {
	Reference string `json:"reference"`
}

// Patient is minimal Patient resource
type Patient struct {
	ResourceIdentifier `json:",inline"`
}

// Endpoint is minimal Endpoint resource
type Endpoint struct {
	ResourceIdentifier `json:",inline"`
	Status             string        `json:"status"`
	ConnectionType     Coding        `json:"connectionType"`
	PayloadType        []PayloadType `json:"payloadType"`
	Address            string        `json:"address"`
}

// PayloadType is Payload Type
type PayloadType struct {
	Text string `json:"text"`
}

// ResourceIdentifier is identifier of a Resource
type ResourceIdentifier struct {
	ResourceType string `json:"resourceType"`
	ID           string `json:"id"`
}
