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
    private String modality;
    private List<Element> attributes;
    private List<DicomInstance> instances;
    private String endpoint;
    private String providerName;

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

    public List<Element> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Element> attributes) {
        this.attributes = attributes;
    }

    public List<DicomInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<DicomInstance> instances) {
        this.instances = instances;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
