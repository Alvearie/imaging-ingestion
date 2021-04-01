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
import org.eclipse.microprofile.rest.client.inject.RestClient;
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

    @InjectMock
    @RestClient
    StudyRevisionEventClient eventClient;

    @Inject
    EventProcessorFunction processor;

    @Test
    public void testImageStoredEventChain() {
        ImageStoredEvent data = createData();

        CloudEvent event = Mockito.mock(CloudEvent.class);

        String resource = processor.imageStoredEventChain(data, event);
        Assertions.assertNotNull(resource);
    }

    private ImageStoredEvent createData() {
        ImageStoredEvent data = new ImageStoredEvent();

        List<Element> elements = new ArrayList<>();

        Element element = new Element();
        element.setGroup(DicomConstants.STUDY_INSTANCE_UID_GROUP);
        element.setElement(DicomConstants.STUDY_INSTANCE_UID_ELEMENT);
        element.setValue("study1");
        elements.add(element);

        element = new Element();
        element.setGroup(DicomConstants.SERIES_INSTANCE_UID_GROUP);
        element.setElement(DicomConstants.SERIES_INSTANCE_UID_ELEMENT);
        element.setValue("series1");
        elements.add(element);

        element = new Element();
        element.setGroup(DicomConstants.SOP_INSTANCE_UID_GROUP);
        element.setElement(DicomConstants.SOP_INSTANCE_UID_ELEMENT);
        element.setValue("instance1");
        elements.add(element);

        element = new Element();
        element.setGroup(DicomConstants.MODALITY_GROUP);
        element.setElement(DicomConstants.MODALITY_ELEMENT);
        element.setValue("CT");
        elements.add(element);

        Image image = new Image();
        image.setElements(elements);

        data.setImage(image);

        Store store = new Store();
        store.setProvider("provider1");
        store.setWadoInternalEndpoint("wadoInternalEndpoint");
        store.setWadoExternalEndpoint("wadoExternalEndpoint");

        data.setStore(store);
        return data;
    }
}
