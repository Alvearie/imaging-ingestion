/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RetrieveService {
    private static final Logger LOG = Logger.getLogger(RetrieveService.class);

    public DicomStudyEntity retrieveStudy(String studyUID) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyUID, false);
        if (study != null) {
            if (study.series != null) {
                for (DicomSeriesEntity series : study.series) {
                    if (series != null) {
                        if (series.instances != null) {
                            for (DicomInstanceEntity instance : series.instances) {
                                // Trigger lazzy load
                            }
                        }
                    }
                }
            }

            if (study.provider != null) {
                // Trigger lazzy load
            }
        }

        return study;
    }

    public List<String> getInstances(String studyId) {
        List<String> instances = new ArrayList<>();
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, false);
        if (study != null) {
            if (study.series != null) {
                for (DicomSeriesEntity series : study.series) {
                    if (series != null) {
                        if (series.instances != null) {
                            for (DicomInstanceEntity instance : series.instances) {
                                instances.add(instance.objectName);
                            }
                        }
                    }
                }
            }
        }

        return instances;
    }

    public List<String> getInstances(String studyId, String seriesId) {
        List<String> instances = new ArrayList<>();
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, false);
        if (series != null && studyId.equals(series.study.studyInstanceUID)) {
            if (series.instances != null) {
                for (DicomInstanceEntity instance : series.instances) {
                    instances.add(instance.objectName);
                }
            }
        }

        return instances;
    }

    public List<String> getInstances(String studyId, String seriesId, String instanceId) {
        List<String> instances = new ArrayList<>();
        DicomInstanceEntity instance = DicomInstanceEntity.findBySopInstanceUID(instanceId);
        if (instance != null && seriesId.equals(instance.series.seriesInstanceUID)
                && studyId.equals(instance.series.study.studyInstanceUID)) {
            instances.add(instance.objectName);
        }

        return instances;
    }
}
