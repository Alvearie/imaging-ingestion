/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.nats.NatsAssociationSubscriber;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {
    private static final Logger LOG = Logger.getLogger(Main.class);

    @Inject
    NatsAssociationSubscriber subscriber;

    @Inject
    ProxyServer server;

    @Override
    public int run(String... args) throws Exception {
        LOG.info("Application starting ...");

        server.init();
        subscriber.subscribe();

        Quarkus.waitForExit();
        return 0;
    }
}
