/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

import java.util.ArrayList;
import java.util.List;

import org.alvearie.imaging.ingestion.service.dimse.DimseCommandRegistry;
import org.alvearie.imaging.ingestion.service.dimse.RequestWrapper;
import org.alvearie.imaging.ingestion.service.dimse.SimpleAssociateRQ;
import org.alvearie.imaging.ingestion.service.dimse.Utils;
import org.dcm4che3.net.Dimse;
import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

public class NatsMessageSubscriber {
    private static final Logger LOG = Logger.getLogger(NatsMessageSubscriber.class);

    private Connection connection;
    private Dispatcher dispatcher;
    private String subject;
    private DimseCommandRegistry commandRegistry;
    private List<Message> messages = new ArrayList<>();
    private SimpleAssociateRQ arq;

    public NatsMessageSubscriber(Connection connection, String subject, String replyTo,
            DimseCommandRegistry commandRegistry) {
        this.connection = connection;
        this.subject = subject;
        this.commandRegistry = commandRegistry;

        dispatcher = connection.createDispatcher((msg) -> {
            onMessage(msg);
        });
        dispatcher.subscribe(subject);

        LOG.info("Subscribed to " + subject);
        LOG.info("Replying to " + replyTo);
        connection.publish(replyTo, null);
    }

    public void onMessage(Message msg) {
        LOG.info(msg.getSubject() + ":" + msg.getSID());
        messages.add(msg);

        if (msg.getSubject().endsWith(".EOF")) {
            LOG.info("EOF received");
            reply(msg);
        } else if (msg.getSubject().endsWith(".RELEASE")) {
            LOG.info("RELEASE received");
            try {
                LOG.info("Release target association");
                commandRegistry.onClose(this.arq);
            } catch (Exception e) {
                LOG.error("Error releasing target association", e);
            }
        }
    }

    private void reply(Message msg) {
        try {
            verify(msg);

            LOG.info("Message parts: " + messages.size());

            RequestWrapper req = new RequestWrapper(Utils.combineData(messages));
            Dimse dimse = Utils.getDimse(req.getCmd());

            this.arq = req.getAssociateRQ();

            byte[] data = commandRegistry.onDimseRQ(dimse, arq, req.getPresentationContext(), req.getCmd(),
                    req.getData());
            LOG.info("Replying to " + msg.getReplyTo());
            connection.publish(msg.getReplyTo(), data);

            messages.clear();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean verify(Message msg) {
        String[] sp = msg.getSubject().split("\\.");
        if (sp != null && sp.length == 4) {
            int parts = Integer.parseInt(sp[3]);
            if (messages.size() == parts + 1) {
                return true;
            }
        }
        return false;
    }

    public void close() {
        LOG.info("Unsubscribing " + subject);
        dispatcher.unsubscribe(subject);
    }

    public String getSubject() {
        return subject;
    }
}
