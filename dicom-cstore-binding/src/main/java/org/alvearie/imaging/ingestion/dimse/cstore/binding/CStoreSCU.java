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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

    @Inject
    ManagedExecutor executor;

    public interface RSPHandlerFactory {
        DimseRSPHandler createDimseRSPHandler(File f);
    }

    private Association openAssociation(String cuid, String ts)
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        Device device = new Device(deviceName);
        Connection conn = new Connection();
        device.addConnection(conn);

        ApplicationEntity ae = new ApplicationEntity(aet);

        device.addApplicationEntity(ae);
        ae.addConnection(conn);
        device.setExecutor(executor);

        AAssociateRQ rq = new AAssociateRQ();
        addPresentationContexts(rq, cuid, ts);

        rq.setCalledAET(targetAe);

        Connection remote = new Connection();
        remote.setHostname(targetHost);
        remote.setPort(targetPort);

        Association as = ae.connect(remote, rq);

        return as;
    }

    private void closeAssociation(Association as) throws IOException, InterruptedException {
        if (as != null) {
            if (as.isReadyForDataTransfer()) {
                as.release();
            }
            as.waitForSocketClose();
        }
    }

    public void cecho()
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        Association as = openAssociation(null, null);
        as.cecho().next();
        closeAssociation(as);
    }

    public void cstore(String path)
            throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {

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

        final Association as = openAssociation(cuid, ts);

        try {
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
        } finally {
            closeAssociation(as);
        }
    }

    private void addPresentationContexts(AAssociateRQ rq, String cuid, String ts) {
        rq.addPresentationContext(new PresentationContext(1, UID.Verification, UID.ImplicitVRLittleEndian));
        if (cuid != null && ts != null) {
            if (rq.containsPresentationContextFor(cuid, ts)) {
                return;
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

            rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid, ts));
        }
    }
}
