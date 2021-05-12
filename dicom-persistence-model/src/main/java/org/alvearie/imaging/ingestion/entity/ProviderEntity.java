/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "PROVIDER")
public class ProviderEntity extends PanacheEntity {
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    public DicomSeriesEntity series;

    public String name;

    public String bucketEndpoint;

    public String bucketRegion;

    public String bucketName;

    public String wadoInternalEndpoint;

    public String wadoExternalEndpoint;

    @Override
    public String toString() {
        return "ProviderEntity [name=" + name + ", bucketEndpoint=" + bucketEndpoint + ", bucketRegion=" + bucketRegion
                + ", bucketName=" + bucketName + ", wadoInternalEndpoint=" + wadoInternalEndpoint
                + ", wadoExternalEndpoint=" + wadoExternalEndpoint + "]";
    }
}
