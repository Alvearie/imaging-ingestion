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
        
        // TODO:  Implement the query
        List<DicomStudyEntity> studyInstances = DicomStudyEntity.listAll();
        
        for (DicomStudyEntity studyInstance : studyInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            
            // Add the study date
            DicomAttribute studyDateAttribute = new DicomAttribute();
            studyDateAttribute.addValue(studyInstance.studyDate);
            studyDateAttribute.setVr(VR.DA.name());
            searchResult.addElement(Tag.StudyDate, studyDateAttribute);
            
            // Add the study time
            DicomAttribute studyTimeAttribute = new DicomAttribute();
            studyTimeAttribute.addValue(studyInstance.studyTime);
            studyTimeAttribute.setVr(VR.TM.name());
            searchResult.addElement(Tag.StudyTime, studyTimeAttribute);
            
            // Add the study instance UID
            DicomAttribute studyInstanceUidAttribute = new DicomAttribute();
            studyInstanceUidAttribute.addValue(studyInstance.studyInstanceUID);
            studyInstanceUidAttribute.setVr(VR.UI.name());
            searchResult.addElement(Tag.StudyInstanceUID, studyInstanceUidAttribute);
            
            // Add the study ID
            DicomAttribute studyIdAttribute = new DicomAttribute();
            studyIdAttribute.addValue(studyInstance.studyID);
            studyIdAttribute.setVr(VR.SH.name());
            searchResult.addElement(Tag.StudyID, studyIdAttribute);
            
            List<DicomSeriesEntity> studySeries = studyInstance.series;
            
            // Number of study related series
            DicomAttribute seriesCountAttribute = new DicomAttribute();
            seriesCountAttribute.addValue(Integer.valueOf(studySeries.size()).toString());
            seriesCountAttribute.setVr(VR.IS.name());
            searchResult.addElement(Tag.NumberOfStudyRelatedSeries, seriesCountAttribute);
            
            int instancesInStudy = 0;
            List<String> modalitiesInStudy = new ArrayList<String>();
            for (DicomSeriesEntity series : studySeries) {
                instancesInStudy += series.instances.size();
                modalitiesInStudy.add(series.modality);
            }
            
            // Modalities in study
            DicomAttribute modalitiesInStudyAttribute = new DicomAttribute();
            for (String modality : modalitiesInStudy) {
                modalitiesInStudyAttribute.addValue(modality);
            }
            modalitiesInStudyAttribute.setVr(VR.CS.name());
            searchResult.addElement(Tag.ModalitiesInStudy, modalitiesInStudyAttribute);
           
            // Number of study related instances
            DicomAttribute instancesCountAttribute = new DicomAttribute();
            instancesCountAttribute.addValue(Integer.valueOf(instancesInStudy).toString());
            instancesCountAttribute.setVr(VR.IS.name());
            searchResult.addElement(Tag.NumberOfStudyRelatedInstances, instancesCountAttribute);
            
            // Resource Link
            DicomAttribute retrieveUrlAttribute = new DicomAttribute();
            retrieveUrlAttribute.addValue(studyInstance.provider.wadoExternalEndpoint 
                    + "/studies/" + studyInstance.studyInstanceUID);
            retrieveUrlAttribute.setVr(VR.UR.name());
            searchResult.addElement(Tag.RetrieveURL, retrieveUrlAttribute);
            
            
            List<DicomStudyAttributesEntity> studyAttributes = studyInstance.attributes;
            for (DicomStudyAttributesEntity studyAttribute : studyAttributes) {
                DicomAttribute resultAttribute = new DicomAttribute();
                String value = studyAttribute.value;
                
                if (VR.CS.name().equals(studyAttribute.vr)) {
                    String[] values = value.split(","); 
                    for (String singleValue : values) {
                        resultAttribute.addValue(singleValue);
                    }
                } else {
                    resultAttribute.addValue(value);
                }                
                resultAttribute.setVr(studyAttribute.vr);
                searchResult.addElement(studyAttribute.tag, resultAttribute);
            }
            results.add(searchResult);
        }
        return results;
    }
    
    public List<DicomEntityResult> handleSeriesQuery(DicomQueryModel model) {
        List<DicomEntityResult> results = new ArrayList<>();
        
        // TODO:  Implement the query string
        List<DicomSeriesEntity> seriesInstances;
        if (model.getStudyUid() != null) {
            seriesInstances = DicomSeriesEntity.list("study.studyInstanceUID", model.getStudyUid());
        } else {
            seriesInstances = DicomSeriesEntity.listAll();
        }
        
        for (DicomSeriesEntity series: seriesInstances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            
            // Modality
            DicomAttribute modalityAttribute = new DicomAttribute();
            modalityAttribute.addValue(series.modality);
            modalityAttribute.setVr(VR.CS.name());
            searchResult.addElement(Tag.Modality, modalityAttribute);
            
            // SeriesInstanceUid
            DicomAttribute seriesInstanceUidAttribute = new DicomAttribute();
            seriesInstanceUidAttribute.addValue(series.seriesInstanceUID);
            seriesInstanceUidAttribute.setVr(VR.UI.name());
            searchResult.addElement(Tag.SeriesInstanceUID, seriesInstanceUidAttribute);
            
            // SeriesNumber
            DicomAttribute seriesNumberAttribute = new DicomAttribute();
            seriesNumberAttribute.addValue(Integer.valueOf(series.number).toString());
            seriesNumberAttribute.setVr(VR.IS.name());
            searchResult.addElement(Tag.SeriesNumber, seriesNumberAttribute);
            
            // Instances
            DicomAttribute instancesNumberAttribute = new DicomAttribute();
            instancesNumberAttribute.addValue(Integer.valueOf(series.instances.size()).toString());
            instancesNumberAttribute.setVr(VR.IS.name());
            searchResult.addElement(Tag.NumberOfSeriesRelatedInstances, instancesNumberAttribute);
            
            // Resource Link
            DicomAttribute retrieveUrlAttribute = new DicomAttribute();
            retrieveUrlAttribute.addValue(series.study.provider.wadoExternalEndpoint 
                    + "/studies/" + series.study.studyInstanceUID 
                    + "/series/" + series.seriesInstanceUID);
            retrieveUrlAttribute.setVr(VR.UR.name());
            searchResult.addElement(Tag.RetrieveURL, retrieveUrlAttribute);
            
            List<DicomSeriesAttributesEntity> seriesAttributes = series.attributes;
            for (DicomSeriesAttributesEntity seriesAttribute : seriesAttributes) {
                DicomAttribute resultAttribute = new DicomAttribute();
                String value = seriesAttribute.value;
                
                if (VR.CS.name().equals(seriesAttribute.vr)) {
                    String[] values = value.split(","); 
                    for (String singleValue : values) {
                        resultAttribute.addValue(singleValue);
                    }
                } else {
                    resultAttribute.addValue(value);
                }                
                resultAttribute.setVr(seriesAttribute.vr);
                searchResult.addElement(seriesAttribute.tag, resultAttribute);
            }
            
            results.add(searchResult);
        } 
        return results;
    }
    
    public List<DicomEntityResult> handleInstanceQuery(DicomQueryModel model) {
        List<DicomEntityResult> results = new ArrayList<>();
        
        // TODO:  Implement the query string
        List<DicomInstanceEntity> instances;
        if (model.getSeriesUid() != null) {
            if (model.getStudyUid() != null) {
                instances = DicomInstanceEntity.list("series.study.studyInstanceUID = ?1 and series.seriesInstanceUID = ?2" , model.getStudyUid(), model.getSeriesUid());
            } else {
                instances = DicomInstanceEntity.list("series.seriesInstanceUID", model.getSeriesUid());
            }
        } else {
            instances = DicomInstanceEntity.listAll();
        }
        
        for (DicomInstanceEntity instance : instances) {
            DicomEntityResult searchResult = new DicomEntityResult();
            
            // SOP Class UID
            DicomAttribute sopClassUidAttribute = new DicomAttribute();
            sopClassUidAttribute.addValue(instance.sopClassUID);
            sopClassUidAttribute.setVr(VR.UI.name());
            searchResult.addElement(Tag.SOPClassUID, sopClassUidAttribute);
            
            // SOP Instance UID
            DicomAttribute sopInstanceAttribute = new DicomAttribute();
            sopInstanceAttribute.addValue(instance.sopInstanceUID);
            sopInstanceAttribute.setVr(VR.UI.name());
            searchResult.addElement(Tag.SOPInstanceUID, sopInstanceAttribute);
            
            // Instance Number
            DicomAttribute instanceNumberAttribute = new DicomAttribute();
            instanceNumberAttribute.addValue(Integer.valueOf(instance.number).toString());
            instanceNumberAttribute.setVr(VR.IS.name());
            searchResult.addElement(Tag.SeriesNumber, instanceNumberAttribute);
            
            // Resource Link
            DicomAttribute retrieveUrlAttribute = new DicomAttribute();
            retrieveUrlAttribute.addValue(instance.series.study.provider.wadoExternalEndpoint 
                    + "/studies/" + instance.series.study.studyInstanceUID 
                    + "/series/" + instance.series.seriesInstanceUID
                    + "/instances/" + instance.sopInstanceUID);
            retrieveUrlAttribute.setVr(VR.UR.name());
            searchResult.addElement(Tag.RetrieveURL, retrieveUrlAttribute);
            
            results.add(searchResult);
        }
        return results;
    }
}
