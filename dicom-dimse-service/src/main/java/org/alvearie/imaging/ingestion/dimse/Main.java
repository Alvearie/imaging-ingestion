/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.nats.NatsAssociationSubscriber;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {
    private static final Logger LOG = Logger.getLogger(Main.class);

    @ConfigProperty(name = "dimse.nats.disabled")
    Boolean natsDisabled;

    @Inject
    DimseServer server;

    @Inject
    NatsAssociationSubscriber subscriber;

    @Override
    public int run(String... args) throws Exception {
        LOG.info("Application starting ...");

        server.init();

        if (!natsDisabled) {
            subscriber.subscribe();
        }

        Quarkus.waitForExit();
        return 0;
    }
}
