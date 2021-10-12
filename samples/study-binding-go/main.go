/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package main

import (
	"context"
	"fmt"
	"log"

	"github.com/Alvearie/imaging-ingestion/dicomweb-fhir-binding/model/events"
	cloudevents "github.com/cloudevents/sdk-go/v2"
)

const (
	// StudyRevisionEventType is the Study Revision Event Type
	StudyRevisionEventType string = "StudyRevisionEvent"
)

// Receiver is a struct
type Receiver struct {
	client cloudevents.Client
}

func main() {
	client, err := cloudevents.NewDefaultClient()
	if err != nil {
		log.Fatal(err.Error())
	}

	r := Receiver{client: client}

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
}
