/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.LockModeType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "DICOM_STUDY")
public class DicomStudyEntity extends PanacheEntity {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "study", fetch = FetchType.LAZY)
    public ProviderEntity provider;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "study", fetch = FetchType.LAZY)
    @OrderBy("number")
    public List<DicomSeriesEntity> series = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "study", fetch = FetchType.LAZY)
    public List<DicomStudyAttributesEntity> attributes = new ArrayList<>();

    @Column(name = "study_instance_uid", unique = true)
    public String studyInstanceUID;

    @Column(name = "study_id")
    public String studyID;

    @Column(name = "study_date")
    public String studyDate;

    @Column(name = "study_time")
    public String studyTime;

    public Integer revision;

    @Column(name = "revision_time")
    public OffsetDateTime revisionTime;

    public void addSeries(DicomSeriesEntity s) {
        s.study = this;
        if (!this.series.contains(s)) {
            this.series.add(s);
        }
    }

    public void addAttribute(DicomStudyAttributesEntity attr) {
        attr.study = this;
        if (!this.attributes.contains(attr)) {
            this.attributes.add(attr);
        }
    }

    public static DicomStudyEntity findByStudyInstanceUID(String id, boolean lock) {
        if (lock) {
            return find("studyInstanceUID", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        } else {
            return find("studyInstanceUID", id).firstResult();
        }
    }

    public static DicomStudyEntity findByStudyInstanceUID(String id, String source, boolean lock) {
        if (lock) {
            return find("studyInstanceUID = ?1 and provider.name = ?2", id, source)
                    .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        } else {
            return find("studyInstanceUID = ?1 and provider.name = ?2", id, source).firstResult();
        }
    }

    @Override
    public String toString() {
        return "DicomStudyEntity [provider=" + provider + ", series=" + series + ", attributes=" + attributes
                + ", studyInstanceUID=" + studyInstanceUID + ", studyID=" + studyID + ", studyDate=" + studyDate
                + ", studyTime=" + studyTime + ", revision=" + revision + ", revisionTime=" + revisionTime + ", id="
                + id + "]";
    }
}
