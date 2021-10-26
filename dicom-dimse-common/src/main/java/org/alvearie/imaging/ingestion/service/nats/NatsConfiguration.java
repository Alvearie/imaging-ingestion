/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class NatsConfiguration {

    @ConfigProperty(name = "dimse.nats.url")
    String natsUrl;

    @ConfigProperty(name = "dimse.nats.tls.enabled")
    Boolean tlsEnabled;

    @ConfigProperty(name = "dimse.nats.traceConnection")
    Boolean traceConnection;

    @ConfigProperty(name = "dimse.nats.maxControlLine")
    Integer maxControlLine;

    @ConfigProperty(name = "dimse.nats.auth.token")
    Optional<String> token;

    @ConfigProperty(name = "dimse.nats.subject.root")
    String natsSubjectRoot;

    public String getNatsUrl() {
        return natsUrl;
    }

    public Integer getMaxControlLine() {
        return maxControlLine;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public boolean isTraceConnection() {
        return traceConnection;
    }

    public String getToken() {
        try {
            return token.get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getNatsSubjectRoot() {
        return natsSubjectRoot;
    }
}
