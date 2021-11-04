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
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.pdu.AAbort;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CStoreHandler implements DimseCommandHandler {
    private static final Logger LOG = Logger.getLogger(CStoreHandler.class);

    @ConfigProperty(name = "dimse.target.ae")
    String targetAe;

    @ConfigProperty(name = "dimse.ae")
    String ae;

    @Inject
    Device clientDevice;

    @Inject
    Connection remoteConnection;

    @Inject
    ActiveAssociationHolder holder;

    @Override
    public byte[] onDimseRQ(Dimse dimse, SimpleAssociateRQ arq, SimplePresentationContext pc, Attributes cmd,
            Attributes data) throws Exception {
        Association targetAssociation = null;
        ByteArrayOutputStream bos = null;
        try {
            String tsuid = pc.getTransferSyntax();
            String cuid = cmd.getString(Tag.AffectedSOPClassUID);
            String iuid = cmd.getString(Tag.AffectedSOPInstanceUID);
            int priority = cmd.getInt(Tag.Priority, 0);

            if (holder.isActiveAssociation(arq.getId())) {
                targetAssociation = holder.getAssociation(arq.getId());
            } else {
                AAssociateRQ rq = new AAssociateRQ();
                for (SimplePresentationContext spc : arq.getPresentationContexts()) {
                    rq.addPresentationContext(
                            new PresentationContext(spc.getPCID(), spc.getAbstractSyntax(), spc.getTransferSyntaxes()));
                }
                rq.setCalledAET(targetAe);

                targetAssociation = clientDevice.getApplicationEntity(ae).connect(remoteConnection, rq);
                holder.addAssociation(arq.getId(), targetAssociation);
            }
            DimseRSP rsp = targetAssociation.cstore(cuid, iuid, priority, new DataWriterAdapter(data), tsuid);
            rsp.next();

            bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(rsp.getCommand());
            out.flush();

            return bos.toByteArray();
        } catch (Exception e) {
            LOG.error(String.format("Failed to forward C-STORE RQ to %s: %s",
                    new Object[] { targetAssociation, e.getMessage() }));
            e.printStackTrace();
            throw new AAbort(AAbort.UL_SERIVE_USER, 0);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
