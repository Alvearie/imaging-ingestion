/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

import java.util.List;

public class DicomStudy {
    private String studyInstanceUID;
    private List<Element> attributes;
    private List<DicomSeries> series;
   

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }

    public List<DicomSeries> getSeries() {
        return series;
    }

    public void setSeries(List<DicomSeries> series) {
        this.series = series;
    }

    public List<Element> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Element> attributes) {
        this.attributes = attributes;
    }
}
