/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.io.IOException;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomService;

public abstract class AbstractDicomService implements DicomService {
    private final String[] sopClasses;

    protected AbstractDicomService(String... sopClasses) {
        this.sopClasses = sopClasses.clone();
    }

    @Override
    public String[] getSOPClasses() {
        return sopClasses;
    }

    @Override
    public void onClose(Association as) {
        // NOOP
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, PDVInputStream data)
            throws IOException {
        onDimseRQ(as, pc, dimse, cmd, readDataset(pc, data));
    }

    private Attributes readDataset(PresentationContext pc, PDVInputStream data) throws IOException {
        if (data == null)
            return null;

        Attributes dataset = data.readDataset(pc.getTransferSyntax());
        Dimse.LOG.debug("Dataset:\n{}", dataset);
        return dataset;
    }

    protected abstract void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd,
            Attributes data) throws IOException;
}
