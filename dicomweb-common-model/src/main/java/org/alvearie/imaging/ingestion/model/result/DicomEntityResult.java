/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

public class DicomEntityResult extends DicomSearchResult {
    public class DicomResource {
        private String objectName;

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

    }

    public DicomEntityResult() {

    }

    private DicomResource resource;

    public DicomResource createResource() {
        return new DicomResource();
    }

    public DicomResource getResource() {
        return resource;
    }

    public void setResource(DicomResource resource) {
        this.resource = resource;
    }

    // Override to remove @JsonValue
    @Override
    @JsonValue(false)
    public Map<String, DicomAttribute> getAttributes() {
        return super.getAttributes();
    }
}
