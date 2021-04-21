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
import org.alvearie.imaging.ingestion.entity.DicomSeriesAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomSeriesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyAttributesEntity;
import org.alvearie.imaging.ingestion.entity.DicomStudyEntity;
import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.alvearie.imaging.ingestion.model.result.DicomResource;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
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

    public List<DicomEntityResult> getResults(String studyId) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomStudyEntity study = DicomStudyEntity.findByStudyInstanceUID(studyId, false);
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
                                results.add(result);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    public List<DicomEntityResult> getResults(String studyId, String seriesId) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomSeriesEntity series = DicomSeriesEntity.findBySeriesInstanceUID(seriesId, false);
        if (series != null && studyId.equals(series.study.studyInstanceUID)) {
            if (series.instances != null) {
                for (DicomInstanceEntity instance : series.instances) {
                    DicomEntityResult result = new DicomEntityResult();

                    DicomResource resource = new DicomResource();
                    resource.setObjectName(instance.objectName);

                    result.setResource(resource);
                    results.add(result);
                }
            }
        }

        return results;
    }

    public List<DicomEntityResult> getResults(String studyId, String seriesId, String instanceId) {
        List<DicomEntityResult> results = new ArrayList<>();
        DicomInstanceEntity instance = DicomInstanceEntity.findBySopInstanceUID(instanceId);
        if (instance != null && seriesId.equals(instance.series.seriesInstanceUID)
                && studyId.equals(instance.series.study.studyInstanceUID)) {
            DicomEntityResult result = new DicomEntityResult();

            DicomResource resource = new DicomResource();
            resource.setObjectName(instance.objectName);

            result.setResource(resource);
            results.add(result);
        }

        return results;
    }
    
    public List<DicomEntityResult> getResults(DicomQueryModel model) {
        switch (model.getScope()) {
        case SERIES:
            return handleSeriesQuery(model);
        case INSTANCE:
            return handleInstanceQuery(model);
        case STUDY: 
        default:
            return handleStudyQuery(model);
        }
    }
    
    public List<DicomEntityResult> handleStudyQuery(DicomQueryModel model) {
        List<DicomEntityResult> results = new ArrayList<>();
        
        List<DicomStudyEntity> studyInstances = new QueryHelper().queryStudies(model);
        
        for (DicomStudyEntity studyInstance : studyInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.StudyDate, VR.DA, studyInstance.studyDate);
            addAttributeToEntity(searchResult, Tag.StudyTime, VR.TM, studyInstance.studyTime);
            addAttributeToEntity(searchResult, Tag.StudyInstanceUID, VR.UI, studyInstance.studyInstanceUID);
            addAttributeToEntity(searchResult, Tag.StudyID, VR.SH, studyInstance.studyID);
            
            List<DicomSeriesEntity> studySeries = studyInstance.series;
            
            int instancesInStudy = 0;
            List<String> modalitiesInStudy = new ArrayList<String>();
            for (DicomSeriesEntity series : studySeries) {
                instancesInStudy += series.instances.size();
                modalitiesInStudy.add(series.modality);
            }
            
            addAttributeToEntity(searchResult, Tag.NumberOfStudyRelatedSeries, VR.IS, studySeries.size());
            addAttributeToEntity(searchResult, Tag.NumberOfStudyRelatedInstances, VR.IS, instancesInStudy);        
            addAttributeToEntity(searchResult, Tag.ModalitiesInStudy, VR.CS, modalitiesInStudy);
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR, studyInstance.provider.wadoExternalEndpoint 
                    + "/studies/" + studyInstance.studyInstanceUID);
              
            List<DicomStudyAttributesEntity> studyAttributes = studyInstance.attributes;
            for (DicomStudyAttributesEntity studyAttribute : studyAttributes) {
                addAttributeToEntity(searchResult, studyAttribute.tag.intValue(), VR.valueOf(studyAttribute.vr), studyAttribute.value);
            }
            results.add(searchResult);
        }
        return results;
    }
    
    public List<DicomEntityResult> handleSeriesQuery(DicomQueryModel model) {
        List<DicomEntityResult> results = new ArrayList<>();
        
        List<DicomSeriesEntity> seriesInstances =  new QueryHelper().querySeries(model);
        
        for (DicomSeriesEntity series: seriesInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.Modality, VR.CS, series.modality);
            addAttributeToEntity(searchResult, Tag.SeriesInstanceUID, VR.UI, series.seriesInstanceUID);
            addAttributeToEntity(searchResult, Tag.SeriesNumber, VR.IS, series.number);
            addAttributeToEntity(searchResult, Tag.NumberOfSeriesRelatedInstances, VR.IS, series.instances.size());
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR
                    , series.study.provider.wadoExternalEndpoint 
                    + "/studies/" + series.study.studyInstanceUID 
                    + "/series/" + series.seriesInstanceUID);
            
            for (DicomSeriesAttributesEntity seriesAttribute : series.attributes) {
                addAttributeToEntity(searchResult, seriesAttribute.tag.intValue(), VR.valueOf(seriesAttribute.vr), seriesAttribute.value);
            }
            
            results.add(searchResult);
        } 
        return results;
    }
    
    public List<DicomEntityResult> handleInstanceQuery(DicomQueryModel model) {
        List<DicomEntityResult> results = new ArrayList<>();
        
        // TODO:  Implement the query string
        List<DicomInstanceEntity> instances = new QueryHelper().queryInstances(model);
        for (DicomInstanceEntity instance : instances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            addAttributeToEntity(searchResult, Tag.SOPClassUID, VR.UI, instance.sopClassUID);
            addAttributeToEntity(searchResult, Tag.SOPInstanceUID, VR.UI, instance.sopInstanceUID);
            addAttributeToEntity(searchResult, Tag.SeriesNumber, VR.IS, instance.number);
            addAttributeToEntity(searchResult, Tag.RetrieveURL, VR.UR 
                    , instance.series.study.provider.wadoExternalEndpoint 
                    + "/studies/" + instance.series.study.studyInstanceUID 
                    + "/series/" + instance.series.seriesInstanceUID
                    + "/instances/" + instance.sopInstanceUID);        
            results.add(searchResult);
        }
        return results;
    }
    
    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation, int value) {
        return addAttributeToEntity(result, tag, valueRepresentation, Integer.valueOf(value).toString());
    }
    
    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation, String value) {
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
    
    private DicomEntityResult addAttributeToEntity(DicomEntityResult result, int tag, VR valueRepresentation, List<String> values) {
        DicomAttribute instanceNumberAttribute = new DicomAttribute();
        for (String value : values) {
            instanceNumberAttribute.addValue(value);
        }
        instanceNumberAttribute.setVr(valueRepresentation.name());
        result.addElement(tag, instanceNumberAttribute);
        return result;
    }
}
