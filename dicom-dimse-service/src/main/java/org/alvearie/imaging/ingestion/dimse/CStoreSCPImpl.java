/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.s3.SimpleStoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomServiceException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CStoreSCPImpl extends AbstractDicomService {
    private static final Logger LOG = Logger.getLogger(CStoreSCPImpl.class);

    @Inject
    StoreService storeService;

    public CStoreSCPImpl() {
        super("*");
    }

    public CStoreSCPImpl(String... sopClasses) {
        super(sopClasses);
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes rq, PDVInputStream data)
            throws IOException {
        if (dimse != Dimse.C_STORE_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        Attributes rsp = Commands.mkCStoreRSP(rq, Status.Success);
        store(as, pc, rq, data, rsp);
        as.tryWriteDimseRSP(pc, rsp);
    }

    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
            throws IOException {
        StoreContext ctx = new SimpleStoreContext();
        ctx.setTransferSyntaxUID(pc.getTransferSyntax());
        storeService.store(ctx, data);
    }

    @Override
    protected void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, Attributes data)
            throws IOException {
        throw new UnsupportedOperationException();
    }
}
