/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package events

// StudyRevisionEvent is the Study Revision Event
type StudyRevisionEvent struct {
	Revision int                            `json:"revision"`
	Study DicomStudy                        `json:"study"`
	ChangeSet RevisionChangeSet             `json:"changeSet"`
}

// DicomStudy is Dicom Study
type DicomStudy struct {
	StudyInstanceUID string                  `json:"studyInstanceUID"`
	Attributes       []DicomElement          `json:"attributes"`
	Series           []DicomSeries           `json:"series"`
}

// DicomSeries is Dicom Series
type DicomSeries struct {
	SeriesInstanceUID string                 `json:"seriesInstanceUID"`
	Number            int                    `json:"number"`
	Modality          string                 `json:"modality"`
	Attributes        []DicomElement         `json:"attributes"`
	Instances         []DicomInstance        `json:"instances"`
	Endpoint          string                 `json:"endpoint"`
	ProviderName      string                 `json:providerName"`
}

// DicomElement is Dicom tag element
type DicomElement struct {
	Group   int    `json:"group"`
	Element int    `json:"element"`
	VR      string `json:"vr"`
	Value   string `json:"value"`
}

// DicomInstance is Dicom Instance
type DicomInstance struct {
	SopInstanceUID string `json:"sopInstanceUID"`
	SopClassUID    string `json:"sopClassUID"`
	Number         int    `json:"number"`
}

// RevisionChangeSet is the changes in this revision vs the previous revision
type RevisionChangeSet struct {
	Additions     []string   `json:"additions"`
	Deletions     []string   `json:"deletions"`
	Modifications []string   `json:"modifications"`
}
