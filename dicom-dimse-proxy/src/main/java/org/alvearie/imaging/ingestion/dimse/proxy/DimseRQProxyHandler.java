/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.RequestWrapper;
import org.alvearie.imaging.ingestion.service.dimse.SimpleAssociateRQ;
import org.alvearie.imaging.ingestion.service.dimse.SimplePresentationContext;
import org.alvearie.imaging.ingestion.service.nats.Constants.NatsSubjectChannel;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.DimseRQHandler;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.pdu.AAbort;
import org.dcm4che3.net.pdu.PresentationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DimseRQProxyHandler implements DimseRQHandler {
    private static final Logger LOG = Logger.getLogger(DimseRQProxyHandler.class);

    @ConfigProperty(name = "dimse.nats.subject.root")
    String subjectRoot;

    @ConfigProperty(name = "dimse.nats.subject.channel")
    NatsSubjectChannel subjectChannel;

    @Inject
    NatsMessagePublisher messagePublisher;

    @Inject
    ActiveAssociationHolder holder;

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, PDVInputStream data)
            throws IOException {
        LOG.info(String.format("onDimseRQ %s, %s", as, pc));

        try {
            String subject = subjectRoot + "." + subjectChannel.getPublishChannel();
            Attributes dataAttributes = readDataset(pc, data);
            SimplePresentationContext spc = new SimplePresentationContext(pc.getPCID(), pc.getResult(),
                    pc.getAbstractSyntax(), pc.getTransferSyntaxes());

            String rqId = String.format("%s.%d", subject, as.getSerialNo());
            SimpleAssociateRQ rq = new SimpleAssociateRQ(rqId, as.getAAssociateRQ().getPresentationContexts());
            RequestWrapper request = new RequestWrapper(rq, spc, cmd, dataAttributes);

            int messageId = cmd.getInt(Tag.MessageID, 0);

            LOG.info(String.format("Publishing message to %s: %d", subject, messageId));
            byte[] bytes = messagePublisher.publish(subject, as, messageId, request.getBytes());

            if (bytes == null || bytes.length == 0) {
                LOG.error(String.format("No valid RSP received, aborting %s, %s", as, pc));
                throw new AAbort(AAbort.UL_SERIVE_USER, 0);
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;
            in = new ObjectInputStream(bis);

            LOG.info(String.format("De-serialize RSP %s: %d", subject, messageId));
            Attributes o = (Attributes) in.readObject();

            LOG.info(String.format("Write DIMSE RSP %s", pc));
            as.writeDimseRSP(pc, o);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new AAbort(AAbort.UL_SERIVE_USER, 0);
        }
    }

    private Attributes readDataset(PresentationContext pc, PDVInputStream data) throws IOException {
        if (data == null)
            return null;

        Attributes dataset = data.readDataset(pc.getTransferSyntax());
        Dimse.LOG.debug("Dataset:\n{}", dataset);
        return dataset;
    }

    @Override
    public void onClose(Association as) {
        LOG.info("Closing association: " + as);
        closeAssociation(as);

        String subject = subjectRoot + "." + subjectChannel.getPublishChannel();
        String key = subject + "." + as.getSerialNo();
        LOG.info("Remove association from holder: " + key);
        holder.removeAssociation(key);

        LOG.info("Remove subscriber from holder: " + key);
        holder.removeSubscriber(key);
    }

    private void closeAssociation(Association as) {
        if (as != null) {
            try {
                if (as.isReadyForDataTransfer()) {
                    as.release();
                }
            } catch (Exception e) {
                LOG.error(e);
            }
        }
    }
}
