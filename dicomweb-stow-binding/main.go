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
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"mime"
	"mime/multipart"
	"net/http"
	"net/textproto"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/kelseyhightower/envconfig"
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
	client        cloudevents.Client
	StowEndpoint  string `envconfig:"STOW_ENDPOINT"`
	StowAuthToken string `envconfig:"STOW_AUTH_TOKEN"`
	Selector      string `envconfig:"SELECTOR"`
}

var (
	selectorPatterns = []string{}
)

func main() {
	client, err := cloudevents.NewDefaultClient()
	if err != nil {
		log.Fatal(err.Error())
	}

	r := Receiver{client: client}
	envconfig.MustProcess("", &r)

	initSelectorPatterns(r.Selector)

	fmt.Println("Starting server")
	if err := client.StartReceiver(context.Background(), r.Receive); err != nil {
		log.Fatal(err)
	}
}

func initSelectorPatterns(selector string) {
	if selector != "" {
		parts := strings.Split(selector, ",")
		for _, p := range parts {
			p = strings.Trim(p, " ")
			if len(p) > 0 {
				selectorPatterns = append(selectorPatterns, p)
			}
		}
	}
}

func isFilteredEvent(s string) bool {
	if len(selectorPatterns) == 0 {
		return false
	}

	for _, p := range selectorPatterns {
		match, _ := regexp.MatchString(p, s)
		if match {
			return false
		}
	}
	return true
}

// Receive is invoked whenever we receive an event.
func (recv *Receiver) Receive(ctx context.Context, event cloudevents.Event) {
	var dicomAvailableEvent DicomAvailableEvent
	if err := event.DataAs(&dicomAvailableEvent); err != nil {
		log.Printf("failed to convert data: %s\n", err)
		return
	}

	if isFilteredEvent(dicomAvailableEvent.Provider) {
		log.Printf("Filtered event - id: %s, source: %s, provider: %s", event.ID(), event.Source(), dicomAvailableEvent.Provider)
		return
	}

	fmt.Printf("dicomReadURL: %s\n", dicomAvailableEvent.Endpoint)
	filePath, err := recv.Get(dicomAvailableEvent.Endpoint)
	if err != nil {
		log.Printf("failed to get dicom image: %s\n", err)
		return
	}

	err = recv.Post(filePath)
	if err != nil {
		log.Printf("failed to post dicom image to stow endpoint: %s\n", err)
		return
	}

	err = recv.Cleanup(filePath)
	if err != nil {
		log.Printf("failed to cleanup temp file: %s\n", err)
		return
	}
}

// Post will post dicom image to stow endpoint
func (recv *Receiver) Post(path string) error {
	file, err := os.Open(path)
	if err != nil {
		log.Printf("Error opening file: %s", err.Error())
		return err
	}
	defer file.Close()

	insecureSkipVerify := false
	if os.Getenv("INSECURE_SKIP_VERIFY") == "true" {
		insecureSkipVerify = true
	}
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: insecureSkipVerify},
	}
	client := &http.Client{Transport: tr}

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	h := make(textproto.MIMEHeader)
	h.Set("Content-Disposition", fmt.Sprintf(`attachment; name="%s"; filename="%s"`, "file", filepath.Base(path)))
	h.Set("Content-Type", "application/dicom")
	part, err := writer.CreatePart(h)
	if err != nil {
		log.Printf("Error creating multi part: %s", err.Error())
		return err
	}
	_, err = io.Copy(part, file)

	err = writer.Close()
	if err != nil {
		if err != nil {
			log.Printf("Error closing writer: %s", err.Error())
			return err
		}
		return err
	}

	req, err := http.NewRequest(http.MethodPost, recv.StowEndpoint, body)
	if err != nil {
		log.Printf("Error creating new http request: %s", err.Error())
		return err
	}
	req.Header.Set("Content-Type", "multipart/related;type=application/dicom"+";boundary="+writer.Boundary())
	req.Header.Set("Accept", "application/dicom+xml")
	req.Header.Set("Authorization", recv.StowAuthToken)
	req.Close = true

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error sending request: %s", err.Error())
		return err
	}
	defer resp.Body.Close()

	fmt.Println("File posted to stow endpoint")

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
