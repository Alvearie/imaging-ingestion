/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomServiceException;

@ApplicationScoped
public class CEchoSCPImpl extends AbstractDicomService {
    public CEchoSCPImpl() {
        super(UID.Verification);
    }

    public CEchoSCPImpl(String... sopClasses) {
        super(sopClasses);
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, Attributes data)
            throws IOException {
        if (dimse != Dimse.C_ECHO_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        as.tryWriteDimseRSP(pc, Commands.mkEchoRSP(cmd, Status.Success));
    }
}
