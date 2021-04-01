/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.service.client.StudyStoredEventClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class SimpleStoreServiceTest {
    @Inject
    SimpleStoreService storeService;

    @InjectMock
    StoreConfiguration storeConfiguration;

    @InjectMock
    S3Service persistenceService;

    @InjectMock
    @RestClient
    StudyStoredEventClient eventClient;

    @Test
    public void testStore() throws IOException, NoSuchAlgorithmException {
        Mockito.spy(persistenceService);
        Mockito.spy(eventClient);

        InputStream data = new FileInputStream(new File("../test-data/dicom/file1.dcm"));
        StoreContext ctx = new SimpleStoreContext();

        storeService.store(ctx, data);

        Mockito.verify(persistenceService, Mockito.times(1)).putObject(ctx);
        Mockito.verify(eventClient, Mockito.times(1)).sendEvent(Mockito.anyString(), Mockito.anyString(),
                Mockito.any());
    }
}
