/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.alvearie.imaging.ingestion.entity.DicomInstanceEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.alvearie.imaging.ingestion.model.result.DicomResource;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;

@ApplicationScoped
public class RetrieveService {
    
    protected DicomStudyEntity loadStudy(String studyUID, boolean lock) {
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyUID, lock);
        if (study != null) {
            if (study.series != null) {
                for (DicomSeriesEntity series : study.series) {
                    if (series != null) {
                        if (series.instances != null) {
                            for (@SuppressWarnings("unused")
                            DicomInstanceEntity instance : series.instances) {
                                // Trigger lazzy load
                            }
                        }

                        if (series.provider != null) {
                            // Trigger lazzy load
                        }
                    }
                }
            }
        }

        return study;
    }

    public DicomStudyEntity retrieveStudy(String studyUID) {
        return loadStudy(studyUID, false);
    }
    
    public DicomStudyEntity lockStudy(String studyUID) {
        return loadStudy(studyUID, true);
    }

    public List<DicomEntityResult> getResults(String studyId, String source) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, source, false);
        if (study != null) {
            if (study.series != null) {
                for (DicomSeriesEntity series : study.series) {
                    if (series != null) {
                        if (series.instances != null) {
                            for (DicomInstanceEntity instance : series.instances) {
                                DicomEntityResult result = new DicomEntityResult();

                                DicomResource resource = new DicomResource();
                                resource.setObjectName(instance.objectName);
                                result.setResource(resource);
                                result.setLastModified(instance.lastModified);
                                results.add(result);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    public List<DicomEntityResult> getResults(String studyId, String seriesId, String source) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, source, false);
        if (series != null && studyId.equals(series.study.studyInstanceUID)) {
            if (series.instances != null) {
                for (DicomInstanceEntity instance : series.instances) {
                    DicomEntityResult result = new DicomEntityResult();

                    DicomResource resource = new DicomResource();
                    resource.setObjectName(instance.objectName);
                    result.setResource(resource);
                    result.setLastModified(instance.lastModified);
                    results.add(result);
                }
            }
        }

        return results;
    }

    public List<DicomEntityResult> getResults(String studyId, String seriesId, String instanceId, String source) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomInstanceEntity instance = DicomInstanceEntity.findBySopInstanceUID(instanceId);
        if (instance != null && seriesId.equals(instance.series.seriesInstanceUID)
                && studyId.equals(instance.series.study.studyInstanceUID)) {
            DicomEntityResult result = new DicomEntityResult();

            DicomResource resource = new DicomResource();
            resource.setObjectName(instance.objectName);
            result.setResource(resource);
            result.setLastModified(instance.lastModified);
            results.add(result);
        }

        return results;
    }

    public List<DicomEntityResult> getResults(DicomQueryModel model, String source) {
        switch (model.getScope()) {
        case SERIES:
            return handleSeriesQuery(model, source);
        case INSTANCE:
            return handleInstanceQuery(model, source);
        case STUDY:
        default:
            return handleStudyQuery(model, source);
        }
    }

    public List<DicomEntityResult> handleStudyQuery(DicomQueryModel model, String source) {
        List<DicomEntityResult> results = new ArrayList<>();

        List<DicomStudyEntity> studyInstances = new QueryHelper().queryStudies(model, source);

        for (DicomStudyEntity studyInstance : studyInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.StudyDate, VR.DA, studyInstance.studyDate);
            addAttributeToEntity(searchResult, Tag.StudyTime, VR.TM, studyInstance.studyTime);
            addAttributeToEntity(searchResult, Tag.StudyInstanceUID, VR.UI, studyInstance.studyInstanceUID);
            addAttributeToEntity(searchResult, Tag.StudyID, VR.SH, studyInstance.studyID);

            List<DicomSeriesEntity> studySeries = studyInstance.series;

            int instancesInStudy = 0;
            String endpoint = null;
            Set<String> modalitiesInStudy = new HashSet<String>();
            Set<String> sopClassesInStudy = new HashSet<String>();
            for (DicomSeriesEntity series : studySeries) {
                instancesInStudy += series.instances.size();
                modalitiesInStudy.add(series.modality);
                if (StringUtils.isBlank(endpoint) && series.provider != null) {
                    endpoint = series.provider.wadoExternalEndpoint;
                }
                for (DicomInstanceEntity instance : series.instances) {
                    sopClassesInStudy.add(instance.sopClassUID);
                }
            }

            // Build out the Query/Retrieve attributes as defined in Part 4, Table C.3-1 
            // Missing Tag.AlternateRepresentationSequence 
            addAttributeToEntity(searchResult, Tag.NumberOfStudyRelatedSeries, VR.IS, studySeries.size());
            addAttributeToEntity(searchResult, Tag.NumberOfStudyRelatedInstances, VR.IS, instancesInStudy);
            addAttributeToEntity(searchResult, Tag.ModalitiesInStudy, VR.CS, modalitiesInStudy.toArray(new String[0]));
            addAttributeToEntity(searchResult, Tag.SOPClassesInStudy, VR.CS, sopClassesInStudy.toArray(new String[0]));
            
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR,
                    String.format("%s/studies/%s", endpoint, studyInstance.studyInstanceUID));

            List<DicomStudyAttributesEntity> studyAttributes = studyInstance.attributes;
            for (DicomStudyAttributesEntity studyAttribute : studyAttributes) {
                addAttributeToEntity(searchResult, studyAttribute.tag.intValue(), VR.valueOf(studyAttribute.vr),
                        studyAttribute.value);
            }
            results.add(searchResult);
        }
        return results;
    }

    public List<DicomEntityResult> handleSeriesQuery(DicomQueryModel model, String source) {
        List<DicomEntityResult> results = new ArrayList<>();

        List<DicomSeriesEntity> seriesInstances = new QueryHelper().querySeries(model, source);

        for (DicomSeriesEntity series : seriesInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.Modality, VR.CS, series.modality);
            addAttributeToEntity(searchResult, Tag.SeriesInstanceUID, VR.UI, series.seriesInstanceUID);
            addAttributeToEntity(searchResult, Tag.SeriesNumber, VR.IS, series.number);
            addAttributeToEntity(searchResult, Tag.NumberOfSeriesRelatedInstances, VR.IS, series.instances.size());
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR, String.format("%s/studies/%s/series/%s",
                    series.provider.wadoExternalEndpoint, series.study.studyInstanceUID, series.seriesInstanceUID));

            for (DicomSeriesAttributesEntity seriesAttribute : series.attributes) {
                addAttributeToEntity(searchResult, seriesAttribute.tag.intValue(), VR.valueOf(seriesAttribute.vr),
                        seriesAttribute.value);
            }

            results.add(searchResult);
        }
        return results;
    }

    public List<DicomEntityResult> handleInstanceQuery(DicomQueryModel model, String source) {
        List<DicomEntityResult> results = new ArrayList<>();

        List<DicomInstanceEntity> instances = new QueryHelper().queryInstances(model, source);
        for (DicomInstanceEntity instance : instances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.SOPClassUID, VR.UI, instance.sopClassUID);
            addAttributeToEntity(searchResult, Tag.SOPInstanceUID, VR.UI, instance.sopInstanceUID);
            addAttributeToEntity(searchResult, Tag.TransferSyntaxUID, VR.UI, instance.transferSyntaxUID);
            addAttributeToEntity(searchResult, Tag.SeriesNumber, VR.IS, instance.number);
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR,
                    String.format("%s/studies/%s/series/%s/instances/%s", instance.series.provider.wadoExternalEndpoint,
                            instance.series.study.studyInstanceUID, instance.series.seriesInstanceUID,
                            instance.sopInstanceUID));
            results.add(searchResult);
        }
        return results;
    }

    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation,
            int value) {
        return addAttributeToEntity(result, tag, valueRepresentation, Integer.valueOf(value).toString());
    }

    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation,
            String value) {
        DicomAttribute attribute = new DicomAttribute();
        if (value != null && VR.CS == valueRepresentation) {
            String[] values = value.split(",");
            for (String singleValue : values) {
                attribute.addValue(singleValue);
            }
        } else {
            attribute.addValue(value);
        }
        attribute.setVr(valueRepresentation.name());
        result.addElement(tag, attribute);
        return result;
    }

    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation,
            String[] values) {
        DicomAttribute attribute = new DicomAttribute();
        if (values != null) {
            for (String value : values) {
                attribute.addValue(value);
            }
        }
        attribute.setVr(valueRepresentation.name());
        result.addElement(tag, attribute);
        return result;
    }
}
