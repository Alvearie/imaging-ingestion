/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonRootName(value = "")
public class DicomSearchResult {

    @JsonInclude(Include.NON_NULL)
    public class DicomAttribute {

        @JsonProperty("vr")
        private String vr;

        @JsonProperty("value")
        private List<String> value;

        public String getVr() {
            return vr;
        }

        public void setVr(String vr) {
            this.vr = vr;
        }

        public List<String> getValue() {
            return value;
        }

        public void addValue(String value) {
            if (this.value == null) {
                this.value = new ArrayList<String>();
            }
            this.value.add(value);
        }
    }

    private Map<String, DicomAttribute> attributes = new HashMap<String, DicomAttribute>();

    public DicomSearchResult() {

    }

    public DicomAttribute createAttribute() {
        return new DicomAttribute();
    }

    public void addElement(Integer tag, DicomAttribute element) {
        String tagString = String.format("%08X", tag);
        this.attributes.put(tagString, element);
    }

    @JsonValue
    public Map<String, DicomAttribute> getAttributes() {
        return attributes;
    }

}
