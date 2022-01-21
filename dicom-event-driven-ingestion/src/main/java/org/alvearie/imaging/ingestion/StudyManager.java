/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.StudyRevisionEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class StudyManager {
    private static final Logger log = Logger.getLogger(StudyManager.class);

    private Map<String, OffsetDateTime> studyLastUpdated = new ConcurrentHashMap<>();

    @ConfigProperty(name = "imaging.ingestion.study.aggregation.timeoutSeconds")
    Integer timeout;

    @ConfigProperty(name = "event.source")
    String eventSource;

    @Inject
    @RestClient
    StudyRevisionEventClient eventClient;

    @Inject
    StudyRevisonEventBuilder eventBuilder;

    @Scheduled(every = "{imaging.ingestion.study.aggregation.schedule}")
    void schedule() {
        log.debug("Schedule triggered");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Map.Entry<String, OffsetDateTime> e : studyLastUpdated.entrySet()) {
            log.debug("Checking study: " + e.getKey() + ", " + e.getValue());
            long durationSeconds = Duration.between(e.getValue(), now).toSeconds();
            log.debug("Duration sec: " + durationSeconds);
            if (durationSeconds > timeout) {
                studyLastUpdated.remove(e.getKey());
                StudyRevisionEvent event = eventBuilder.build(e.getKey());
                if (event != null) {
                    log.info("Sending StudyRevisionEvent: " + e.getKey());
                    try {
                        eventClient.sendEvent(UUID.randomUUID().toString(), Events.StudyRevisionEvent, event,
                                eventSource);
                    } catch (Exception ex) {
                        log.error("Error sending event", ex);
                        log.info(event);
                    }
                }
            }
        }
    }

    public void markLastUpdated(String id) {
        log.debug("Marking lastUpdated for study: " + id);
        this.studyLastUpdated.put(id, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
