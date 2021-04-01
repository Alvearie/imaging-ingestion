/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public class StudyRevisionEvent {
    private DicomStudy study;
    private String endpoint;

    public DicomStudy getStudy() {
        return study;
    }

    public void setStudy(DicomStudy study) {
        this.study = study;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
