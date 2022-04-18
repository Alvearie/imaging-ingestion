/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CEchoHandler implements DimseCommandHandler {
    private static final Logger LOG = Logger.getLogger(CEchoHandler.class);

    @ConfigProperty(name = "dimse.target.ae")
    String targetAe;

    @ConfigProperty(name = "dimse.ae")
    String ae;

    @Inject
    Device clientDevice;

    @Inject
    Connection remoteConnection;

    @Override
    public byte[] onDimseRQ(Dimse dimse, SimpleAssociateRQ arq, SimplePresentationContext pc, Attributes cmd,
            Attributes data) throws Exception {
        LOG.info(String.format("C-ECHO RQ %s, %s", arq.getId(), pc));
        AAssociateRQ rq = new AAssociateRQ();
        for (SimplePresentationContext spc : arq.getPresentationContexts()) {
            rq.addPresentationContext(
                    new PresentationContext(spc.getPCID(), spc.getAbstractSyntax(), spc.getTransferSyntaxes()));
        }
        rq.setCalledAET(targetAe);

        Association targetAssociation = null;
        ByteArrayOutputStream bos = null;
        try {
            try {
                targetAssociation = clientDevice.getApplicationEntity(ae).connect(remoteConnection, rq);
            } catch (IOException e) {
                throw new IOException("No valid connection to target assoiciation");
            }

            LOG.info(String.format("Forwarding C-ECHO RQ to target association %s, %s, %s", targetAssociation,
                    arq.getId(), pc));
            DimseRSP rsp = targetAssociation.cecho();
            rsp.next();

            LOG.info(String.format("Serializing C-ECHO RSP object of target association %s, %s, %s", targetAssociation,
                    arq.getId(), pc));
            bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(rsp.getCommand());
            out.flush();

            return bos.toByteArray();

        } catch (Exception e) {
            LOG.error(String.format("Failed to forward C-ECHO RQ to target association %s, %s, %s", targetAssociation,
                    arq.getId(), pc));
            LOG.error(e.getMessage(), e);

            return new byte[0];
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    @Override
    public void onClose(SimpleAssociateRQ arq) throws Exception {
        // NOOP
    }
}
