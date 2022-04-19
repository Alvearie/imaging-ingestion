/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"mime"
	"mime/multipart"
	"net/http"
	"os"
	"strings"

	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/suyashkumar/dicom"
)

const (
	// DicomAvailableEventType is the Dicom Available Event Type
	DicomAvailableEventType string = "DicomAvailableEvent"
)

// DicomAvailableEvent is the Dicom Available Event struct
type DicomAvailableEvent struct {
	Provider string `json:"provider"`
	Endpoint string `json:"endpoint"`
}

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
	var dicomAvailableEvent DicomAvailableEvent
	if err := event.DataAs(&dicomAvailableEvent); err != nil {
		log.Printf("failed to convert data: %s\n", err)
		return
	}

	fmt.Printf("dicomReadURL: %s\n", dicomAvailableEvent.Endpoint)
	filePath, err := recv.Get(dicomAvailableEvent.Endpoint)
	if err != nil {
		log.Printf("failed to get dicom image: %s\n", err)
		return
	}

	err = recv.Process(filePath)
	if err != nil {
		log.Printf("failed to process dicom file: %s\n", err)
		return
	}

	err = recv.Cleanup(filePath)
	if err != nil {
		log.Printf("failed to cleanup temp file: %s\n", err)
		return
	}
}

// Process will process the dicom image
func (recv *Receiver) Process(path string) error {
	dataset, err := dicom.ParseFile(path, nil)
	if err != nil {
		log.Printf("Error opening file: %s", err.Error())
		return err
	}

	// Print dataset data
	fmt.Println(dataset)

	return nil
}

// Get will get dicom image from wado endpoint
func (recv *Receiver) Get(url string) (string, error) {
	insecureSkipVerify := false
	if os.Getenv("INSECURE_SKIP_VERIFY") == "true" {
		insecureSkipVerify = true
	}
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: insecureSkipVerify},
	}

	client := &http.Client{Transport: tr}
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		log.Printf("Error creating new http request: %s", err.Error())
		return "", err
	}

	req.Header.Set("Accept", "application/json; multipart/related;type=application/dicom")
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error sending request: %s", err.Error())
		return "", err
	}
	defer resp.Body.Close()

	fmt.Print("Response Headers: ")
	fmt.Println(resp.Header)
	fmt.Printf("Response Status: %s\n", resp.Status)

	contentType, params, err := mime.ParseMediaType(resp.Header.Get("Content-Type"))
	if err != nil {
		log.Printf("cannot get content-type: %s", err.Error())
		return "", err
	}

	f, err := ioutil.TempFile("", "dcm")
	if err != nil {
		log.Printf("cannot open temp file: %s", err.Error())
		return "", err
	}
	defer f.Close()

	if strings.HasPrefix(contentType, "multipart/") {
		multipartReader := multipart.NewReader(resp.Body, params["boundary"])
		part, err := multipartReader.NextPart()
		if err != nil {
			log.Printf("Error reading part: %s", err.Error())
			return "", err
		}
		defer part.Close()

		io.Copy(f, part)
	} else {
		io.Copy(f, resp.Body)
	}

	fmt.Printf("File copied to %s\n", f.Name())
	return f.Name(), nil
}

// Cleanup will delete temp file
func (recv *Receiver) Cleanup(path string) error {
	fmt.Printf("Removing temp file: %s\n", path)
	return os.Remove(path)
}
