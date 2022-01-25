/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.alvearie.imaging.ingestion.event.Events;
import org.alvearie.imaging.ingestion.event.StudyRevisionEvent;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
@ApplicationScoped
@ClientHeaderParam(name = "Ce-Specversion", value = Events.EventSpecVersion)
public interface StudyRevisionEventClient {
    @POST
    @Path("")
    @Produces("application/json")
    void sendEvent(@HeaderParam("Ce-Id") String eventId, @HeaderParam("Ce-Type") String eventType,
            StudyRevisionEvent event, @HeaderParam("Ce-Source") String eventSource);
}
