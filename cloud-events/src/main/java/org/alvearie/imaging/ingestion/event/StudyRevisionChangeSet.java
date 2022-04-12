/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

import java.util.List;

public class StudyRevisionChangeSet {
    
    private List<String> additions;
    private List<String> deletions;
    private List<String> modifications;
    
    public List<String> getAdditions() {
        return additions;
    }
    
    public void setAdditions(List<String> additions) {
        this.additions = additions;
    }

    public List<String> getDeletions() {
        return deletions;
    }

    public void setDeletions(List<String> deletions) {
        this.deletions = deletions;
    }

    public List<String> getModifications() {
        return modifications;
    }

    public void setModifications(List<String> modifications) {
        this.modifications = modifications;
    }
}
