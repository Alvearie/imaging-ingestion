/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import javax.inject.Inject;

import org.alvearie.imaging.ingestion.event.Events;
import org.jboss.logging.Logger;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class StudyRetrieveFunction {
    private static final Logger LOG = Logger.getLogger(StudyRetrieveFunction.class);

    @Inject
    StudyRevisonEventBuilder eventBuilder;

    @Funq
    @CloudEventMapping(trigger = Events.StudyRetrieveEvent)
    public void studyRetrieveEventChain(String id, @Context CloudEvent event) {
        LOG.info("Received event: " + event.id());
        LOG.info(eventBuilder.build(id));
        LOG.info("Event processed: " + event.id());
    }
}
