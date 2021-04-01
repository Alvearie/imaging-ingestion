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
    
    @ConfigProperty(name = "dimse.nats.auth.token")
    Optional<String> token;
    
    
    public String getNatsUrl() {
        return natsUrl;
    }
    
    public boolean isTlsEnabled() {
        return tlsEnabled;
    }
    
    public String getToken() {
        try {
            return token.get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    
}
