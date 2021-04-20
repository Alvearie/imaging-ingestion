/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonRootName(value = "")
@JsonSerialize(using = DicomSearchResultSerializer.class)
public class DicomSearchResult {

    private Map<String, DicomAttribute> attributes = new HashMap<String, DicomAttribute>();

    public DicomSearchResult() {

    }

    public void addElement(Integer tag, DicomAttribute element) {
        String tagString = String.format("%08X", tag);
        this.attributes.put(tagString, element);
    }

    public Map<String, DicomAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, DicomAttribute> attributes) {
        this.attributes = attributes;
    }
}
