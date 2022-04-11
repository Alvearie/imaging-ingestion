/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.cstore.binding;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.alvearie.imaging.ingestion.event.DicomAvailableEvent;
import org.alvearie.imaging.ingestion.event.Events;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class InstanceBindingFunction {
    private static final Logger log = Logger.getLogger(InstanceBindingFunction.class);

    @ConfigProperty(name = "selector")
    Optional<String> selector;

    @Inject
    RestClientHelper clientHelper;

    @Inject
    CStoreSCU storescu;

    List<String> selectorPatterns = new ArrayList<>();

    @PostConstruct
    private void init() {
        if (selector != null && selector.isPresent()) {
            String[] parts = selector.get().split(",");
            for (String p : parts) {
                p = p.trim();
                if (p.length() > 0) {
                    selectorPatterns.add(p);
                }
            }
        }
    }

    private boolean isFilteredEvent(String s) {
        if (selectorPatterns.size() == 0) {
            return false;
        }

        for (String p : selectorPatterns) {
            if (Pattern.matches(p, s)) {
                return false;
            }
        }

        return true;
    }

    @Funq
    @CloudEventMapping(trigger = Events.DicomAvailableEvent)
    public void bindingEvent(DicomAvailableEvent data, @Context CloudEvent<DicomAvailableEvent> event)
            throws Exception {
        log.info(String.format("Event received - id: %s, source: %s", event.id(), event.source()));
        if (data.getProvider() == null || isFilteredEvent(data.getProvider())) {
            log.info(String.format("Filtered event - id: %s, source: %s, provider: %s", event.id(), event.source(),
                    data.getProvider()));
            return;
        }

        String file = retrieveDicom(data);
        processDicom(file);
    }

    private String retrieveDicom(DicomAvailableEvent data) throws Exception {
        log.info("WADO URL: " + data.getEndpoint());

        WadoClient client = clientHelper.getClient(data.getEndpoint());
        Response response = client.getDicom();
        log.info("Response Status: " + response.getStatus());
        log.info("Response Headers: " + response.getHeaders());

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
        storescu.cstore(path);
    }
}
