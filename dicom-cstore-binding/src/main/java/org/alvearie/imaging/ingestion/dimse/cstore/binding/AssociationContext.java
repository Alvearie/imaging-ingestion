/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.cstore.binding;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.dcm4che3.net.Association;

public class AssociationContext {
    private Association association;
    private OffsetDateTime lastUpdated;

    public AssociationContext(Association association) {
        this.association = association;
        this.lastUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void markLastUpdated() {
        this.lastUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Association getAssociation() {
        return association;
    }

    public void setAssociation(Association association) {
        this.association = association;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
