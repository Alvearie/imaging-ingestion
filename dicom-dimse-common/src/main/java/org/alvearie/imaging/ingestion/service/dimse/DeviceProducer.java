/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;

@ApplicationScoped
public class DeviceProducer {
    @ConfigProperty(name = "dimse.target.host")
    String targetHost;

    @ConfigProperty(name = "dimse.target.port")
    Integer targetPort;

    @ConfigProperty(name = "dimse.target.device")
    String targetDeviceName;

    @ConfigProperty(name = "dimse.ae")
    String ae;

    @Inject
    ManagedExecutor executor;

    @Produces
    @ApplicationScoped
    public Device clientDevice() {
        Device device = new Device(targetDeviceName);

        Connection conn = new Connection();
        device.addConnection(conn);

        ApplicationEntity applicationEntity = new ApplicationEntity(ae);
        device.addApplicationEntity(applicationEntity);
        applicationEntity.addConnection(conn);

        device.setExecutor(executor);

        return device;
    }

    @Produces
    @ApplicationScoped
    public Connection remoteConnection() {
        Connection remote = new Connection();
        remote.setHostname(targetHost);
        remote.setPort(targetPort);

        return remote;
    }
}
