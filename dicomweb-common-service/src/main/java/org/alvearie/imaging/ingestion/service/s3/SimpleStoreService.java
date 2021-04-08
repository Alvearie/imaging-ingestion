/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.event.Element;
import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.Image;
import org.alvearie.imaging.ingestion.event.ImageStoredEvent;
import org.alvearie.imaging.ingestion.event.Store;
import org.alvearie.imaging.ingestion.service.client.StudyStoredEventClient;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.dcm4che3.imageio.codec.Transcoder;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.service.DicomServiceException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimpleStoreService implements StoreService {
    private static final Logger LOG = Logger.getLogger(SimpleStoreService.class);

    @ConfigProperty(name = "provider.name", defaultValue = "")
    String providerName;

    @ConfigProperty(name = "wado.internal.endpoint", defaultValue = "")
    String wadoInternalEndpoint;

    @ConfigProperty(name = "wado.external.endpoint", defaultValue = "")
    String wadoExternalEndpoint;

    StoreConfiguration config;
    private PersistenceService persistenceService;

    @Inject
    @RestClient
    StudyStoredEventClient eventClient;

    @Inject
    public SimpleStoreService(StoreConfiguration config, Instance<PersistenceService> availablePersistenceServices) {
        this.config = config;
        LOG.info("Determining Storage Configuration");
        if (config.isLocalStorage()) {
            LOG.warn("Binding Local PersistenceService");
            persistenceService = availablePersistenceServices.select(LocalFileService.class).get();
        } else {
            LOG.info("Binding S3 PersistenceService");
            persistenceService = availablePersistenceServices.select(S3Service.class).get();
        }
    }

    @Override
    public void store(StoreContext ctx, InputStream data) throws IOException {
        writeToStorage(ctx, data);
    }

    private void writeToStorage(StoreContext ctx, InputStream data) throws DicomServiceException {
        try (Transcoder transcoder = new Transcoder(data)) {
            transcoder.transcode(new TranscoderHandler(ctx));
            persistenceService.putObject(ctx);
            eventClient.sendEvent(UUID.randomUUID().toString(), Events.ImageStoredEvent, buildEvent(ctx));
        } catch (Throwable e) {
            LOG.warn("Failed to store received object:\n", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
    }

    private ImageStoredEvent buildEvent(StoreContext ctx) {
        List<Element> elements = new ArrayList<>();
        Attributes attrs = ctx.getAttributes();
        try {
            attrs.accept(new Attributes.Visitor() {
                @Override
                public boolean visit(Attributes attrs, int tag, VR vr, Object value) throws Exception {
                    Element elem = getElement(attrs, tag, vr, value);
                    elements.add(elem);
                    return true;
                }
            }, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Image image = new Image();
        image.setElements(elements);

        Store store = new Store();
        store.setProvider(providerName);
        store.setBucketName(config.getBucketName());
        store.setObjectName(ctx.getObjectName());
        store.setWadoInternalEndpoint(wadoInternalEndpoint);
        store.setWadoExternalEndpoint(wadoExternalEndpoint);

        ImageStoredEvent event = new ImageStoredEvent();
        event.setImage(image);
        event.setStore(store);

        return event;
    }

    private final class TranscoderHandler implements Transcoder.Handler {
        private final StoreContext storeContext;

        private TranscoderHandler(StoreContext storeContext) {
            this.storeContext = storeContext;
        }

        @Override
        public OutputStream newOutputStream(Transcoder transcoder, Attributes dataset) throws IOException {
            storeContext.setAttributes(dataset);
            File file = File.createTempFile("stow", ".tmp");
            storeContext.setFilePath(file.getAbsolutePath());
            return new FileOutputStream(file);
        }
    }

    private Element getElement(Attributes attrs, int tag, VR vr, Object value) {
        String ts = String.format("%08X", tag);
        Element elem = new Element();
        elem.setGroup(ts.substring(0, 4));
        elem.setElement(ts.substring(4));
        elem.setName(vr.toString());
        elem.setValue(attrs.getString(tag));

        LOG.debugf("(%s, %s) %s %s", elem.getGroup(), elem.getElement(), elem.getName(), elem.getValue());

        return elem;
    }
}