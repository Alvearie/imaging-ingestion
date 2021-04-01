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

    @Column(name = "study_instance_uid", unique = true)
    public String studyInstanceUID;

    public Integer revision;

    @Column(name = "revision_time")
    public OffsetDateTime revisionTime;

    public void addSeries(DicomSeriesEntity s) {
        s.study = this;
        if (!this.series.contains(s)) {
            this.series.add(s);
        }
    }

    public static DicomStudyEntity findByStudyInstanceUID(String id, boolean lock) {
        if (lock) {
            return find("studyInstanceUID", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        } else {
            return find("studyInstanceUID", id).firstResult();
        }
    }

    @Override
    public String toString() {
        return "DicomStudyEntity [provider=" + provider + ", series=" + series + ", studyInstanceUID="
                + studyInstanceUID + ", revision=" + revision + ", revisionTime=" + revisionTime + ", id=" + id + "]";
    }
}
