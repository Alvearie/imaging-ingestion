/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;

import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.alvearie.imaging.ingestion.model.result.DicomResource;
import org.dcm4che3.data.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class QidoResourceTest {

    @InjectMock
    @RestClient
    DicomQueryClient queryClient;

    @Test
    public void testSearchStudiesByPatientIdAttribute() {
        Mockito.when(queryClient.getResults(Mockito.any(DicomQueryModel.class), Mockito.anyString()))
                .thenReturn(generateTestData());
        given().log().all(true).get("/wado-rs/studies?PatientID=xxx").then().log().all().statusCode(200);
    }

    @Test
    public void testSearchSeriesByPatientIdTag() {
        Mockito.when(queryClient.getResults(Mockito.any(DicomQueryModel.class), Mockito.anyString()))
                .thenReturn(generateTestData());
        given().log().all(true).get("/wado-rs/series?00100020=xxx").then().log().all().statusCode(200);
    }

    @Test
    public void testSearchInstancesBySopClassUid() {
        Mockito.when(queryClient.getResults(Mockito.any(DicomQueryModel.class), Mockito.anyString()))
                .thenReturn(generateTestData());
        given().log().all(true).get("/wado-rs/instances?00080016=1.2.840.10008.5.1.4.1.1.2").then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSearchStudiesByPatientIdAndStudyDateRange() {
        Mockito.when(queryClient.getResults(Mockito.any(DicomQueryModel.class), Mockito.anyString()))
                .thenReturn(generateTestData());
        given().log().all(true).get("/wado-rs/studies?PatientID=xxx&00080020=20200101-20201231").then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSearchStudiesContentEncoding() {
        Mockito.when(queryClient.getResults(Mockito.any(DicomQueryModel.class), Mockito.anyString()))
                .thenReturn(generateTestData());
        given().log().all(true).header("Accept-Encoding", "gzip").get("/wado-rs/studies").then().log().all()
                .statusCode(200).and().header("Content-Encoding", "gzip");
    }

    private List<DicomEntityResult> generateTestData() {
        List<DicomEntityResult> searchResult = new ArrayList<DicomEntityResult>();

        DicomEntityResult dicom = new DicomEntityResult();
        DicomAttribute attribute1 = new DicomAttribute();
        attribute1.setVr("CS");
        attribute1.addValue("abc.123");
        dicom.addElement(Tag.PatientID, attribute1);

        DicomAttribute attribute2 = new DicomAttribute();
        attribute2.setVr("CS");
        attribute2.addValue("abc.123");
        attribute2.addValue("Smith");
        dicom.addElement(Tag.PatientName, attribute2);

        DicomAttribute attribute3 = new DicomAttribute();
        attribute3.setVr("CS");
        dicom.addElement(Tag.AccessionNumber, attribute3);

        DicomResource resource = new DicomResource();
        resource.setObjectName("/path_to_resource");
        dicom.setResource(resource);

        searchResult.add(dicom);

        return searchResult;

    }
}
