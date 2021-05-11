/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.dcm4che3.data.Tag;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheQuery;

public class QueryHelper {
    private static final Logger LOG = Logger.getLogger(QueryHelper.class);

    public static final int PAGE_SIZE = 1000;

    public class QueryBuilder {
        String queryString;
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        public QueryBuilder(Class<?> entity, String entityAlias, String... joins) {
            queryString = String.format("SELECT DISTINCT %s FROM %s %s", entityAlias, entity.getSimpleName(),
                    entityAlias);
            for (int i = 0; i < joins.length; i += 2) {
                queryString += String.format(" INNER JOIN %s.%s %s", entityAlias, joins[i], joins[i + 1]);
            }
            queryString += " WHERE ";
        }

        public void addParameter(String entityName, String parameterName, Object parameterValue) {
            queryString += String.format("%s %s = :%s", queryParameters.isEmpty() ? "" : " AND", entityName,
                    parameterName);
            queryParameters.put(parameterName, parameterValue);
        }

        public void addListParameter(String entityName, String parameterName, List<Object> parameterValues) {
            queryString += String.format("%s %s IN (:%s)", queryParameters.isEmpty() ? "" : " AND", entityName,
                    parameterName);
            queryParameters.put(parameterName, parameterValues);
        }

        public void addWildcardParameter(String entityName, String parameterName, Object parameterValue) {
            queryString += String.format("%s %s LIKE :%s", queryParameters.isEmpty() ? "" : " AND", entityName,
                    parameterName);
            queryParameters.put(parameterName, parameterValue);
        }

        public void addRangeParameter(String entityName, String parameterName, Object gtValue, Object ltValue) {
            queryString += String.format("%s %s BETWEEN :%s_GT AND :%s_LT", queryParameters.isEmpty() ? "" : " AND",
                    entityName, parameterName, parameterName);
            queryParameters.put(parameterName + "_GT", gtValue);
            queryParameters.put(parameterName + "_LT", ltValue);
        }

        public String getQueryString() {
            return queryString;
        }

        public Map<String, Object> getParameters() {
            return queryParameters;
        }
    }

    @SuppressWarnings("unchecked")
    public List<DicomStudyEntity> queryStudies(DicomQueryModel model, String source) {
        QueryBuilder queryBuilder = new QueryBuilder(DicomStudyEntity.class, "e", "attributes", "a", "series", "s");
        handlePatientIeAttributes(queryBuilder, "a", model);
        handleStudyIeAttributes(queryBuilder, "e", "a", "s", model);
        queryBuilder.addParameter("s.provider.name", "source", source);
        LOG.info("Study Query: " + queryBuilder.getQueryString());
        PanacheQuery<DicomStudyEntity> query = queryBuilder.getParameters().isEmpty() ? DicomStudyEntity.findAll()
                : DicomStudyEntity.find(queryBuilder.getQueryString(), queryBuilder.getParameters());
        LOG.info("Query Result: " + query.count());
        query = (PanacheQuery<DicomStudyEntity>) handlePagination(query, model);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<DicomSeriesEntity> querySeries(DicomQueryModel model, String source) {
        QueryBuilder queryBuilder = new QueryBuilder(DicomSeriesEntity.class, "e", "attributes", "a", "instances", "i",
                "study.attributes", "sa");
        handlePatientIeAttributes(queryBuilder, "sa", model);
        if (model.getStudyUid() != null) {
            queryBuilder.addParameter("e.study.studyInstanceUID", "studyInstanceUID", model.getStudyUid());
        } else {
            handleStudyIeAttributes(queryBuilder, "e.study", "sa", "e", model);
        }
        handleSeriesIeAttributes(queryBuilder, "e", "a", model);
        queryBuilder.addParameter("e.provider.name", "source", source);
        LOG.info("Series Query: " + queryBuilder.getQueryString());
        PanacheQuery<DicomSeriesEntity> query = queryBuilder.getParameters().isEmpty() ? DicomSeriesEntity.findAll()
                : DicomSeriesEntity.find(queryBuilder.getQueryString(), queryBuilder.getParameters());
        LOG.info("Query Result: " + query.count());
        query = (PanacheQuery<DicomSeriesEntity>) handlePagination(query, model);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<DicomInstanceEntity> queryInstances(DicomQueryModel model, String source) {
        QueryBuilder queryBuilder = new QueryBuilder(DicomInstanceEntity.class, "e", "series.attributes", "a",
                "series.study.attributes", "sa");
        handlePatientIeAttributes(queryBuilder, "sa", model);
        if (model.getStudyUid() != null) {
            queryBuilder.addParameter("e.series.study.studyInstanceUID", "studyInstanceUID", model.getStudyUid());
        }
        if (model.getStudyUid() != null) {
            queryBuilder.addParameter("e.series.seriesInstanceUID", "seriesInstanceUID", model.getSeriesUid());
        }

        handleStudyIeAttributes(queryBuilder, "e.series.study", "sa", "e.series", model);
        handleSeriesIeAttributes(queryBuilder, "series", "a", model);
        handleInstanceIeAttributes(queryBuilder, "e", model);
        queryBuilder.addParameter("e.series.provider.name", "source", source);
        LOG.info("Instances Query: " + queryBuilder.getQueryString());
        PanacheQuery<DicomInstanceEntity> query = queryBuilder.getParameters().isEmpty() ? DicomInstanceEntity.findAll()
                : DicomInstanceEntity.find(queryBuilder.getQueryString(), queryBuilder.getParameters());
        LOG.info("Query Result: " + query.count());
        query = (PanacheQuery<DicomInstanceEntity>) handlePagination(query, model);
        return query.list();
    }

    private void handlePatientIeAttributes(QueryBuilder queryBuilder, String studyAttributesAlias,
            DicomQueryModel model) {
        for (Map.Entry<Integer, String> attribute : model.getQueryAttributes().entrySet()) {
            switch (attribute.getKey()) {
            case Tag.PatientName:
                queryBuilder.addParameter(studyAttributesAlias + ".tag", "tag", Integer.valueOf(attribute.getKey()));
                if (model.isFuzzyMatching()) {
                    queryBuilder.addWildcardParameter(studyAttributesAlias + ".value", "value",
                            attribute.getValue().replaceAll("\\*", "%"));
                } else {
                    queryBuilder.addParameter(studyAttributesAlias + ".value", "value", attribute.getValue());
                }
                break;
            case Tag.PatientID:
                queryBuilder.addParameter(studyAttributesAlias + ".tag", "tag", Integer.valueOf(attribute.getKey()));
                queryBuilder.addParameter(studyAttributesAlias + ".value", "value", attribute.getValue());
                break;
            }
        }
    }

    private void handleStudyIeAttributes(QueryBuilder queryBuilder, String studyAlias, String studyAttributesAlias,
            String seriesAlias, DicomQueryModel model) {
        for (Map.Entry<Integer, String> attribute : model.getQueryAttributes().entrySet()) {
            switch (attribute.getKey()) {
            case Tag.StudyInstanceUID:
                String[] values = attribute.getValue().split(",");
                if (values.length > 1) {
                    List<Object> parameters = new ArrayList<Object>();
                    for (String value : values) {
                        parameters.add(value);
                    }
                    queryBuilder.addListParameter(studyAlias + ".studyInstanceUID", "studyInstanceUID", parameters);
                } else {
                    queryBuilder.addParameter(studyAlias + ".studyInstanceUID", "studyInstanceUID",
                            attribute.getValue());
                }
                break;
            case Tag.StudyID:
                queryBuilder.addParameter(studyAlias + ".studyID", "studyID", attribute.getValue());
                break;
            case Tag.StudyTime:
                values = attribute.getValue().split("-");
                if (values.length == 2) {
                    queryBuilder.addRangeParameter(studyAlias + ".studyTime", "studyTime", values[0], values[1]);
                } else {
                    queryBuilder.addParameter(studyAlias + ".studyTime", "studyTime", attribute.getValue());
                }
                break;
            case Tag.StudyDate:
                values = attribute.getValue().split("-");
                if (values.length == 2) {
                    queryBuilder.addRangeParameter(studyAlias + ".studyDate", "studyDate", values[0], values[1]);
                } else {
                    queryBuilder.addParameter(studyAlias + ".studyDate", "studyDate", attribute.getValue());
                }
                break;
            case Tag.ReferringPhysicianName:
            case Tag.AccessionNumber:
                queryBuilder.addParameter(studyAttributesAlias + ".tag", "tag", Integer.valueOf(attribute.getKey()));
                queryBuilder.addParameter(studyAttributesAlias + ".value", "value", attribute.getValue());
                break;
            case Tag.ModalitiesInStudy:
                values = attribute.getValue().split(",");
                if (values.length > 1) {
                    List<Object> parameters = new ArrayList<Object>();
                    for (String value : values) {
                        parameters.add(value);
                    }
                    queryBuilder.addListParameter(seriesAlias + ".modality", "modality", parameters);
                } else {
                    queryBuilder.addParameter(seriesAlias + ".modality", "modality", attribute.getValue());
                }
            }
        }
    }

    private void handleSeriesIeAttributes(QueryBuilder queryBuilder, String seriesAlias, String seriesAttributeAlias,
            DicomQueryModel model) {
        for (Map.Entry<Integer, String> attribute : model.getQueryAttributes().entrySet()) {
            switch (attribute.getKey()) {
            case Tag.Modality:
                queryBuilder.addParameter("e.modality", "modality", attribute.getValue());
                break;
            case Tag.SeriesInstanceUID:
                String[] values = attribute.getValue().split(",");
                if (values.length > 1) {
                    List<Object> parameters = new ArrayList<Object>();
                    for (String value : values) {
                        parameters.add(value);
                    }
                    queryBuilder.addListParameter(seriesAlias + ".seriesInstanceUID", "seriesInstanceUID", parameters);
                } else {
                    queryBuilder.addParameter(seriesAlias + ".seriesInstanceUID", "seriesInstanceUID",
                            attribute.getValue());
                }
                break;
            case Tag.SeriesNumber:
                queryBuilder.addParameter(seriesAlias + ".number", "number", attribute.getValue());
                break;
            case Tag.PerformedProcedureStepStartDate:
            case Tag.PerformedProcedureStepStartTime:
                queryBuilder.addParameter(seriesAttributeAlias + ".tag", "tag", Integer.valueOf(attribute.getKey()));
                queryBuilder.addParameter(seriesAttributeAlias + ".value", "value", attribute.getValue());
            }
        }
    }

    private void handleInstanceIeAttributes(QueryBuilder queryBuilder, String instanceAlias, DicomQueryModel model) {
        for (Map.Entry<Integer, String> attribute : model.getQueryAttributes().entrySet()) {
            switch (attribute.getKey()) {
            case Tag.SOPClassUID:
                String[] values = attribute.getValue().split(",");
                if (values.length > 1) {
                    List<Object> parameters = new ArrayList<Object>();
                    for (String value : values) {
                        parameters.add(value);
                    }
                    queryBuilder.addListParameter(instanceAlias + ".sopClassUID", "sopClassUID", parameters);
                } else {
                    queryBuilder.addParameter(instanceAlias + ".sopClassUID", "sopClassUID", attribute.getValue());
                }
                break;
            case Tag.SOPInstanceUID:
                values = attribute.getValue().split(",");
                if (values.length > 1) {
                    List<Object> parameters = new ArrayList<Object>();
                    for (String value : values) {
                        parameters.add(value);
                    }
                    queryBuilder.addListParameter(instanceAlias + ".sopInstanceUID", "sopInstanceUID", parameters);
                } else {
                    queryBuilder.addParameter(instanceAlias + ".sopInstanceUID", "sopInstanceUID",
                            attribute.getValue());
                }
                break;
            case Tag.InstanceNumber:
                queryBuilder.addParameter(instanceAlias + ".number", "number", attribute.getValue());

            }
        }
    }

    private static PanacheQuery<?> handlePagination(PanacheQuery<?> query, DicomQueryModel model) {
        int startIndex = model.getOffset();
        int endIndex = model.getLimit() == 0 ? PAGE_SIZE : startIndex + model.getLimit();
        LOG.info(String.format("Range %d-%d", startIndex, endIndex));
        return query.range(startIndex, endIndex);
    }
}
