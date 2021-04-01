/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.entity.ProviderEntity;
import org.alvearie.imaging.ingestion.event.Element;
import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.ImageStoredEvent;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

/**
 * EventProcessorFunction
 */
public class EventProcessorFunction {
    private static final Logger log = Logger.getLogger(EventProcessorFunction.class);

    @Inject
    StudyManager studyManager;

    @Funq
    @CloudEventMapping(trigger = Events.ImageStoredEvent, responseSource = Events.EventSource, responseType = Events.DicomAvailableEvent)
    public String imageStoredEventChain(ImageStoredEvent data, @Context CloudEvent event) {
        log.info("Received event: " + event.id());

        String resource = null;
        try {
            resource = process(data);
            log.info("Resource: " + resource);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("Event processed: " + event.id());
        return resource;
    }

    @Transactional
    public DicomStudyEntity storeStudy(String studyId, String providerName, String endpoint, String region,
            String bucket, String wadoInternalEndpoint, String wadoExternalEndpoint) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, false);

        if (study == null) {
            study = new DicomStudyEntity();
            study.studyInstanceUID = studyId;
            study.revision = 0;
            study.revisionTime = OffsetDateTime.now(ZoneOffset.UTC);
            study.provider = new ProviderEntity();
            study.provider.study = study;
        }
        study.provider.name = providerName;
        study.provider.bucketEndpoint = endpoint;
        study.provider.bucketRegion = region;
        study.provider.bucketName = bucket;
        study.provider.wadoInternalEndpoint = wadoInternalEndpoint;
        study.provider.wadoExternalEndpoint = wadoExternalEndpoint;

        if (!study.isPersistent()) {
            study.persist();
        }

        return study;
    }

    @Transactional
    public DicomSeriesEntity storeSeries(String studyId, String seriesId, Integer seriesNumber,
            List<DicomSeriesAttributesEntity> attributes) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, false);
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, false);

        if (series == null) {
            series = new DicomSeriesEntity();
            series.seriesInstanceUID = seriesId;
            study.addSeries(series);
        }
        series.number = seriesNumber;
        for (DicomSeriesAttributesEntity attr : attributes) {
            series.addAttribute(attr);
        }

        if (!series.isPersistent()) {
            series.persist();
        }

        return series;
    }

    @Transactional
    public DicomInstanceEntity storeInstance(String seriesId, String instanceId, String sopClassId,
            Integer instanceNumber, String objectName) {
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, false);
        DicomInstanceEntity instance = DicomInstanceEntity.findBySopInstanceUID(instanceId);

        if (instance == null) {
            instance = new DicomInstanceEntity();
            instance.sopInstanceUID = instanceId;
            series.addInstance(instance);
        } else {
            log.info("Instance exists: " + instanceId);
        }
        instance.sopClassUID = sopClassId;
        instance.number = instanceNumber;
        instance.objectName = objectName;
        instance.lastModified = OffsetDateTime.now(ZoneOffset.UTC);

        if (!instance.isPersistent()) {
            try {
                instance.persist();
            } catch (ConstraintViolationException e) {
                log.info(e.getMessage());
            }
        }

        return instance;
    }

    private String process(ImageStoredEvent e) {
        DicomStudyEntity study = null;
        DicomSeriesEntity series = null;
        DicomInstanceEntity instance = null;
        List<DicomSeriesAttributesEntity> attributes = new ArrayList<>();

        if (e != null) {
            String providerName = null;

            if (e.getImage() != null) {
                String studyId = null;
                String seriesId = null;
                String instanceId = null;
                String sopClassId = null;
                Integer seriesNumber = null;
                Integer instanceNumber = null;
                String bucketEndpoint = null;
                String bucketRegion = null;
                String bucketName = null;
                String objectName = null;
                String wadoInternalEndpoint = null;
                String wadoExternalEndpoint = null;

                if (e.getImage().getElements() != null) {
                    for (Element elem : e.getImage().getElements()) {
                        if (DicomConstants.STUDY_INSTANCE_UID_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.STUDY_INSTANCE_UID_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            studyId = elem.getValue();
                        } else if (DicomConstants.SERIES_INSTANCE_UID_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.SERIES_INSTANCE_UID_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            seriesId = elem.getValue();
                        } else if (DicomConstants.SOP_INSTANCE_UID_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.SOP_INSTANCE_UID_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            instanceId = elem.getValue();
                        } else if (DicomConstants.SOP_CLASS_UID_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.SOP_CLASS_UID_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            sopClassId = elem.getValue();
                        } else if (DicomConstants.SERIES_NUMBER_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.SERIES_NUMBER_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            try {
                                seriesNumber = Integer.parseInt(elem.getValue());
                            } catch (NumberFormatException ex) {
                                log.error(ex.getMessage());
                            }
                        } else if (DicomConstants.INSTANCE_NUMBER_GROUP.equalsIgnoreCase(elem.getGroup())
                                && DicomConstants.INSTANCE_NUMBER_ELEMENT.equalsIgnoreCase(elem.getElement())) {
                            try {
                                instanceNumber = Integer.parseInt(elem.getValue());
                            } catch (NumberFormatException ex) {
                                log.error(ex.getMessage());
                            }
                        } else if (DicomConstants.MODALITY_GROUP.equals(elem.getGroup())
                                && DicomConstants.MODALITY_ELEMENT.equals(elem.getElement())) {
                            attributes.add(buildAttribute(elem.getGroup(), elem.getElement(), elem.getValue()));
                        }
                    }
                }

                if (e.getStore() != null) {
                    providerName = e.getStore().getProvider();
                    bucketName = e.getStore().getBucketName();
                    objectName = e.getStore().getObjectName();
                    wadoInternalEndpoint = e.getStore().getWadoInternalEndpoint();
                    wadoExternalEndpoint = e.getStore().getWadoExternalEndpoint();
                }

                if (studyId == null) {
                    log.error("Study ID is null");
                    return null;
                }

                study = storeStudyWithRetry(studyId, providerName, bucketEndpoint, bucketRegion, bucketName,
                        wadoInternalEndpoint, wadoExternalEndpoint);
                log.info("Study ID: " + study.id);

                if (seriesId == null) {
                    log.error("Series ID is null");
                    return null;
                }

                series = storeSeriesWithRetry(studyId, seriesId, seriesNumber, attributes);
                log.info("Series ID: " + series.id);

                if (instanceId == null) {
                    log.error("Instance ID is null");
                    return null;
                }

                instance = storeInstance(seriesId, instanceId, sopClassId, instanceNumber, objectName);
                log.info("Instance ID: " + instance.id);

                studyManager.markLastUpdated(studyId);
            }
        }

        return study == null || study.provider == null ? null : study.provider.wadoInternalEndpoint;
    }

    private DicomSeriesEntity storeSeriesWithRetry(String studyId, String seriesId, Integer seriesNumber,
            List<DicomSeriesAttributesEntity> attributes) {
        DicomSeriesEntity series = null;
        try {
            series = storeSeries(studyId, seriesId, seriesNumber, attributes);
        } catch (Exception e) {
            // Check for ConstraintViolationException for seriesId when there is high
            // concurrency. ConstraintViolationException is wrapped under RollbackException.
            // Retry when ConstraintViolationException or rethrow.
            if (e.getCause() instanceof javax.transaction.RollbackException
                    && e.getCause().getCause() instanceof javax.persistence.PersistenceException && e.getCause()
                            .getCause().getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                log.error("ConstraintViolationException, retrying storeSeries ...");
                series = storeSeries(studyId, seriesId, seriesNumber, attributes);
            } else {
                throw e;
            }
        }

        return series;
    }

    private DicomStudyEntity storeStudyWithRetry(String studyId, String providerName, String endpoint, String region,
            String bucket, String wadoInternalEndpoint, String wadoExternalEndpoint) {
        DicomStudyEntity study = null;
        try {
            study = storeStudy(studyId, providerName, endpoint, region, bucket, wadoInternalEndpoint,
                    wadoExternalEndpoint);
        } catch (Exception e) {
            // Check for ConstraintViolationException for studyId when there is high
            // concurrency. ConstraintViolationException is wrapped under RollbackException.
            // Retry when ConstraintViolationException or rethrow.
            if (e.getCause() instanceof javax.transaction.RollbackException
                    && e.getCause().getCause() instanceof javax.persistence.PersistenceException && e.getCause()
                            .getCause().getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                log.error("ConstraintViolationException, retrying storeStudy ...");
                study = storeStudy(studyId, providerName, endpoint, region, bucket, wadoInternalEndpoint,
                        wadoExternalEndpoint);
            } else {
                throw e;
            }
        }

        return study;
    }

    private DicomSeriesAttributesEntity buildAttribute(String group, String element, String value) {
        DicomSeriesAttributesEntity fe = new DicomSeriesAttributesEntity();

        fe.group = Integer.parseInt(group, 16);
        fe.element = Integer.parseInt(element, 16);
        fe.value = value;

        return fe;
    }
}
