/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DicomEntityResultSerializer extends JsonSerializer<Object> {
    public DicomEntityResultSerializer() {
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DicomEntityResult obj = (DicomEntityResult) value;
        gen.writeStartObject();
        gen.writeObjectField("attributes", obj.getAttributes());
        gen.writeObjectField("resource", obj.getResource());
        gen.writeEndObject();
    }
}
