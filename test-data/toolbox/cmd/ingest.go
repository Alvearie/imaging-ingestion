/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package cmd

import (
	"bufio"
	"crypto/tls"
	"errors"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/spf13/cobra"
)

var (
	imagePath   string
	concurrency int
	endpoint    string
	token       string
)

func init() {
	ingestCmd.Flags().StringVar(&imagePath, "image-path", "", "DICOM Image Path of File or Folder")
	ingestCmd.Flags().IntVar(&concurrency, "concurrency", 1, "Ingest Concurrency")
	ingestCmd.Flags().StringVar(&endpoint, "endpoint", os.Getenv("INGEST_ENDPOINT"), "Ingest Endpoint")
	ingestCmd.Flags().StringVar(&token, "token", os.Getenv("TOKEN"), "Authorization token (ex: Bearer xxxx)")
	rootCmd.AddCommand(ingestCmd)
}

var ingestCmd = &cobra.Command{
	Use:   "ingest",
	Short: "Ingest DICOM Images",
	Run: func(cmd *cobra.Command, args []string) {
		ingest(cmd, args)
	},
}

func ingest(cmd *cobra.Command, args []string) {
	if endpoint == "" {
		panic(errors.New("Ingest Endpoint is not set"))
	}

	defer timeTrack(time.Now(), "Ingest")
	m, err := ingestAll(imagePath)
	if err != nil {
		log.Println(err)
		return
	}
	var paths []string
	for path := range m {
		paths = append(paths, path)
	}

	/*
		for _, path := range paths {
			log.Printf("%s: %d\n", path, m[path])
		}
	*/
	log.Printf("Ingested %d files\n", len(paths))
}

// walkFiles starts a goroutine to walk the directory tree at root and send the
// path of each regular file on the string channel.  It sends the result of the
// walk on the error channel.  If done is closed, walkFiles abandons its work.
func walkFiles(done <-chan struct{}, root string) (<-chan string, <-chan error) {
	paths := make(chan string)
	errc := make(chan error, 1)
	go func() { // HL
		// Close the paths channel after Walk returns.
		defer close(paths) // HL
		// No select needed for this send, since errc is buffered.
		errc <- filepath.Walk(root, func(path string, info os.FileInfo, err error) error { // HL
			if err != nil {
				return err
			}
			if !info.Mode().IsRegular() {
				return nil
			}
			select {
			case paths <- path: // HL
			case <-done: // HL
				return errors.New("walk canceled")
			}
			return nil
		})
	}()
	return paths, errc
}

type result struct {
	path       string
	statusCode int
	err        error
}

// ingester reads path names from paths and sends status code of the post
// on c until either paths or done is closed.
func ingester(done <-chan struct{}, paths <-chan string, c chan<- result) {
	for path := range paths { // HLpaths
		code, err := post(path)
		select {
		case c <- result{path, code, err}:
		case <-done:
			return
		}
	}
}

func post(path string) (int, error) {
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

	reader := bufio.NewReader(file)
	req, err := http.NewRequest(http.MethodPut, endpoint, reader)
	if err != nil {
		log.Printf("Error creating new http request: %s", err.Error())
		return 0, err
	}
	req.Header.Set("Content-Type", "application/octet-stream")
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

func ingestAll(root string) (map[string]int, error) {
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
			ingester(done, paths, c) // HLc
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

func timeTrack(start time.Time, name string) {
	elapsed := time.Since(start)
	log.Printf("%s took %s", name, elapsed)
}
