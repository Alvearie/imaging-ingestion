/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(Include.NON_NULL)
public class DicomAttribute {

    @JsonProperty("vr")
    private String vr;

    @JsonProperty("Value")
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
