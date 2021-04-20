/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.io.IOException;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DicomSearchResultSerializer extends JsonSerializer<Object> {
    public DicomSearchResultSerializer() {
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DicomSearchResult obj = (DicomSearchResult) value;

        gen.writeStartObject();
        for (Entry<String, DicomAttribute> element : obj.getAttributes().entrySet()) {
            gen.writeObjectField(element.getKey(), element.getValue());
        }
        gen.writeEndObject();

    }
}
