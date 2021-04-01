/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.DeviceConfiguration;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DimseServer {
    private static final Logger LOG = Logger.getLogger(DimseServer.class);

    @ConfigProperty(name = "dimse.ingestion.host")
    String dimseHost;

    @ConfigProperty(name = "dimse.ingestion.port")
    Integer dimsePort;

    @ConfigProperty(name = "dimse.ingestion.aet")
    String aet;

    @ConfigProperty(name = "dimse.ingestion.device")
    String deviceName;

    @Inject
    ManagedExecutor executor;

    @Inject
    DeviceConfiguration deviceConfiguration;

    @Inject
    DicomServiceRegistry serviceRegistry;

    @Inject
    CEchoSCPImpl echoscp;

    @Inject
    CStoreSCPImpl storescp;

    AssociationHandler associationHandler = new AssociationHandler();

    Device device;

    public void init() {
        LOG.info("Initializing Server");

        try {
            device = createDevice();
            device.setExecutor(executor);
            device.setAssociationHandler(associationHandler);
            serviceRegistry.addDicomService(echoscp);
            serviceRegistry.addDicomService(storescp);
            device.setDimseRQHandler(serviceRegistry);

            start();
        } catch (RuntimeException re) {
            LOG.error(re.getMessage(), re);
            destroy();
            throw re;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            destroy();
            throw new RuntimeException(e);
        }

        LOG.info("Server Initialized");
    }

    @PreDestroy
    public void destroy() {
        LOG.info("Shutting down Server");
        stop();
    }

    public void start() throws Exception {
        device.bindConnections();
    }

    public void stop() {
        device.unbindConnections();
    }

    private Device createDevice() {
        Connection dicom = new Connection("dicom", dimseHost, dimsePort);

        Device device = new Device(deviceName);

        ApplicationEntity ae = new ApplicationEntity(aet);
        ae.setAssociationAcceptor(true);
        ae.setAssociationInitiator(true);

        addTCs(ae);

        device.addApplicationEntity(ae);
        device.addConnection(dicom);
        ae.addConnection(dicom);

        return device;
    }

    private void addTCs(ApplicationEntity ae) {
        addTC(ae, TransferCapability.Role.SCP, UID.Verification, UID.ImplicitVRLittleEndian);
        String[] tsuids = deviceConfiguration.getImageTsuids().toArray(new String[] {});
        for (String cuid : deviceConfiguration.getImageCuids()) {
            LOG.info("Adding TC " + cuid);
            addTC(ae, TransferCapability.Role.SCP, cuid, tsuids);
        }
    }

    private void addTC(ApplicationEntity ae, TransferCapability.Role role, String cuid, String... tss) {
        String name = UID.nameOf(cuid).replace('/', ' ');
        TransferCapability tc = new TransferCapability(name + ' ' + role, cuid, role, tss);
        ae.addTransferCapability(tc);
    }
}
