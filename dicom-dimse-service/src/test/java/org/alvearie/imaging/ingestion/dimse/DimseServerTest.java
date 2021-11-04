/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.DeviceConfiguration;
import org.alvearie.imaging.ingestion.service.dimse.Utils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class DimseServerTest {
    @ConfigProperty(name = "dimse.port")
    Integer port;

    @InjectMock
    DeviceConfiguration deviceConfiguration;

    @Inject
    DimseServer server;

    @Test
    public void testServer() {
        server.init();
        Assertions.assertTrue(Utils.isServerListening("localhost", port));
        server.destroy();
    }
}
