/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import static io.restassured.RestAssured.given;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomResource;
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

    private static final String TEST_FILENAME = "../test-data/dicom/file1.dcm";

    RenderService renderService;

    @InjectMock
    @RestClient
    DicomQueryClient queryClient;

    @Test
    public void testRetrieveStudy() {
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        given().log().all(true).get("/wado-rs/studies/123").then().statusCode(404);
    }

    @Test
    public void testBasicRender() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/rendered").then().log().headers()
                .statusCode(200);
    }

    @Test
    public void testThumbnail() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/thumbnail").then().log().headers()
                .statusCode(200);
    }

    @Test
    public void testScaledThumbnail() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/thumbnail?viewport=75,100").then()
                .log().headers().statusCode(200);
    }

    @Test
    public void testBasicViewport() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=200,200").then()
                .log().headers().statusCode(200);
    }

    @Test
    public void testTopLeftViewportRegion() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true)
                .get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=200,200,,,200,200").then()
                .log().headers().statusCode(200);
    }

    @Test
    public void testBottomRightViewportRegion() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true)
                .get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=256,256,256,256").then().log()
                .headers().statusCode(200);
    }

    @Test
    public void testRetrieveContentEncoding() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).header("Accept-Encoding", "gzip")
                .get("/wado-rs/studies/123/series/1234/instances/12345").then().log().headers().statusCode(200).and()
                .header("Content-Encoding", "gzip");
    }

    @Test
    public void testMetadataContentEncoding() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).header("Accept-Encoding", "gzip")
                .get("/wado-rs/studies/123/series/1234/instances/12345/metadata").then().log().headers().statusCode(200)
                .and().header("Content-Encoding", "gzip");
    }

    private List<DicomEntityResult> getResults(String objectName) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomEntityResult result = new DicomEntityResult();

        DicomResource resource = new DicomResource();
        resource.setObjectName(objectName);

        result.setResource(resource);
        results.add(result);

        return results;
    }

    private ByteArrayOutputStream getObject(String filename) {
        try {
            FileInputStream fis = new FileInputStream(new File(filename));
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            result.write(fis.readAllBytes());
            return result;
        } catch (IOException e) {

        }
        return null;
    }
}
