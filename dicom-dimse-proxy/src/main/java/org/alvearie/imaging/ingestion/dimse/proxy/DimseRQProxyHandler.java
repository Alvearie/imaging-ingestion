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

import org.alvearie.imaging.ingestion.service.dimse.Constants.Actor;
import org.alvearie.imaging.ingestion.service.dimse.RequestWrapper;
import org.alvearie.imaging.ingestion.service.dimse.SimpleAssociateRQ;
import org.alvearie.imaging.ingestion.service.dimse.SimplePresentationContext;
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

    @ConfigProperty(name = "dimse.proxy.actor")
    Actor actor;

    @Inject
    NatsMessagePublisher messagePublisher;

    @Inject
    ActiveAssociationHolder holder;

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, PDVInputStream data)
            throws IOException {
        LOG.info("onDimseRQ");

        try {
            String subject = subjectRoot + "." + actor.getPublishDirection();
            Attributes dataAttributes = readDataset(pc, data);
            SimplePresentationContext spc = new SimplePresentationContext(pc.getPCID(), pc.getResult(),
                    pc.getAbstractSyntax(), pc.getTransferSyntaxes());

            String rqId = String.format("%s.%d", subject, as.getSerialNo());
            SimpleAssociateRQ rq = new SimpleAssociateRQ(rqId, as.getAAssociateRQ().getPresentationContexts());
            RequestWrapper request = new RequestWrapper(rq, spc, cmd, dataAttributes);

            int messageId = cmd.getInt(Tag.MessageID, 0);
            byte[] bytes = messagePublisher.publish(subject, as, messageId, request.getBytes());

            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;
            in = new ObjectInputStream(bis);
            Attributes o = (Attributes) in.readObject();
            as.writeDimseRSP(pc, o);
        } catch (Exception e) {
            e.printStackTrace();
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
        String subject = subjectRoot + "." + actor.getPublishDirection();
        String key = subject + "." + as.getSerialNo();
        LOG.info("Remove association from holder: " + key);
        holder.removeAssociation(key);

        LOG.info("Remove subscriber from holder: " + key);
        holder.removeSubscriber(key);
    }
}
