/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package cmd

import (
	"bytes"
	"crypto/tls"
	"errors"
	"fmt"
	"io"
	"log"
	"mime/multipart"
	"net/http"
	"net/textproto"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/spf13/cobra"
)

func init() {
	stowCmd.Flags().StringVar(&imagePath, "image-path", "", "DICOM Image Path of File or Folder")
	stowCmd.Flags().IntVar(&concurrency, "concurrency", 1, "Ingest Concurrency")
	stowCmd.Flags().StringVar(&endpoint, "endpoint", os.Getenv("STOW_ENDPOINT"), "Ingest Endpoint")
	stowCmd.Flags().StringVar(&token, "token", os.Getenv("TOKEN"), "Authorization token (ex: Bearer xxxx)")
	rootCmd.AddCommand(stowCmd)
}

var stowCmd = &cobra.Command{
	Use:   "stow",
	Short: "Ingest DICOM Images using stow-rs",
	Run: func(cmd *cobra.Command, args []string) {
		stow(cmd, args)
	},
}

func stow(cmd *cobra.Command, args []string) {
	if endpoint == "" {
		panic(errors.New("Ingest Endpoint is not set"))
	}

	defer timeTrack(time.Now(), "Stow")
	m, err := stowIngestAll(imagePath)
	if err != nil {
		log.Println(err)
		return
	}
	var paths []string
	for path := range m {
		paths = append(paths, path)
	}
	log.Printf("Ingested %d files\n", len(paths))
}

// stowIngester reads path names from paths and sends status code of the post
// on c until either paths or done is closed.
func stowIngester(done <-chan struct{}, paths <-chan string, c chan<- result) {
	for path := range paths { // HLpaths
		code, err := stowPost(path)
		select {
		case c <- result{path, code, err}:
		case <-done:
			return
		}
	}
}

func stowPost(path string) (int, error) {
	file, err := os.Open(path)
	if err != nil {
		log.Printf("Error opening file: %s", err.Error())
		return 0, err
	}
	defer file.Close()

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	h := make(textproto.MIMEHeader)
	h.Set("Content-Disposition", fmt.Sprintf(`attachment; name="%s"; filename="%s"`, "file", filepath.Base(path)))
	h.Set("Content-Type", "application/dicom")
	part, err := writer.CreatePart(h)
	if err != nil {
		return 0, err
	}
	_, err = io.Copy(part, file)

	err = writer.Close()
	if err != nil {
		return 0, err
	}

	req, err := http.NewRequest(http.MethodPost, endpoint, body)
	if err != nil {
		log.Printf("Error creating new http request: %s", err.Error())
		return 0, err
	}
	req.Header.Set("Content-Type", "multipart/related;type=application/dicom"+";boundary="+writer.Boundary())
	req.Header.Set("Accept", "application/dicom+xml")
	if token != "" {
		req.Header.Set("Authorization", token)
	}
	req.Close = true

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error sending request: %s", err.Error())
		return 0, err
	}
	defer resp.Body.Close()

	// log.Printf("Request sent: %s\n", resp.Status)
	return resp.StatusCode, nil
}

func stowIngestAll(root string) (map[string]int, error) {
	// ingestAll closes the done channel when it returns; it may do so before
	// receiving all the values from c and errc.
	done := make(chan struct{})
	defer close(done)

	paths, errc := walkFiles(done, root)

	// Start a fixed number of goroutines to read and ingest files.
	c := make(chan result) // HLc
	var wg sync.WaitGroup
	wg.Add(concurrency)
	for i := 0; i < concurrency; i++ {
		go func() {
			stowIngester(done, paths, c) // HLc
			wg.Done()
		}()
	}
	go func() {
		wg.Wait()
		close(c) // HLc
	}()
	// End of pipeline. OMIT

	m := make(map[string]int)
	for r := range c {
		/*
			if r.err != nil {
				return nil, r.err
			}
		*/
		m[r.path] = r.statusCode
	}
	// Check whether the Walk failed.
	if err := <-errc; err != nil { // HLerrc
		return nil, err
	}
	return m, nil
}
