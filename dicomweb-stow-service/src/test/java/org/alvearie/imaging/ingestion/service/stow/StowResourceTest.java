/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.stow;

import static io.restassured.RestAssured.given;

import java.io.File;

import org.alvearie.imaging.ingestion.service.s3.StoreService;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class StowResourceTest {
    @InjectMock
    StoreService storeService;

    @Test
    public void testStoreInstances() {
        given().log().all(true).accept("application/dicom+xml").contentType("multipart/related;type=application/dicom")
                .multiPart("file1", new File("../test-data/dicom/file1.dcm"), "application/dicom").when()
                .post("/stow-rs/studies").then().statusCode(200);
    }
}
