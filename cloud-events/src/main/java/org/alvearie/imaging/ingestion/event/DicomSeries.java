/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

import java.util.List;

public class DicomSeries {
    private String seriesInstanceUID;
    private Integer number;
    private List<DicomSeriesAttribute> attributes;
    private List<DicomInstance> instances;

    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }

    public void setSeriesInstanceUID(String seriesInstanceUID) {
        this.seriesInstanceUID = seriesInstanceUID;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public List<DicomSeriesAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<DicomSeriesAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<DicomInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<DicomInstance> instances) {
        this.instances = instances;
    }
}
