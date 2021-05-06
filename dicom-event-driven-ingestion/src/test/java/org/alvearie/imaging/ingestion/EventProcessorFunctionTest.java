/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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

    @Test
    public void testImageStoredEventChain() {
        ImageStoredEvent data = createData();

        CloudEvent event = Mockito.mock(CloudEvent.class);

        String resource = processor.imageStoredEventChain(data, event);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals(String.format("%s/studies/%s/series/%s/instances/%s", "wadoInternalEndpoint", "study1",
                "series1", "instance1"), resource);
    }

    private ImageStoredEvent createData() {
        ImageStoredEvent data = new ImageStoredEvent();

        List<Element> elements = new ArrayList<>();

        elements.add(buildElement(Tag.StudyInstanceUID, "study1"));
        elements.add(buildElement(Tag.SeriesInstanceUID, "series1"));
        elements.add(buildElement(Tag.SOPInstanceUID, "instance1"));
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
