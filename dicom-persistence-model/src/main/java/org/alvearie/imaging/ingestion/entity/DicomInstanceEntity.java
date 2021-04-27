/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.entity;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "DICOM_INSTANCE")
public class DicomInstanceEntity extends PanacheEntity {
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    public DicomSeriesEntity series;

    @Column(name = "sop_instance_uid", unique = true)
    public String sopInstanceUID;

    @Column(name = "sop_class_uid")
    public String sopClassUID;

    public Integer number;

    public String objectName;

    @Column(name = "last_modified")
    public OffsetDateTime lastModified;

    public static DicomInstanceEntity findBySopInstanceUID(String id) {
        return find("sopInstanceUID", id).firstResult();
    }

    public static DicomInstanceEntity findBySopInstanceUID(String id, String source) {
        return find("sopInstanceUID = ?1 and series.study.provider.name = ?2", id, source).firstResult();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if ((object == null) || !(object instanceof DicomInstanceEntity)) {
            return false;
        }

        final DicomInstanceEntity e = (DicomInstanceEntity) object;

        if (sopInstanceUID != null && e.sopInstanceUID != null) {
            return sopInstanceUID.equals(e.sopInstanceUID);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DicomInstanceEntity [sopInstanceUID=" + sopInstanceUID + ", sopClassUID=" + sopClassUID + ", number="
                + number + ", objectName=" + objectName + ", lastModified=" + lastModified + ", id=" + id + "]";
    }
}
