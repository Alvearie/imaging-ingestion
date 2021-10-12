/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package main

import (
	"bytes"
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/Alvearie/imaging-ingestion/dicomweb-fhir-binding/model/events"
	"github.com/Alvearie/imaging-ingestion/dicomweb-fhir-binding/model/fhir"
	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/google/uuid"
	"github.com/kelseyhightower/envconfig"
)

const (
	// StudyRevisionEventType is the Study Revision Event Type
	StudyRevisionEventType string = "StudyRevisionEvent"
)

// Receiver is a struct
type Receiver struct {
	client               cloudevents.Client
	FhirEndpoint         string `envconfig:"FHIR_ENDPOINT"`
	AuthToken            string `envconfig:"FHIR_AUTH_TOKEN"`
	Username             string `envconfig:"FHIR_AUTH_USERNAME"`
	Password             string `envconfig:"FHIR_AUTH_PASSWORD"`
	ImagingStudyEndpoint string `envconfig:"IMAGINGSTUDY_ENDPOINT"`
}

func main() {
	client, err := cloudevents.NewDefaultClient()
	if err != nil {
		log.Fatal(err.Error())
	}

	r := Receiver{client: client}
	envconfig.MustProcess("", &r)

	fmt.Println("Starting server")
	if err := client.StartReceiver(context.Background(), r.Receive); err != nil {
		log.Fatal(err)
	}
}

// Receive is invoked whenever we receive an event.
func (recv *Receiver) Receive(ctx context.Context, event cloudevents.Event) {
	var studyRevisionEvent events.StudyRevisionEvent
	if err := event.DataAs(&studyRevisionEvent); err != nil {
		log.Printf("failed to convert data: %s\n", err)
		return
	}

	fmt.Printf("StudyRevisionEvent: %v\n", studyRevisionEvent)

	resource := recv.Convert(studyRevisionEvent)

	b, err := json.Marshal(resource)
	if err != nil {
		log.Printf("error getting resource bytes: %s\n", err)
		return
	}

	fmt.Printf("FHIR resource: %s\n", string(b))

	err = recv.Post(b)
	if err != nil {
		log.Printf("failed to post resource to FHIR endpoint: %s\n", err)
		return
	}
}

func (recv *Receiver) Convert(event events.StudyRevisionEvent) fhir.ImagingStudy {
	var contained []interface{}

	study := fhir.ImagingStudy{
		ResourceIdentifier: fhir.ResourceIdentifier{
			ResourceType: "ImagingStudy",
			ID:           uuid.New().String(),
		},
		Identifier: []fhir.Identifier{
			{
				System: "urn:dicom:uid",
				Value:  "urn:oid:" + event.Study.StudyInstanceUID,
			},
		},
		Status:    "available",
		Subject:   recv.AddSubject(&contained),
		Series:    recv.GetSeries(&contained, event.Study.Series),
		Contained: contained,
	}

	return study
}

func (recv *Receiver) AddSubject(contained *[]interface{}) fhir.PatientReference {
	id := "patient.contained.inline"
	*contained = append(*contained,
		fhir.Patient{
			ResourceIdentifier: fhir.ResourceIdentifier{
				ResourceType: "Patient",
				ID:           id,
			},
		},
	)

	return fhir.PatientReference{
		Reference: "#" + id,
	}
}

func (recv *Receiver) GetSeries(contained *[]interface{}, eventSeries []events.DicomSeries) []fhir.ImagingSeries {
	series := []fhir.ImagingSeries{}
	for _, s := range eventSeries {
		endpointID := uuid.New().String()
		*contained = append(*contained, fhir.Endpoint{
			ResourceIdentifier: fhir.ResourceIdentifier{
				ResourceType: "Endpoint",
				ID:           endpointID,
			},
			Status: "active",
			ConnectionType: fhir.Coding{
				System: "http://terminology.hl7.org/CodeSystem/endpoint-connection-type",
				Code:   "dicom-wado-rs",
			},
			PayloadType: []fhir.PayloadType{
				{
					Text: "DICOM WADO-RS",
				},
			},
			Address: recv.GetImagingStudyEndpoint(s.Endpoint),
		})
		series = append(series, fhir.ImagingSeries{
			UID:    s.SeriesInstanceUID,
			Number: s.Number,
			Modality: fhir.Coding{
				System: "http://dicom.nema.org/resources/ontology/DCM",
				Code:   s.Modality,
			},
			NumberOfInstances: len(s.Instances),
			Instance:          getInstances(s.Instances),
			Endpoint: []fhir.EndpointReference{
				{
					Reference: "#" + endpointID,
				},
			},
		})
	}

	return series
}

func (recv *Receiver) GetImagingStudyEndpoint(ep string) string {
	if recv.ImagingStudyEndpoint != "" {
		idx := strings.Index(ep, "/studies")
		if idx != -1 {
			ep = recv.ImagingStudyEndpoint + ep[idx:]
		}
	}

	return ep
}

func getInstances(eventInstances []events.DicomInstance) []fhir.ImagingInstance {
	instances := []fhir.ImagingInstance{}
	for _, inst := range eventInstances {
		instances = append(instances, fhir.ImagingInstance{
			UID: inst.SopInstanceUID,
			SopClass: fhir.Coding{
				System: "urn:ietf:rfc:3986",
				Code:   "urn:oid:" + inst.SopClassUID,
			},
			Number: inst.Number,
		})
	}
	return instances
}

// Post will post resource to FHIR endpoint
func (recv *Receiver) Post(b []byte) error {
	insecureSkipVerify := false
	if os.Getenv("INSECURE_SKIP_VERIFY") == "true" {
		insecureSkipVerify = true
	}
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: insecureSkipVerify},
	}
	client := &http.Client{Transport: tr}

	req, err := http.NewRequest(http.MethodPost, recv.FhirEndpoint, bytes.NewBuffer(b))
	if err != nil {
		log.Printf("Error creating new http request: %s", err.Error())
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	if recv.AuthToken != "" {
		fmt.Println("Using token authentication")
		req.Header.Set("Authorization", recv.AuthToken)
	} else if recv.Username != "" && recv.Password != "" {
		fmt.Println("Using basic authentication")
		req.SetBasicAuth(recv.Username, recv.Password)
	} else {
		fmt.Println("No authentication material")
	}
	req.Close = true

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error sending request: %s", err.Error())
		return err
	}
	defer resp.Body.Close()

	fmt.Printf("Resource posted to FHIR endpoint with status: %s\n", resp.Status)
	body, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(body))

	return nil
}
