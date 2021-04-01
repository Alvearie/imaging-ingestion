/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public class DicomSeriesAttribute {
    private Integer group;
    private Integer element;
    private String value;

    public Integer getGroup() {
        return group;
    }

    public void setGroup(Integer group) {
        this.group = group;
    }

    public Integer getElement() {
        return element;
    }

    public void setElement(Integer element) {
        this.element = element;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
