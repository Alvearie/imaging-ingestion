/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

public class TokenUtils {
    private static final Logger LOG = Logger.getLogger(TokenUtils.class);

    public static String getUser(String token) {
        String user = null;
        try {
            JwtConsumer consumer = new JwtConsumerBuilder().setSkipAllValidators().setDisableRequireSignature()
                    .setSkipSignatureVerification().build();
            JwtClaims claims = consumer.processToClaims(token);

            String sub = claims.getSubject();
            LOG.info("sub: " + sub);

            if (sub != null) {
                // If subject is a URI, use leaf as user
                // Token added by NATSServiceRole has a pattern of
                // system:serviceaccount:<namespace>:<name>
                if (sub.contains(":")) {
                    String[] fields = sub.split(":");
                    user = fields[fields.length - 1];
                } else {
                    user = sub;
                }
            }

        } catch (Exception e) {
            LOG.error("Error parsing user from token", e);
        }

        LOG.info("user: " + user);
        return user;
    }
}
