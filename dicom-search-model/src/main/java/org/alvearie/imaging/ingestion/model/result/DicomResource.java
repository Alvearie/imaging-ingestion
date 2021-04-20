/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DicomResource {
    private String objectName;

    public DicomResource() {
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    @Override
    public String toString() {
        return "DicomResource [objectName=" + objectName + "]";
    }
}
