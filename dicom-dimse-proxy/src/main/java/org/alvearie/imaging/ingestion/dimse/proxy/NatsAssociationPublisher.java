/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.Constants.Actor;
import org.alvearie.imaging.ingestion.service.nats.NatsConnectionFactory;
import org.dcm4che3.net.Association;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.Message;

@ApplicationScoped
public class NatsAssociationPublisher {
    private static final Logger LOG = Logger.getLogger(NatsAssociationPublisher.class);

    @ConfigProperty(name = "dimse.nats.subject.root")
    String subjectRoot;

    @ConfigProperty(name = "dimse.proxy.actor")
    Actor actor;

    @ConfigProperty(name = "dimse.nats.reply.timeoutSeconds")
    Integer replyTimeoutSeconds;

    @Inject
    NatsConnectionFactory natsConnectionFactory;

    public void onAssociation(Association as) {
        LOG.info(String.format("Publishing association %d", as.getSerialNo()));
        String subject = subjectRoot + "." + actor.getDirection() + "." + as.getSerialNo();

        try {
            Connection connection = natsConnectionFactory.getConnection();
            if (connection != null) {
                LOG.info(subject + ": onAssociation: " + as.getSerialNo());
                Future<Message> reply = connection.request(subject, Integer.toString(as.getSerialNo()).getBytes());
                reply.get(replyTimeoutSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
