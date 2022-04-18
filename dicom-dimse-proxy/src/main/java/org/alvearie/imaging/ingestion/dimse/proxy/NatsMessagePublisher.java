/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.Utils;
import org.alvearie.imaging.ingestion.service.nats.NatsConnectionFactory;
import org.dcm4che3.net.Association;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.Message;

@ApplicationScoped
public class NatsMessagePublisher {
    private static final Logger LOG = Logger.getLogger(NatsMessagePublisher.class);

    @ConfigProperty(name = "dimse.nats.reply.timeoutSeconds")
    Integer replyTimeoutSeconds;

    @ConfigProperty(name = "dimse.nats.chunk.size")
    Integer chunkSize;

    @Inject
    NatsConnectionFactory natsConnectionFactory;

    public byte[] publish(String aet, Association as, int messageId, byte[] data) {
        String subject = String.format("%s.%d.%d", aet, as.getSerialNo(), messageId);
        LOG.info("Publishing to " + subject);
        Connection connection = natsConnectionFactory.getConnection();
        if (connection != null) {
            if (data == null) {
                return eof(connection, subject + ".0", data);
            } else {
                LOG.info("Bytes len: " + data.length);
                LOG.info("Max Size: " + connection.getMaxPayload());
                List<byte[]> ar = Utils.divideArray(data, chunkSize);
                LOG.info("Parts: " + ar.size());
                String partSubject;
                for (int i = 0; i < ar.size() - 1; i++) {
                    partSubject = subject + "." + i;
                    LOG.info("Publishing to " + partSubject);
                    connection.publish(partSubject, ar.get(i));
                }
                return eof(connection, subject + "." + (ar.size() - 1), ar.get(ar.size() - 1));
            }

        } else {
            return new byte[0];
        }

    }

    public void release(String aet, Association as) {
        Connection connection = natsConnectionFactory.getConnection();
        if (connection != null) {
            String subject = String.format("%s.%d.RELEASE", aet, as.getSerialNo());
            LOG.info("Publishing RELEASE to " + subject);
            connection.publish(subject, null);
        }
    }

    private byte[] eof(Connection connection, String subject, byte[] data) {
        String eofSubject = subject + ".EOF";

        LOG.info("Publishing to " + eofSubject);
        LOG.info("EOF data size " + data.length);

        Future<Message> reply = connection.request(eofSubject, data);
        try {
            Message msg = reply.get(replyTimeoutSeconds, TimeUnit.SECONDS);
            return msg.getData();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
