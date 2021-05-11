/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.event.DicomInstance;
import org.alvearie.imaging.ingestion.event.DicomSeries;
import org.alvearie.imaging.ingestion.event.DicomSeriesAttribute;
import org.alvearie.imaging.ingestion.event.DicomStudy;
import org.alvearie.imaging.ingestion.event.StudyRevisionEvent;

@ApplicationScoped
public class StudyRevisonEventBuilder {
    @Inject
    RetrieveService retrieveService;

    public StudyRevisionEvent build(String id) {
        DicomStudyEntity studyEntity = retrieveService.retrieveStudy(id);

        return build(studyEntity);
    }

    public StudyRevisionEvent build(DicomStudyEntity studyEntity) {
        StudyRevisionEvent event = new StudyRevisionEvent();

        if (studyEntity != null) {
            DicomStudy study = new DicomStudy();
            study.setStudyInstanceUID(studyEntity.studyInstanceUID);
            if (studyEntity.series != null) {
                List<DicomSeries> series = new ArrayList<>();
                for (DicomSeriesEntity se : studyEntity.series) {
                    if (se != null) {
                        DicomSeries s = new DicomSeries();
                        s.setSeriesInstanceUID(se.seriesInstanceUID);
                        s.setNumber(se.number);
                        s.setModality(se.modality);
                        if (se.attributes != null) {
                            List<DicomSeriesAttribute> attributes = new ArrayList<>();
                            for (DicomSeriesAttributesEntity sae : se.attributes) {
                                if (sae != null) {
                                    DicomSeriesAttribute sa = new DicomSeriesAttribute();
                                    sa.setTag(sae.tag);
                                    sa.setValue(sae.value);

                                    attributes.add(sa);
                                }
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

            event.setStudy(study);
        }

        return event;
    }
}
