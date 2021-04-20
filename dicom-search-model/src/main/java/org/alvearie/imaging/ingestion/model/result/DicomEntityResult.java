/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonSerialize(using = DicomEntityResultSerializer.class)
public class DicomEntityResult extends DicomSearchResult {
    private DicomResource resource;

    public DicomEntityResult() {
        super();
    }

    public DicomResource getResource() {
        return resource;
    }

    public void setResource(DicomResource resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "DicomEntityResult [resource=" + resource + "]";
    }
}
