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
import org.alvearie.imaging.ingestion.entity.DicomStudyAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.entity.ProviderEntity;
import org.alvearie.imaging.ingestion.event.Element;
import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.ImageStoredEvent;
import org.dcm4che3.data.Tag;
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
    public DicomStudyEntity storeStudy(String studyInstanceUID, String studyID, String studyDate, String studyTime,
            String providerName, String endpoint, String region, String bucket, String wadoInternalEndpoint,
            String wadoExternalEndpoint, List<DicomStudyAttributesEntity> studyAttributes) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyInstanceUID, false);

        if (study == null) {
            study = new DicomStudyEntity();
            study.studyInstanceUID = studyInstanceUID;
            study.studyID = studyID;
            study.revision = 0;
            study.revisionTime = OffsetDateTime.now(ZoneOffset.UTC);
            study.provider = new ProviderEntity();
            study.provider.study = study;
        }
        study.studyDate = studyDate;
        study.studyTime = studyTime;
        study.provider.name = providerName;
        study.provider.bucketEndpoint = endpoint;
        study.provider.bucketRegion = region;
        study.provider.bucketName = bucket;
        study.provider.wadoInternalEndpoint = wadoInternalEndpoint;
        study.provider.wadoExternalEndpoint = wadoExternalEndpoint;
        for (DicomStudyAttributesEntity attr : studyAttributes) {
            study.addAttribute(attr);
        }

        if (!study.isPersistent()) {
            study.persist();
        }

        return study;
    }

    @Transactional
    public DicomSeriesEntity storeSeries(String studyId, String seriesId, Integer seriesNumber, String modality,
            List<DicomSeriesAttributesEntity> attributes) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, false);
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, false);

        if (series == null) {
            series = new DicomSeriesEntity();
            series.seriesInstanceUID = seriesId;
            study.addSeries(series);
        }
        series.number = seriesNumber;
        series.modality = modality;
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
            String transferSyntaxUID, Integer instanceNumber, String objectName) {
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
        instance.transferSyntaxUID = transferSyntaxUID;
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
        List<DicomSeriesAttributesEntity> seriesAttributes = new ArrayList<>();
        List<DicomStudyAttributesEntity> studyAttributes = new ArrayList<>();

        if (e != null) {
            String providerName = null;

            if (e.getImage() != null) {
                String transferSyntaxUID = e.getImage().getTransferSyntaxUID();
                String studyInstanceUID = null;
                String studyID = null;
                String studyDate = null;
                String studyTime = null;
                String seriesId = null;
                String instanceId = null;
                String sopClassId = null;
                Integer seriesNumber = null;
                String modality = null;
                Integer instanceNumber = null;
                String bucketEndpoint = null;
                String bucketRegion = null;
                String bucketName = null;
                String objectName = null;
                String wadoInternalEndpoint = null;
                String wadoExternalEndpoint = null;

                if (e.getImage().getElements() != null) {
                    for (Element elem : e.getImage().getElements()) {
                        int tag = getTag(elem.getGroup(), elem.getElement());
                        if (tag == 0) {
                            continue;
                        }

                        switch (tag) {
                        case Tag.StudyInstanceUID:
                            studyInstanceUID = elem.getValue();
                            break;
                        case Tag.StudyID:
                            studyID = elem.getValue();
                            break;
                        case Tag.StudyDate:
                            studyDate = elem.getValue();
                            break;
                        case Tag.StudyTime:
                            studyTime = elem.getValue();
                            break;
                        case Tag.AccessionNumber:
                        case Tag.ModalitiesInStudy:
                        case Tag.ReferringPhysicianName:
                        case Tag.PatientName:
                        case Tag.StudyDescription:
                        case Tag.PatientID:
                        case Tag.PatientBirthDate:
                        case Tag.PatientSex:
                            studyAttributes.add(buildStudyAttribute(elem.getGroup(), elem.getElement(), elem.getVR(),
                                    elem.getValue()));
                            break;
                        case Tag.SeriesInstanceUID:
                            seriesId = elem.getValue();
                            break;
                        case Tag.SOPInstanceUID:
                            instanceId = elem.getValue();
                            break;
                        case Tag.SOPClassUID:
                            sopClassId = elem.getValue();
                            break;
                        case Tag.SeriesNumber:
                            try {
                                seriesNumber = Integer.parseInt(elem.getValue());
                            } catch (NumberFormatException ex) {
                                log.error(ex.getMessage());
                            }
                            break;
                        case Tag.Modality:
                            modality = elem.getValue();
                            break;
                        case Tag.SeriesDescription:
                        case Tag.PerformedProcedureStepStartDate:
                        case Tag.PerformedProcedureStepStartTime:
                        case Tag.RequestAttributesSequence:
                        case Tag.ScheduledProcedureStepID:
                        case Tag.RequestedProcedureID:
                            seriesAttributes.add(buildSeriesAttribute(elem.getGroup(), elem.getElement(), elem.getVR(),
                                    elem.getValue()));
                            break;
                        case Tag.InstanceNumber:
                            try {
                                instanceNumber = Integer.parseInt(elem.getValue());
                            } catch (NumberFormatException ex) {
                                log.error(ex.getMessage());
                            }
                            break;
                        default:
                            break;
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

                if (studyInstanceUID == null) {
                    log.error("Study ID is null");
                    return null;
                }

                study = storeStudyWithRetry(studyInstanceUID, studyID, studyDate, studyTime, providerName,
                        bucketEndpoint, bucketRegion, bucketName, wadoInternalEndpoint, wadoExternalEndpoint,
                        studyAttributes);
                log.info("Study ID: " + study.id);

                if (seriesId == null) {
                    log.error("Series ID is null");
                    return null;
                }

                series = storeSeriesWithRetry(studyInstanceUID, seriesId, seriesNumber, modality, seriesAttributes);
                log.info("Series ID: " + series.id);

                if (instanceId == null) {
                    log.error("Instance ID is null");
                    return null;
                }

                if (transferSyntaxUID == null) {
                    log.error("TransferSyntaxUID is null");
                    return null;
                }

                instance = storeInstance(seriesId, instanceId, sopClassId, transferSyntaxUID, instanceNumber,
                        objectName);
                log.info("Instance ID: " + instance.id);

                studyManager.markLastUpdated(studyInstanceUID);

                return String.format("%s/studies/%s/series/%s/instances/%s", study.provider.wadoInternalEndpoint,
                        study.studyInstanceUID, series.seriesInstanceUID, instance.sopInstanceUID);
            }
        }

        return null;
    }

    private DicomSeriesEntity storeSeriesWithRetry(String studyId, String seriesId, Integer seriesNumber,
            String modality, List<DicomSeriesAttributesEntity> attributes) {
        DicomSeriesEntity series = null;
        try {
            series = storeSeries(studyId, seriesId, seriesNumber, modality, attributes);
        } catch (Exception e) {
            // Check for ConstraintViolationException for seriesId when there is high
            // concurrency. ConstraintViolationException is wrapped under RollbackException.
            // Retry when ConstraintViolationException or rethrow.
            if (e.getCause() instanceof javax.transaction.RollbackException
                    && e.getCause().getCause() instanceof javax.persistence.PersistenceException && e.getCause()
                            .getCause().getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                log.error("ConstraintViolationException, retrying storeSeries ...");
                series = storeSeries(studyId, seriesId, seriesNumber, modality, attributes);
            } else {
                throw e;
            }
        }

        return series;
    }

    private DicomStudyEntity storeStudyWithRetry(String studyInstanceUID, String studyID, String studyDate,
            String studyTime, String providerName, String endpoint, String region, String bucket,
            String wadoInternalEndpoint, String wadoExternalEndpoint, List<DicomStudyAttributesEntity> attributes) {
        DicomStudyEntity study = null;
        try {
            study = storeStudy(studyInstanceUID, studyID, studyDate, studyTime, providerName, endpoint, region, bucket,
                    wadoInternalEndpoint, wadoExternalEndpoint, attributes);
        } catch (Exception e) {
            // Check for ConstraintViolationException for studyId when there is high
            // concurrency. ConstraintViolationException is wrapped under RollbackException.
            // Retry when ConstraintViolationException or rethrow.
            if (e.getCause() instanceof javax.transaction.RollbackException
                    && e.getCause().getCause() instanceof javax.persistence.PersistenceException && e.getCause()
                            .getCause().getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                log.error("ConstraintViolationException, retrying storeStudy ...");
                study = storeStudy(studyInstanceUID, studyID, studyDate, studyTime, providerName, endpoint, region,
                        bucket, wadoInternalEndpoint, wadoExternalEndpoint, attributes);
            } else {
                throw e;
            }
        }

        return study;
    }

    private DicomStudyAttributesEntity buildStudyAttribute(String group, String element, String vr, String value) {
        DicomStudyAttributesEntity attr = new DicomStudyAttributesEntity();

        attr.tag = Integer.parseInt(group + element, 16);
        attr.vr = vr;
        attr.value = value;

        return attr;
    }

    private DicomSeriesAttributesEntity buildSeriesAttribute(String group, String element, String vr, String value) {
        DicomSeriesAttributesEntity attr = new DicomSeriesAttributesEntity();

        attr.tag = Integer.parseInt(group + element, 16);
        attr.vr = vr;
        attr.value = value;

        return attr;
    }

    private int getTag(String group, String element) {
        int tag = 0;

        try {
            tag = Integer.parseInt(group + element, 16);
        } catch (NumberFormatException e) {
            log.error("Invalid group/element: " + group + "/" + element);
        }

        return tag;
    }
}
