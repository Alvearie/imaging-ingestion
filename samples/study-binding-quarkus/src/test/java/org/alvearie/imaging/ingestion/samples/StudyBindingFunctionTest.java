/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.samples;

import java.nio.file.Paths;
import java.util.UUID;

import org.alvearie.imaging.ingestion.event.Events;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class StudyBindingFunctionTest {
    private static final String TEST_FILENAME = "src/test/resources/event.json";

    @Test
    void testStudyRevisionEvent() throws Exception {
        RestAssured.given().log().all(true).contentType("application/json") //
                .header("ce-specversion", "1.0") //
                .header("ce-id", UUID.randomUUID().toString()) //
                .header("ce-type", Events.StudyRevisionEvent) //
                .header("ce-source", "test") //
                .body(IoUtils.readFile(Paths.get(TEST_FILENAME))) //
                .post("/") //
                .then() //
                .log() //
                .headers() //
                .statusCode(204);
    }
}
