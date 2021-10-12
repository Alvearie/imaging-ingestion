/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.samples;

import org.alvearie.imaging.ingestion.event.DicomInstance;
import org.alvearie.imaging.ingestion.event.DicomSeries;
import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.StudyRevisionEvent;
import org.jboss.logging.Logger;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class StudyBindingFunction {
    private static final Logger log = Logger.getLogger(StudyBindingFunction.class);

    @Funq
    @CloudEventMapping(trigger = Events.StudyRevisionEvent)
    public void studyRevisionEvent(StudyRevisionEvent data, @Context CloudEvent<String> event) throws Exception {
        log.info("Received event: " + event.id());
        log.info("StudyInstanceUID: " + data.getStudy().getStudyInstanceUID());
        for (DicomSeries series : data.getStudy().getSeries()) {
            log.info("SeriesInstanceUID: " + series.getSeriesInstanceUID());
            for (DicomInstance inst : series.getInstances()) {
                log.info("SopInstanceUID: " + inst.getSopInstanceUID());
            }
        }
    }
}
