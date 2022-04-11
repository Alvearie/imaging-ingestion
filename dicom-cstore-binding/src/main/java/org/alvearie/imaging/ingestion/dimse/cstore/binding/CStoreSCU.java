/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.cstore.binding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.dimse.DeviceConfiguration;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.InputStreamDataWriter;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.util.SafeClose;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class CStoreSCU {
    private static final Logger LOG = Logger.getLogger(CStoreSCU.class);

    @ConfigProperty(name = "dimse.ae")
    String aet;

    @ConfigProperty(name = "dimse.device")
    String deviceName;

    @ConfigProperty(name = "dimse.target.host")
    String targetHost;

    @ConfigProperty(name = "dimse.target.port")
    Integer targetPort;

    @ConfigProperty(name = "dimse.target.ae")
    String targetAe;

    @ConfigProperty(name = "dimse.target.device")
    String targetDevice;

    @ConfigProperty(name = "cstore.aggregation.timeoutSeconds")
    Integer timeout;

    @Inject
    DeviceConfiguration deviceConfiguration;

    @Inject
    ManagedExecutor executor;

    private String[] tsuids;
    private String[] cuids;
    private AssociationContext asCtx;
    private Object lock = new Object();

    @PostConstruct
    private void init() {
        tsuids = deviceConfiguration.getImageTsuids().toArray(new String[] {});
        cuids = deviceConfiguration.getImageCuids().toArray(new String[] {});
    }

    public interface RSPHandlerFactory {
        DimseRSPHandler createDimseRSPHandler(File f);
    }

    private Association openAssociation()
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        Device device = new Device(deviceName);
        Connection conn = new Connection();
        device.addConnection(conn);

        ApplicationEntity ae = new ApplicationEntity(aet);

        device.addApplicationEntity(ae);
        ae.addConnection(conn);
        device.setExecutor(executor);

        AAssociateRQ rq = new AAssociateRQ();
        addPresentationContexts(rq);

        rq.setCalledAET(targetAe);

        Connection remote = new Connection();
        remote.setHostname(targetHost);
        remote.setPort(targetPort);

        Association as = ae.connect(remote, rq);

        return as;
    }

    private void closeAssociation(Association as) throws IOException, InterruptedException {
        synchronized (lock) {
            this.asCtx = null;
        }
        if (as != null) {
            if (as.isReadyForDataTransfer()) {
                as.release();
            }
            as.waitForSocketClose();
        }
    }

    public void cecho()
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        Association as = openAssociation();
        as.cecho().next();
        closeAssociation(as);
    }

    public void cstore(String path)
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {

        final Association as;
        synchronized (lock) {
            if (this.asCtx != null && this.asCtx.getAssociation() != null
                    && this.asCtx.getAssociation().isReadyForDataTransfer()) {
                LOG.info("Re-using existing association");
                as = this.asCtx.getAssociation();
                this.asCtx.markLastUpdated();
            } else {
                LOG.info("Creating new association");
                as = openAssociation();
                this.asCtx = new AssociationContext(as);
            }
        }

        File file = new File(path);

        DicomInputStream dis = null;
        Attributes fmi = null;
        long dsPos = -1;
        try {
            dis = new DicomInputStream(file);
            dis.setIncludeBulkData(IncludeBulkData.NO);
            fmi = dis.readFileMetaInformation();
            dsPos = dis.getPosition();
            Attributes ds = dis.readDatasetUntilPixelData();
            if (fmi == null || !fmi.containsValue(Tag.TransferSyntaxUID)
                    || !fmi.containsValue(Tag.MediaStorageSOPClassUID)
                    || !fmi.containsValue(Tag.MediaStorageSOPInstanceUID)) {
                fmi = ds.createFileMetaInformation(dis.getTransferSyntax());
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            SafeClose.close(dis);
        }

        String cuid = fmi.getString(Tag.MediaStorageSOPClassUID);
        String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID);
        String ts = fmi.getString(Tag.TransferSyntaxUID);

        LOG.info("CUID: " + cuid);
        LOG.info("Instance UID: " + iuid);
        LOG.info("Transfer Syntax: " + ts);

        FileInputStream fin = new FileInputStream(path);
        try {
            fin.skip(dsPos);
            InputStreamDataWriter data = new InputStreamDataWriter(fin);
            as.cstore(cuid, iuid, Priority.NORMAL, data, ts, new RSPHandlerFactory() {
                @Override
                public DimseRSPHandler createDimseRSPHandler(File f) {
                    return new DimseRSPHandler(as.nextMessageID()) {
                        @Override
                        public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
                            super.onDimseRSP(as, cmd, data);
                            LOG.info("RSP Status for: " + f.getName() + ": " + cmd.getInt(Tag.Status, -1));
                        }
                    };
                }
            }.createDimseRSPHandler(file));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            SafeClose.close(fin);
        }
    }

    private void addPresentationContexts(AAssociateRQ rq) {
        rq.addPresentationContext(new PresentationContext(1, UID.Verification, UID.ImplicitVRLittleEndian));
        for (String cuid : cuids) {
            LOG.debug("Adding Presentation Context for " + cuid);

            for (String ts : tsuids) {
                if (rq.containsPresentationContextFor(cuid, ts)) {
                    continue;
                }

                if (!rq.containsPresentationContextFor(cuid)) {
                    if (!ts.equals(UID.ExplicitVRLittleEndian)) {
                        rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1,
                                cuid, UID.ExplicitVRLittleEndian));
                    }
                    if (!ts.equals(UID.ImplicitVRLittleEndian)) {
                        rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1,
                                cuid, UID.ImplicitVRLittleEndian));
                    }
                }

                rq.addPresentationContext(
                        new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid, ts));
            }
        }
    }

    @Scheduled(every = "{cstore.aggregation.schedule}")
    void schedule() {
        LOG.debug("Scheduled cleanup triggered");
        Association as = null;
        synchronized (lock) {
            if (this.asCtx != null && this.asCtx.getAssociation() != null && this.asCtx.getLastUpdated() != null) {
                long durationSeconds = Duration.between(this.asCtx.getLastUpdated(), OffsetDateTime.now(ZoneOffset.UTC))
                        .toSeconds();
                if (durationSeconds > timeout) {
                    as = this.asCtx.getAssociation();
                }
            }
        }

        if (as != null) {
            LOG.info("Closing association: " + as);
            try {
                closeAssociation(as);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }
    }
}
