/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.ActiveAssociationHolder;
import org.alvearie.imaging.ingestion.service.dimse.CEchoHandler;
import org.alvearie.imaging.ingestion.service.dimse.Constants.Actor;
import org.alvearie.imaging.ingestion.service.dimse.DimseCommandRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

@ApplicationScoped
public class NatsAssociationSubscriber {
    private static final Logger LOG = Logger.getLogger(NatsAssociationSubscriber.class);

    @ConfigProperty(name = "dimse.nats.subject.root")
    String subjectRoot;

    @ConfigProperty(name = "dimse.proxy.actor")
    Actor actor;

    @Inject
    CEchoHandler echoHandler;

    @Inject
    DimseCommandRegistry commandRegistry;

    @Inject
    ActiveAssociationHolder holder;

    @Inject
    NatsConnectionFactory natsConnectionFactory;

    private Dispatcher dispatcher;
    private String subject;

    public void subscribe() {
        this.subject = subjectRoot + "." + actor.getDirection() + ".*";
        Connection connection = natsConnectionFactory.waitForConnection(0);
        if (connection != null) {

            dispatcher = connection.createDispatcher((msg) -> {
                onMessage(connection, msg);
            });
            dispatcher.subscribe(subject);

            LOG.info("Subscribed to " + subject);
        }
    }

    private void onMessage(Connection connection, Message msg) {
        String serialNumber = new String(msg.getData());
        LOG.info(subject + ": onMessage: " + serialNumber);

        String rootSubject = subjectRoot + "." + actor.getDirection() + "." + serialNumber;
        String messageSubject = rootSubject + ".>";
        NatsMessageSubscriber messageSubscriber = new NatsMessageSubscriber(connection, messageSubject,
                msg.getReplyTo(), commandRegistry);
        holder.addSubscriber(rootSubject, messageSubscriber);
        LOG.info("New message subscriber created for " + messageSubscriber.getSubject());
    }

    @PreDestroy
    public void close() {
        dispatcher.unsubscribe(subject);
    }
}
