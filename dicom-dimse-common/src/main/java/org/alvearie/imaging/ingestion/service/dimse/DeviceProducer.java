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
    @ConfigProperty(name = "dimse.called.host")
    String calledHost;

    @ConfigProperty(name = "dimse.called.port")
    Integer calledPort;

    @ConfigProperty(name = "dimse.called.device")
    String calledDeviceName;

    @ConfigProperty(name = "dimse.calling.aet")
    String callingAet;

    @Inject
    ManagedExecutor executor;

    @Produces
    @ApplicationScoped
    public Device clientDevice() {
        Device device = new Device(calledDeviceName);

        Connection conn = new Connection();
        device.addConnection(conn);

        ApplicationEntity ae = new ApplicationEntity(callingAet);
        device.addApplicationEntity(ae);
        ae.addConnection(conn);

        device.setExecutor(executor);

        return device;
    }

    @Produces
    @ApplicationScoped
    public Connection remoteConnection() {
        Connection remote = new Connection();
        remote.setHostname(calledHost);
        remote.setPort(calledPort);

        return remote;
    }
}
