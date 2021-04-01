/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public class Store {
    String provider;
    String bucketName;
    String objectName;
    String wadoInternalEndpoint;
    String wadoExternalEndpoint;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getWadoInternalEndpoint() {
        return wadoInternalEndpoint;
    }

    public void setWadoInternalEndpoint(String wadoInternalEndpoint) {
        this.wadoInternalEndpoint = wadoInternalEndpoint;
    }

    public String getWadoExternalEndpoint() {
        return wadoExternalEndpoint;
    }

    public void setWadoExternalEndpoint(String wadoExternalEndpoint) {
        this.wadoExternalEndpoint = wadoExternalEndpoint;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
