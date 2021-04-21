/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.model.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DicomQueryModel {

    public enum Scope {
        STUDY, SERIES, INSTANCE;
    }

    private Scope scope;
    private String studyUid;
    private String seriesUid;
    private boolean fuzzyMatching;
    private int offset;
    private int limit;
    private List<String> includedFields = new ArrayList<String>();
    private Map<Integer, String> queryAttributes = new HashMap<Integer, String>();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("scope : ");
        builder.append(getScope());
        builder.append(" fuzzyMatching : ");
        builder.append(isFuzzyMatching());
        builder.append(" offset : ");
        builder.append(getOffset());
        builder.append(" limit : ");
        builder.append(getLimit());
        builder.append(" includedFields : ");
        for (String field : getIncludedFields()) {
            builder.append(field);
            builder.append(',');
        }
        builder.append(" Query : ");
        for (Entry<Integer, String> attribute : getQueryAttributes().entrySet()) {
            builder.append(Integer.toHexString(attribute.getKey()));
            builder.append('=');
            builder.append(attribute.getValue());
            builder.append(',');
        }

        return builder.toString();
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public boolean isFuzzyMatching() {
        return fuzzyMatching;
    }

    public void setFuzzyMatching(boolean fuzzyMatching) {
        this.fuzzyMatching = fuzzyMatching;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<String> getIncludedFields() {
        return includedFields;
    }

    public void setIncludedFields(List<String> includedFields) {
        this.includedFields = includedFields;
    }

    public Map<Integer, String> getQueryAttributes() {
        return queryAttributes;
    }

    public void setQueryAttributes(Map<Integer, String> queryAttributes) {
        this.queryAttributes = queryAttributes;
    }

    public String getStudyUid() {
        return studyUid;
    }

    public void setStudyUid(String studyUid) {
        this.studyUid = studyUid;
    }

    public String getSeriesUid() {
        return seriesUid;
    }

    public void setSeriesUid(String seriesUid) {
        this.seriesUid = seriesUid;
    }
}
