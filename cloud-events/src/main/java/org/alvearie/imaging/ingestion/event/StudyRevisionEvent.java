/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public class StudyRevisionEvent {
    private int revision;
    private DicomStudy study;
    private StudyRevisionChangeSet changeSet;

    public DicomStudy getStudy() {
        return study;
    }

    public void setStudy(DicomStudy study) {
        this.study = study;
    }
    
    public StudyRevisionChangeSet getChangeSet() {
        return changeSet;
    }
    
    public void setChangeSet(StudyRevisionChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }
}
