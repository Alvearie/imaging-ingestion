/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;

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
        Mockito.when(queryClient.getResults(Mockito.anyString())).thenReturn(new ArrayList<>());
        given().log().all(true).get("/wado-rs/studies?PatientID=xxx").then().log().all().statusCode(200);
    }

    @Test
    public void testSearchSeriesByPatientIdTag() {
        Mockito.when(queryClient.getResults(Mockito.anyString())).thenReturn(new ArrayList<>());
        given().log().all(true).get("/wado-rs/series?00100020=xxx").then().log().all().statusCode(200);
    }

    @Test
    public void testSearchInstancesBySopClassUid() {
        Mockito.when(queryClient.getResults(Mockito.anyString())).thenReturn(new ArrayList<>());
        given().log().all(true).get("/wado-rs/instances?00080016=1.2.840.10008.5.1.4.1.1.2").then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSearchStudiesByPatientIdAndStudyDateRange() {
        Mockito.when(queryClient.getResults(Mockito.anyString())).thenReturn(new ArrayList<>());
        given().log().all(true).get("/wado-rs/studies?PatientID=xxx&00080020=20200101-20201231").then().log().all()
                .statusCode(200);
    }
}
