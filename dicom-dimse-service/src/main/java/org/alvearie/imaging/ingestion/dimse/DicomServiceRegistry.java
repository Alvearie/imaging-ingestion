/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.DimseRQHandler;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.CommonExtendedNegotiation;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;

@ApplicationScoped
public class DicomServiceRegistry implements DimseRQHandler {
    private final HashMap<String, DimseRQHandler> services = new HashMap<String, DimseRQHandler>();

    public void addDicomService(DicomService service) {
        addDimseRQHandler(service, service.getSOPClasses());
    }

    public synchronized void addDimseRQHandler(DimseRQHandler service, String... sopClasses) {
        for (String uid : sopClasses)
            services.put(uid, service);
    }

    public void removeDicomService(DicomService service) {
        removeDimseRQHandler(service.getSOPClasses());
    }

    public synchronized void removeDimseRQHandler(String... sopClasses) {
        for (String uid : sopClasses)
            services.remove(uid);
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, PDVInputStream data)
            throws IOException {
        try {
            lookupService(as, dimse, cmd).onDimseRQ(as, pc, dimse, cmd, data);
        } catch (DicomServiceException e) {
            Association.LOG.info("{}: processing {} failed. Caused by:\t", as,
                    dimse.toString(cmd, pc.getPCID(), pc.getTransferSyntax()), e);
            Attributes rsp = e.mkRSP(dimse.commandFieldOfRSP(), cmd.getInt(Tag.MessageID, 0));
            as.tryWriteDimseRSP(pc, rsp, e.getDataset());
        }
    }

    private DimseRQHandler lookupService(Association as, Dimse dimse, Attributes cmd) throws DicomServiceException {
        String cuid = cmd.getString(dimse.tagOfSOPClassUID());
        if (cuid == null)
            throw new DicomServiceException(Status.MistypedArgument);

        DimseRQHandler service = services.get(cuid);
        if (service != null)
            return service;

        if (dimse == Dimse.C_STORE_RQ) {
            CommonExtendedNegotiation commonExtNeg = as.getCommonExtendedNegotiationFor(cuid);
            if (commonExtNeg != null) {
                for (String uid : commonExtNeg.getRelatedGeneralSOPClassUIDs()) {
                    service = services.get(uid);
                    if (service != null)
                        return service;
                }
                service = services.get(commonExtNeg.getServiceClassUID());
                if (service != null)
                    return service;
            }
            service = services.get("*");
            if (service != null)
                return service;
        }
        throw new DicomServiceException(dimse.isCService() ? Status.SOPclassNotSupported : Status.NoSuchSOPclass);
    }

    @Override
    public void onClose(Association as) {
        for (DimseRQHandler service : services.values())
            service.onClose(as);
    }
}
