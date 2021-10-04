/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.samples;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;

@QuarkusTest
public class InstanceBindingFunctionTest {
    private static final String TEST_FILENAME = "src/test/resources/dicom/file1.dcm";

    @InjectMock
    RestClientHelper clientHelper;

    @InjectMock
    @RestClient
    WadoClient wadoClient;

    @Test
    void testDicomAvailableEvent() throws Exception {
        Mockito.when(clientHelper.getClient(Mockito.anyString())).thenReturn(wadoClient);
        Mockito.when(wadoClient.getDicom()).thenReturn(Response.ok(getResult(TEST_FILENAME)).build());
        RestAssured.given().log().all(true).contentType("application/json") //
                .header("ce-specversion", "1.0") //
                .header("ce-id", UUID.randomUUID().toString()) //
                .header("ce-type", InstanceBindingFunction.DicomAvailableEvent) //
                .header("ce-source", "test") //
                .body("\"http://example.com/file1.dcm\"") //
                .post("/") //
                .then() //
                .log() //
                .headers() //
                .statusCode(204);
    }

    private MultipartInput getResult(String filename) throws Exception {
        MultipartInput input = new MultipartInput() {

            @Override
            public String getPreamble() {
                return null;
            }

            @Override
            public List<InputPart> getParts() {
                List<InputPart> parts = new ArrayList<>();
                parts.add(new InputPart() {

                    @Override
                    public void setMediaType(MediaType mediaType) {
                    }

                    @Override
                    public boolean isContentTypeFromMessage() {
                        return false;
                    }

                    @Override
                    public MediaType getMediaType() {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, String> getHeaders() {
                        return null;
                    }

                    @Override
                    public String getBodyAsString() throws IOException {
                        return null;
                    }

                    @Override
                    public <T> T getBody(Class<T> type, Type genericType) throws IOException {
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T getBody(GenericType<T> type) throws IOException {
                        try {
                            return (T) new ByteArrayInputStream(getBytes(filename));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        return null;
                    }
                });
                return parts;
            }

            @Override
            public void close() {
            }
        };
        return input;
    }

    private byte[] getBytes(String filename) throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(filename))) {
            return fis.readAllBytes();
        }
    }
}
