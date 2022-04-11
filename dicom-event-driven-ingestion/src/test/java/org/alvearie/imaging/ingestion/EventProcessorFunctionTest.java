/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.event.DicomAvailableEvent;
import org.alvearie.imaging.ingestion.event.Element;
import org.alvearie.imaging.ingestion.event.Image;
import org.alvearie.imaging.ingestion.event.ImageStoredEvent;
import org.alvearie.imaging.ingestion.event.Store;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class EventProcessorFunctionTest {
    @InjectMock
    StudyManager studyManager;

    @Inject
    EventProcessorFunction processor;

    private static final String STUDY_ID = UUID.randomUUID().toString();
    private static final String SERIES_ID = UUID.randomUUID().toString();
    private static final String INSTANCE_ID = UUID.randomUUID().toString();

    @Test
    public void testImageStoredEventChain() {
        ImageStoredEvent data = createData();

        CloudEvent<ImageStoredEvent> event = Mockito.mock(CloudEvent.class);

        CloudEvent<DicomAvailableEvent> result = processor.imageStoredEventChain(data, event);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(String.format("%s/studies/%s/series/%s/instances/%s", "wadoInternalEndpoint", STUDY_ID,
                SERIES_ID, INSTANCE_ID), result.data().getEndpoint());
    }

    private ImageStoredEvent createData() {
        ImageStoredEvent data = new ImageStoredEvent();

        List<Element> elements = new ArrayList<>();

        elements.add(buildElement(Tag.StudyInstanceUID, STUDY_ID));
        elements.add(buildElement(Tag.SeriesInstanceUID, SERIES_ID));
        elements.add(buildElement(Tag.SOPInstanceUID, INSTANCE_ID));
        elements.add(buildElement(Tag.Modality, "CT"));

        Image image = new Image();
        image.setTransferSyntaxUID("tsuid");
        image.setElements(elements);

        data.setImage(image);

        Store store = new Store();
        store.setProvider("provider1");
        store.setWadoInternalEndpoint("wadoInternalEndpoint");
        store.setWadoExternalEndpoint("wadoExternalEndpoint");

        data.setStore(store);
        return data;
    }

    private Element buildElement(int tag, String value) {
        String ts = String.format("%08X", tag);

        Element elem = new Element();
        elem.setGroup(ts.substring(0, 4));
        elem.setElement(ts.substring(4));
        elem.setValue(value);

        return elem;
    }
}
