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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.event.DicomInstance;
import org.alvearie.imaging.ingestion.event.DicomSeries;
import org.alvearie.imaging.ingestion.event.DicomStudy;
import org.alvearie.imaging.ingestion.event.Element;
import org.alvearie.imaging.ingestion.event.StudyRevisionChangeSet;
import org.alvearie.imaging.ingestion.event.StudyRevisionEvent;


@ApplicationScoped
public class StudyRevisonEventBuilder {
    @Inject
    RetrieveService retrieveService;

    @Transactional
    public StudyRevisionEvent build(String id) {
        // Lock the study to prevent additional instances from being added while 
        // evaluating the details of the revision
        DicomStudyEntity studyEntity = retrieveService.lockStudy(id);

        return build(studyEntity);
    }

    private StudyRevisionEvent build(DicomStudyEntity studyEntity) {
        StudyRevisionEvent event = new StudyRevisionEvent();
        StudyRevisionChangeSet changeSet = new StudyRevisionChangeSet();
        event.setChangeSet(changeSet);
        event.setRevision(studyEntity.revision);

        if (studyEntity != null) {
            List<String> additions = new ArrayList<>();
            List<String> deletions = new ArrayList<>();
            List<String> modifications = new ArrayList<>();
            DicomStudy study = new DicomStudy();
            study.setStudyInstanceUID(studyEntity.studyInstanceUID);
            study.setStudyDate(studyEntity.studyDate);
            if (studyEntity.attributes != null) {
                List<Element> attributes = new ArrayList<>();
                for (DicomStudyAttributesEntity studyAttribute : studyEntity.attributes) {
                    attributes.add(createElement(studyAttribute));
                }
                study.setAttributes(attributes);
            }
            if (studyEntity.series != null) {
                List<DicomSeries> series = new ArrayList<>();
                for (DicomSeriesEntity se : studyEntity.series) {
                    if (se != null) {
                        DicomSeries s = new DicomSeries();
                        s.setSeriesInstanceUID(se.seriesInstanceUID);
                        s.setNumber(se.number);
                        s.setModality(se.modality);
                        s.setProviderName(se.provider.name);
                        if (se.attributes != null) {
                            List<Element> attributes = new ArrayList<>();
                            for (DicomSeriesAttributesEntity sae : se.attributes) {
                                attributes.add(createElement(sae));
                            }
                            s.setAttributes(attributes);
                        }

                        if (se.instances != null) {
                            List<DicomInstance> instances = new ArrayList<>();
                            for (DicomInstanceEntity ie : se.instances) {
                                if (ie != null) {
                                    DicomInstance inst = new DicomInstance();
                                    inst.setSopInstanceUID(ie.sopInstanceUID);
                                    inst.setSopClassUID(ie.sopClassUID);
                                    inst.setNumber(ie.number);

                                    instances.add(inst);
                                    
                                    String changeReference = String.format("%s/%s", s.getSeriesInstanceUID(), inst.getSopInstanceUID());
                                    if (ie.initialRevision.equals(studyEntity.revision)) {
                                        additions.add(changeReference);
                                    } else if (studyEntity.revision > 0 && ie.deletedRevision != null && ie.deletedRevision.equals(studyEntity.revision)) {
                                        deletions.add(changeReference);
                                    } else if (ie.lastModified.isAfter(studyEntity.revisionTime)) {
                                        modifications.add(changeReference);
                                    } 
                                }
                            }
                            s.setInstances(instances);
                        }

                        if (se.provider != null) {
                            s.setEndpoint(String.format("%s/studies/%s/series/%s", se.provider.wadoExternalEndpoint,
                                    study.getStudyInstanceUID(), s.getSeriesInstanceUID()));
                        }

                        series.add(s);
                    }
                }
                study.setSeries(series);
            }
            
            
            changeSet.setAdditions(additions);
            changeSet.setDeletions(deletions);
            changeSet.setModifications(modifications);
           
            
            incrementRevision(studyEntity);
            event.setStudy(study);
        }

        return event;
    }
    
   
    private void incrementRevision(DicomStudyEntity study) {
        if (study != null) {
            study.revision++;
            study.revisionTime = OffsetDateTime.now(ZoneOffset.UTC);
            study.persist();
        }
    }
    
    private Element createElement(DicomStudyAttributesEntity studyAttribute) {
        Element element = new Element();
        element.setGroup(getTagGroup(studyAttribute.tag));
        element.setElement(getTagElement(studyAttribute.tag));
        element.setVR(studyAttribute.vr);
        element.setValue(studyAttribute.value);
        return element;
    }
    
    private Element createElement(DicomSeriesAttributesEntity seriesAttribute) {
        Element element = new Element();
        element.setGroup(getTagGroup(seriesAttribute.tag));
        element.setElement(getTagElement(seriesAttribute.tag));
        element.setVR(seriesAttribute.vr);
        element.setValue(seriesAttribute.value);
        return element;
    }
    
    private String getTagGroup(Integer tag) {
        String ts = String.format("%08X", tag);
        return ts.substring(0, 4);
    }
    
    private String getTagElement(Integer tag) {
        String ts = String.format("%08X", tag);
        return ts.substring(4);
    }
}
