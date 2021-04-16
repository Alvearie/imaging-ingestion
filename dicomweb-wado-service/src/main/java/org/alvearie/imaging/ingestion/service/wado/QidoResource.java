/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.alvearie.imaging.ingestion.service.model.DicomSearchResult;
import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.dcm4che3.data.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@RequestScoped
@Path("/wado-rs")
public class QidoResource {
    private static final Logger LOG = Logger.getLogger(QidoResource.class);

    @Inject
    S3Service s3Service;

    @Inject
    @RestClient
    DicomQueryClient queryClient;

    public final static String APPLICATION_DICOM_JSON = "application/dicom+json";
    public final static MediaType APPLICATION_DICOM_JSON_TYPE = new MediaType("application", "dicom+json");
    public final static int DEFAULT_LIMIT = 1000;

    public static final String QUERY_PARAM_FUZZYMATCHING = "fuzzymatching";
    public static final String QUERY_PARAM_OFFSET = "offset";
    public static final String QUERY_PARAM_LIMIT = "limit";
    public static final String QUERY_PARAM_INCLUDEFIELD = "includefield";

    public enum Scope {
        STUDY, SERIES, INSTANCE;
    }

    public enum ValidStudyTags {
        // Core
        StudyInstanceUID(Tag.StudyInstanceUID),
        // Candidate Extended
        StudyID(Tag.StudyID), StudyDate(Tag.StudyDate), StudyTime(Tag.StudyTime),

        // Extended
        AccessionNumber(Tag.AccessionNumber), ModalitiesInStudy(Tag.ModalitiesInStudy),
        ReferringPhysicianName(Tag.ReferringPhysicianName), PatientName(Tag.PatientName), PatientID(Tag.PatientID);

        int value;

        ValidStudyTags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValidStudyTags lookupTag(int tagValue) {
            for (ValidStudyTags value : values()) {
                if (value.getValue() == tagValue) {
                    return value;
                }
            }
            return null;
        }

        public static ValidStudyTags lookupTag(String tagName) {
            for (ValidStudyTags value : values()) {
                if (value.name().equals(tagName)) {
                    return value;
                }
            }
            return null;
        }
    }

    public enum ValidSeriesTags {
        // Core
        SeriesInstanceUID(Tag.SeriesInstanceUID), SeriesNumber(Tag.SeriesNumber), Modality(Tag.Modality),
        // Extended
        PerformedProcedureStepStartDate(Tag.PerformedProcedureStepStartDate),
        PerformedProcedureStepStartTime(Tag.PerformedProcedureStepStartTime),
        RequestAttributesSequence(Tag.RequestAttributesSequence),
        ScheduledProcedureStepID(Tag.ScheduledProcedureStepID), RequestedProcedureID(Tag.RequestedProcedureID);

        int value;

        ValidSeriesTags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValidSeriesTags lookupTag(int tagValue) {
            for (ValidSeriesTags value : values()) {
                if (value.getValue() == tagValue) {
                    return value;
                }
            }
            return null;
        }

        public static ValidSeriesTags lookupTag(String tagName) {
            for (ValidSeriesTags value : values()) {
                if (value.name().equals(tagName)) {
                    return value;
                }
            }
            return null;
        }

    }

    public enum ValidInstanceTags {
        // Core
        SOPClassUID(Tag.SOPClassUID), SOPInstanceUID(Tag.SOPInstanceUID), InstanceNumber(Tag.InstanceNumber);

        int value;

        ValidInstanceTags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValidInstanceTags lookupTag(int tagValue) {
            for (ValidInstanceTags value : values()) {
                if (value.getValue() == tagValue) {
                    return value;
                }
            }
            return null;
        }

        public static ValidInstanceTags lookupTag(String tagName) {
            for (ValidInstanceTags value : values()) {
                if (value.name().equals(tagName)) {
                    return value;
                }
            }
            return null;
        }
    }

    public class QueryModel {
        Scope scope;
        boolean fuzzyMatching;
        int offset;
        int limit;
        List<String> includedFields = new ArrayList<String>();
        Map<Integer, String> queryAttributes = new HashMap<Integer, String>();

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("scope : ");
            builder.append(scope);
            builder.append("fuzzyMatching : ");
            builder.append(fuzzyMatching);
            builder.append(" offset : ");
            builder.append(offset);
            builder.append(" limit : ");
            builder.append(limit);
            builder.append(" includedFields : ");
            for (String field : includedFields) {
                builder.append(field);
                builder.append(',');
            }
            builder.append(" Query : ");
            for (Entry<Integer, String> attribute : queryAttributes.entrySet()) {
                builder.append(Integer.toHexString(attribute.getKey()));
                builder.append('=');
                builder.append(attribute.getValue());
                builder.append(',');
            }

            return builder.toString();
        }
    }

    @GET
    @Path("/studies")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchStudy(@PathParam("studyUID") String studyUID, @Context UriInfo uriInfo,
            @Suspended AsyncResponse ar) throws IOException {
        QueryModel model = new QueryModel();
        model.scope = Scope.STUDY;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        QueryModel model = new QueryModel();
        model.scope = Scope.SERIES;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        QueryModel model = new QueryModel();
        model.scope = Scope.INSTANCE;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);

    }

    @GET
    @Path("/studies/{studyUID}/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@PathParam("studyUID") String studyUID, @Context UriInfo uriInfo,
            @Suspended AsyncResponse ar) {
        QueryModel model = new QueryModel();
        model.scope = Scope.INSTANCE;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);

    }

    @GET
    @Path("/series")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchSeries(@Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        QueryModel model = new QueryModel();
        model.scope = Scope.SERIES;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        QueryModel model = new QueryModel();
        model.scope = Scope.SERIES;
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    private void buildQidoResponse(QueryModel model, MultivaluedMap<String, String> queryParams, AsyncResponse ar) {
        queryParams.entrySet().iterator().forEachRemaining(e -> handleQueryParameter(model, e));
        LOG.info("Qido Model: " + model.toString());

        Response.ResponseBuilder responseBuilder = Response.ok(generateTestData());
        // Response.ResponseBuilder responseBuilder =
        // Response.status(Response.Status.NOT_IMPLEMENTED);

        ar.resume(responseBuilder.build());
    }

    private void handleQueryParameter(QueryModel model, Entry<String, List<String>> entry) {
        if (entry.getKey().equals(QUERY_PARAM_FUZZYMATCHING)) {
            model.fuzzyMatching = Boolean.valueOf(entry.getValue().get(0));
        } else if (entry.getKey().equals(QUERY_PARAM_OFFSET)) {
            model.offset = getIntQueryParam(entry.getValue().get(0), 0);
        } else if (entry.getKey().equals(QUERY_PARAM_LIMIT)) {
            model.limit = getIntQueryParam(entry.getValue().get(0), DEFAULT_LIMIT);
        } else if (entry.getKey().equals(QUERY_PARAM_INCLUDEFIELD)) {
            filteredFields(entry.getValue(), model.includedFields);
        } else {
            switch (model.scope) {
            case STUDY:
                getStudyQueryAttributes(entry.getKey(), entry.getValue().get(0), model.queryAttributes);
                break;
            case SERIES:
                getSeriesQueryAttributes(entry.getKey(), entry.getValue().get(0), model.queryAttributes);
                break;
            case INSTANCE:
                getInstanceQueryAttributes(entry.getKey(), entry.getValue().get(0), model.queryAttributes);
                break;
            }
        }

    }

    private int getIntQueryParam(String valueString, int defaultValue) {
        int value = defaultValue;
        if (valueString != null) {
            try {
                value = Integer.valueOf(valueString);
            } catch (NumberFormatException e) {
                // ignore, use default
            }
        }
        return value;
    }

    private void filteredFields(List<String> parameterValueList, List<String> results) {
        if (parameterValueList != null) {
            for (String parameterValue : parameterValueList) {
                String[] values = parameterValue.split(",");
                for (String value : values) {
                    results.add(value);
                }
            }
        }
    }

    private void getStudyQueryAttributes(String attribute, String value, Map<Integer, String> results) {
        try {
            int tagValue = Integer.parseUnsignedInt(attribute, 16);
            ValidStudyTags tag = ValidStudyTags.lookupTag(tagValue);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        } catch (NumberFormatException e) {
            ValidStudyTags tag = ValidStudyTags.lookupTag(attribute);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        }
    }

    private void getSeriesQueryAttributes(String attribute, String value, Map<Integer, String> results) {
        try {
            int tagValue = Integer.parseUnsignedInt(attribute, 16);
            ValidSeriesTags tag = ValidSeriesTags.lookupTag(tagValue);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        } catch (NumberFormatException e) {
            ValidSeriesTags tag = ValidSeriesTags.lookupTag(attribute);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        }
    }

    private void getInstanceQueryAttributes(String attribute, String value, Map<Integer, String> results) {
        try {
            int tagValue = Integer.parseUnsignedInt(attribute, 16);
            ValidInstanceTags tag = ValidInstanceTags.lookupTag(tagValue);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        } catch (NumberFormatException e) {
            ValidInstanceTags tag = ValidInstanceTags.lookupTag(attribute);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        }
    }

    private List<DicomSearchResult> generateTestData() {
        List<DicomSearchResult> searchResults = new ArrayList<DicomSearchResult>();
        DicomSearchResult searchResult = new DicomSearchResult();
        DicomSearchResult.DicomAttribute attribute1 = searchResult.createAttribute();
        attribute1.setVr("CS");
        attribute1.addValue("abc.123");
        searchResult.addElement(Tag.PatientID, attribute1);

        DicomSearchResult.DicomAttribute attribute2 = searchResult.createAttribute();
        attribute2.setVr("CS");
        attribute2.addValue("abc.123");
        attribute2.addValue("Smith");
        searchResult.addElement(Tag.PatientName, attribute2);

        DicomSearchResult.DicomAttribute attribute3 = searchResult.createAttribute();
        attribute3.setVr("CS");
        searchResult.addElement(Tag.AccessionNumber, attribute3);

        searchResults.add(searchResult);

        return searchResults;

    }

}
