/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "DICOM_SERIES")
public class DicomSeriesEntity extends PanacheEntity {
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    public DicomStudyEntity study;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "series", fetch = FetchType.LAZY)
    @OrderBy("number")
    public List<DicomInstanceEntity> instances = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "series", fetch = FetchType.LAZY)
    public List<DicomSeriesAttributesEntity> attributes = new ArrayList<>();

    @Column(name = "series_instance_uid", unique = true)
    public String seriesInstanceUID;

    public Integer number;

    public String modality;

    public void addInstance(DicomInstanceEntity inst) {
        inst.series = this;
        if (!this.instances.contains(inst)) {
            this.instances.add(inst);
        }
    }

    public void addAttribute(DicomSeriesAttributesEntity attr) {
        attr.series = this;
        if (!this.attributes.contains(attr)) {
            this.attributes.add(attr);
        }
    }

    public static DicomSeriesEntity findBySeriesInstanceUID(String id, boolean lock) {
        if (lock) {
            return find("seriesInstanceUID", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        } else {
            return find("seriesInstanceUID", id).firstResult();
        }
    }

    public static DicomSeriesEntity findBySeriesInstanceUID(String id, String source, boolean lock) {
        if (lock) {
            return find("seriesInstanceUID = ?1 and study.provider.name = ?2", id, source)
                    .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        } else {
            return find("seriesInstanceUID = ?1 and study.provider.name = ?2", id, source).firstResult();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if ((object == null) || !(object instanceof DicomSeriesEntity)) {
            return false;
        }

        final DicomSeriesEntity e = (DicomSeriesEntity) object;

        if (seriesInstanceUID != null && e.seriesInstanceUID != null) {
            return seriesInstanceUID.equals(e.seriesInstanceUID);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DicomSeriesEntity [instances=" + instances + ", attributes=" + attributes + ", seriesInstanceUID="
                + seriesInstanceUID + ", number=" + number + ", modality=" + modality + ", id=" + id + "]";
    }
}
