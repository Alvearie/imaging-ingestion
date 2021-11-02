/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.SSLManagerFactory;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;

public class ProxyEchoTLS {
    private final Device device = new Device("dimse-proxy");
    private Connection conn = new Connection();
    private ApplicationEntity ae = new ApplicationEntity("*");

    public ProxyEchoTLS() {
        device.setDimseRQHandler(createServiceRegistry());
        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.setAssociationAcceptor(true);
        ae.addConnection(conn);
    }

    public static void main(String[] args) throws Exception {
        String aet = "DIMSE";
        String host = "localhost";
        Integer port = 11112;

        String keyStoreURL = "src/test/resources/cert/dimse-proxy-scp-keystore.pkcs12";
        String keyStoreType = "PKCS12";
        String keyStorePass = "secret";
        String keyPass = keyStorePass;

        String trustStoreURL = "src/test/resources/cert/dimse-proxy-scu-truststore.pkcs12";
        String trustStoreType = "PKCS12";
        String trustStorePass = "secret";

        ProxyEchoTLS proxy = new ProxyEchoTLS();

        proxy.conn.setPort(port);
        proxy.conn.setHostname(host);
        proxy.conn.setTlsProtocols("TLSv1.2");
        proxy.conn.setTlsCipherSuites("SSL_RSA_WITH_NULL_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA");

        proxy.ae.setAETitle(aet);
        proxy.ae.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP, "*"));

        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        proxy.device.setScheduledExecutor(scheduledExecutorService);
        proxy.device.setExecutor(executorService);

        try {
            proxy.device.setKeyManager(
                    SSLManagerFactory.createKeyManager(keyStoreType, keyStoreURL, keyStorePass, keyPass));
            proxy.device.setTrustManager(
                    SSLManagerFactory.createTrustManager(trustStoreType, trustStoreURL, trustStorePass));
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        proxy.device.bindConnections();
    }

    private DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        return serviceRegistry;
    }
}
