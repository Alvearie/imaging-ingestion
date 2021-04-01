/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;

import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class WadoResourceTest {
    @InjectMock
    S3Service s3Service;

    @InjectMock
    RenderService renderService;

    @InjectMock
    @RestClient
    DicomQueryClient queryClient;

    @Test
    public void testRetrieveStudy() {
        Mockito.when(queryClient.getInstances(Mockito.anyString())).thenReturn(new ArrayList<>());

        given().log().all(true).get("/wado-rs/studies/123").then().statusCode(404);
    }
}
