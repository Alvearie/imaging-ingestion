/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.io.IOException;
import java.util.ArrayList;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.alvearie.imaging.ingestion.model.result.DicomSearchResult;
import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.dcm4che3.data.Tag;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.GZIP;

@RequestScoped
@Path("/wado-rs")
@GZIP
public class QidoResource {
    private static final Logger LOG = Logger.getLogger(QidoResource.class);

    @ConfigProperty(name = "provider.name")
    String source;

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

    public enum ValidPatientTags {
        PatientName(Tag.PatientName), PatientId(Tag.PatientID);

        int value;

        ValidPatientTags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValidPatientTags lookupTag(int tagValue) {
            for (ValidPatientTags value : values()) {
                if (value.getValue() == tagValue) {
                    return value;
                }
            }
            return null;
        }

        public static ValidPatientTags lookupTag(String tagName) {
            for (ValidPatientTags value : values()) {
                if (value.name().equals(tagName)) {
                    return value;
                }
            }
            return null;
        }

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

    @GET
    @Path("/studies")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchStudy(@PathParam("studyUID") String studyUID, @Context UriInfo uriInfo,
            @Suspended AsyncResponse ar) throws IOException {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.STUDY);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchSeries(@PathParam("studyUID") String studyUID, @Context UriInfo uriInfo,
            @Suspended AsyncResponse ar) {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.SERIES);
        model.setStudyUid(studyUID);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.INSTANCE);
        model.setStudyUid(studyUID);
        model.setSeriesUid(seriesUID);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);

    }

    @GET
    @Path("/studies/{studyUID}/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@PathParam("studyUID") String studyUID, @Context UriInfo uriInfo,
            @Suspended AsyncResponse ar) {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.INSTANCE);
        model.setStudyUid(studyUID);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);

    }

    @GET
    @Path("/series")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchSeries(@Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.SERIES);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    @GET
    @Path("/instances")
    @Produces(APPLICATION_DICOM_JSON)
    public void searchInstance(@Context UriInfo uriInfo, @Suspended AsyncResponse ar) {
        DicomQueryModel model = new DicomQueryModel();
        model.setScope(DicomQueryModel.Scope.INSTANCE);
        buildQidoResponse(model, uriInfo.getQueryParameters(), ar);
    }

    private void buildQidoResponse(DicomQueryModel model, MultivaluedMap<String, String> queryParams,
            AsyncResponse ar) {
        queryParams.entrySet().iterator().forEachRemaining(e -> handleQueryParameter(model, e));
        LOG.info("Qido Model: " + model.toString());

        List<DicomEntityResult> results = queryClient.getResults(model, source);
        Response.ResponseBuilder responseBuilder;
        if (results != null) {
            List<DicomSearchResult> castedResults = new ArrayList<DicomSearchResult>();
            for (DicomEntityResult result : results) {
                DicomSearchResult searchResult = new DicomSearchResult();
                searchResult.setAttributes(result.getAttributes());
                castedResults.add(searchResult);
            }
            responseBuilder = Response.ok(castedResults);
        } else {
            responseBuilder = Response.status(Status.NOT_FOUND);
        }

        ar.resume(responseBuilder.build());
    }

    private void handleQueryParameter(DicomQueryModel model, Entry<String, List<String>> entry) {
        if (entry.getKey().equals(QUERY_PARAM_FUZZYMATCHING)) {
            model.setFuzzyMatching(Boolean.valueOf(entry.getValue().get(0)));
        } else if (entry.getKey().equals(QUERY_PARAM_OFFSET)) {
            model.setOffset(getIntQueryParam(entry.getValue().get(0), 0));
        } else if (entry.getKey().equals(QUERY_PARAM_LIMIT)) {
            model.setLimit(getIntQueryParam(entry.getValue().get(0), DEFAULT_LIMIT));
        } else if (entry.getKey().equals(QUERY_PARAM_INCLUDEFIELD)) {
            filteredFields(entry.getValue(), model.getIncludedFields());
        } else {
            getPatientQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
            switch (model.getScope()) {
            case STUDY:
                getStudyQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
                break;
            case SERIES:
                if (model.getStudyUid() == null) {
                    getStudyQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
                }
                getSeriesQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
                break;
            case INSTANCE:
                if (model.getStudyUid() == null) {
                    getStudyQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
                }
                if (model.getSeriesUid() == null) {
                    getSeriesQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
                }
                getInstanceQueryAttributes(entry.getKey(), entry.getValue().get(0), model.getQueryAttributes());
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

    private void getPatientQueryAttributes(String attribute, String value, Map<Integer, String> results) {
        try {
            int tagValue = Integer.parseUnsignedInt(attribute, 16);
            ValidPatientTags tag = ValidPatientTags.lookupTag(tagValue);
            if (tag != null) {
                results.put(tag.getValue(), value);
            }
        } catch (NumberFormatException e) {
            ValidPatientTags tag = ValidPatientTags.lookupTag(attribute);
            if (tag != null) {
                results.put(tag.getValue(), value);
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
}
