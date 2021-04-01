/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;

@ApplicationScoped
public class NatsConnectionFactory {
    private static final Logger LOG = Logger.getLogger(NatsConnectionFactory.class);
    
    @Inject
    NatsConfiguration configuration;
    
    Connection connection;
    
    Object lock = new Object();
    
    @PostConstruct
    public void natsConnection() throws IOException, InterruptedException {   
        LOG.info("Connecting to NATS cluster at " + configuration.getNatsUrl());
        Options.Builder optionsBuilder = new Options.Builder().server(configuration.getNatsUrl());
        optionsBuilder.maxReconnects(-1);
        if (configuration.isTlsEnabled()) {
            LOG.info("Performing NATS TLS connection with OpenTLS (trust all)");
            try {
                // Trust all certificates/issuers
                optionsBuilder.opentls();
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Unexpected error, unable to perform TLS connection to NATS due to missing JVM Suppoprt");
                throw new IOException("TLS is unavailable", e);
            }
        } 
        
        String token = configuration.getToken();
        if (token != null && !token.isBlank()) {
            LOG.info("Using identify token provided by dimse.nats.auth.token property");
            optionsBuilder.token(token.toCharArray());
        }
        synchronized(lock) {
            optionsBuilder.connectionListener(new ConnectionListener() {
                @Override
                public void connectionEvent(Connection conn, Events type) {
                    LOG.info("NATS connection event " + type.name());
                    synchronized(lock) {
                        switch (type) {
                        case CONNECTED: 
                        case RECONNECTED:
                            connection = conn;
                            lock.notifyAll();
                        default:
                            break;
                        }
                    }
                }
            });
            Nats.connectAsynchronously(optionsBuilder.build(), true);
            try {
                lock.wait(5000);
                if (connection == null) {
                    LOG.error("Could not get NATS connection, connection may be null");
                } else {
                    LOG.info("NATS Connection created");
                }
                
            } catch (InterruptedException e) {
                LOG.error("Could not get NATS connection, connection may be null");
            }
        }
    }
    
    public Connection getConnection() {
        return connection;   
    }
    
    public Connection waitForConnection(long timeout) {
        synchronized (lock) {
            if (connection == null) {
                try {
                    lock.wait(timeout);
                    if (connection == null) {
                        LOG.error("Timed out waiting for NATS connection, connection is null");
                    } else {
                        LOG.info("NATS Connection created");
                    }
                } catch (InterruptedException e) {
                    LOG.error("Interrupted waiting for NATS connection, connection is null");
                }
            }
        }
        return connection;
    }
}
