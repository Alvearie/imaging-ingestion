/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.samples;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class InstanceBindingFunction {
    private static final Logger log = Logger.getLogger(InstanceBindingFunction.class);
    public static final String DicomAvailableEvent = "DicomAvailableEvent";

    @Inject
    RestClientHelper clientHelper;

    @Funq
    @CloudEventMapping(trigger = DicomAvailableEvent)
    public void dicomAvailableEvent(String data, @Context CloudEvent<String> event) throws Exception {
        log.info("Received event: " + event.id());
        String file = retrieveDicom(data);
        processDicom(file);
    }

    private String retrieveDicom(String url) throws Exception {
        log.info("WADO URL: " + url);

        WadoClient client = clientHelper.getClient(url);
        Response response = client.getDicom();
        log.info(response.getStatus());
        log.info(response.getHeaders());

        MultipartInput input = response.readEntity(MultipartInput.class);
        File file = File.createTempFile("dicom", ".dcm");

        List<InputPart> parts = input.getParts();
        if (parts.size() > 0) {
            InputStream is = parts.get(0).getBody(new GenericType<>(InputStream.class));
            Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            IOUtils.closeQuietly(is);
            log.info("File copied to: " + file.getAbsolutePath());
        } else {
            throw new Exception("Invalid dicom data");
        }

        return file.getAbsolutePath();
    }

    private void processDicom(String path) throws Exception {
        InputStream is = new FileInputStream(new File(path));
        try (DicomInputStream dis = new DicomInputStream(is)) {
            Attributes attr = dis.readDatasetUntilPixelData();
            log.info(attr);
        }
    }
}
