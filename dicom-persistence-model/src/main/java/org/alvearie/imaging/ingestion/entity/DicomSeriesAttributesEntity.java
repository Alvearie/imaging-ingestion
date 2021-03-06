/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "DICOM_SERIES_ATTRIBUTES")
public class DicomSeriesAttributesEntity extends PanacheEntity {
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    public DicomSeriesEntity series;

    public Integer tag;

    public String vr;

    @Column(length = 1000)
    public String value;

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if ((object == null) || !(object instanceof DicomSeriesAttributesEntity)) {
            return false;
        }

        final DicomSeriesAttributesEntity e = (DicomSeriesAttributesEntity) object;

        if (tag != null && e.tag != null) {
            return tag.equals(e.tag);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DicomSeriesAttributesEntity [tag=" + tag + ", vr=" + vr + ", value=" + value + ", id=" + id + "]";
    }
}
