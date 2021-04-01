/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package events

// StudyRevisionEvent is the Study Revision Event
type StudyRevisionEvent struct {
	Study    DicomStudy `json:"study"`
	Endpoint string     `json:"endpoint"`
}

// DicomStudy is Dicom Study
type DicomStudy struct {
	StudyInstanceUID string        `json:"studyInstanceUID"`
	Series           []DicomSeries `json:"series"`
}

// DicomSeries is Dicom Series
type DicomSeries struct {
	SeriesInstanceUID string                 `json:"seriesInstanceUID"`
	Number            int                    `json:"number"`
	Attributes        []DicomSeriesAttribute `json:"attributes"`
	Instances         []DicomInstance        `json:"instances"`
}

// DicomSeriesAttribute is Dicom Series Attribute
type DicomSeriesAttribute struct {
	Group   int    `json:"group"`
	Element int    `json:"element"`
	Value   string `json:"value"`
}

// DicomInstance is Dicom Instance
type DicomInstance struct {
	SopInstanceUID string `json:"sopInstanceUID"`
	SopClassUID    string `json:"sopClassUID"`
	Number         int    `json:"number"`
}
