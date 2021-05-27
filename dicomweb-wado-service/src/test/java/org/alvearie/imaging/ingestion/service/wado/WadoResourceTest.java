/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomResource;
import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.ws.rs.MediaTypes;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.response.Response;

@QuarkusTest
public class WadoResourceTest {
    @InjectMock
    S3Service s3Service;

    private static final String TEST_FILENAME = "../test-data/dicom/file1.dcm";

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
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/rendered").then().log().headers()
                .statusCode(501);
    }

    @Test
    public void testThumbnail() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/thumbnail").then().log().headers()
                .statusCode(501);
    }

    @Test
    public void testScaledThumbnail() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/thumbnail?viewport=75,100").then()
                .log().headers().statusCode(501);
    }

    @Test
    public void testBasicViewport() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=200,200").then()
                .log().headers().statusCode(501);
    }

    @Test
    public void testTopLeftViewportRegion() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true)
                .get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=200,200,,,200,200").then()
                .log().headers().statusCode(501);
    }

    @Test
    public void testBottomRightViewportRegion() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true)
                .get("/wado-rs/studies/123/series/1234/instances/12345/rendered?viewport=256,256,256,256").then().log()
                .headers().statusCode(501);
    }

    @Test
    public void testRetrieveContentEncoding() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).header("Accept-Encoding", "gzip")
                .get("/wado-rs/studies/123/series/1234/instances/12345").then().log().headers().statusCode(200).and()
                .header("Content-Encoding", "gzip");
    }

    @Test
    public void testMetadataContentEncoding() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).header("Accept-Encoding", "gzip")
                .get("/wado-rs/studies/123/series/1234/instances/12345/metadata").then().log().headers().statusCode(200)
                .and().header("Content-Encoding", "gzip");
    }

    @Test
    public void testBulkFrameCache() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        Response response = given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/frames/1");

        given().log().all(true).header("If-Modified-Since", response.getHeader("last-modified"))
                .header("If-None-Match", response.getHeader("ETag"))
                .get("/wado-rs/studies/123/series/1234/instances/12345/frames/1").then().log().headers()
                .statusCode(304);
    }

    @Test
    public void testMetadataBulkDataURI() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        given().log().all(true).get("/wado-rs/studies/123/series/1234/instances/12345/metadata").then().log().headers()
                .statusCode(200).body(containsString("7FE00010"), containsString("BulkDataURI"),
                        containsString("studies/123/series/1234/instances/12345"));
    }

    @Test
    public void testMetadataBulkDataURIXML() {
        Mockito.when(s3Service.getObject(Mockito.anyString())).thenReturn(getObject(TEST_FILENAME));
        Mockito.when(queryClient.getResults(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(getResults(TEST_FILENAME));
        Response response = given().log().all(true).header("Accept", MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML)
                .get("/wado-rs/studies/123/series/1234/instances/12345/metadata");
        String body = response.asString();
        assertTrue(body.contains("7FE00010"));
        assertTrue(body.contains("BulkData"));
        assertTrue(body.contains("studies/123/series/1234/instances/12345"));
    }

    private List<DicomEntityResult> getResults(String objectName) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomEntityResult result = new DicomEntityResult();
        DicomAttribute tsuid = new DicomAttribute();
        tsuid.setVr(VR.UI.name());
        tsuid.addValue(UID.ExplicitVRLittleEndian);
        result.addElement(Tag.TransferSyntaxUID, tsuid);

        DicomResource resource = new DicomResource();
        resource.setObjectName(objectName);

        result.setResource(resource);
        result.setLastModified(OffsetDateTime.now());
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
