/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
@ApplicationScoped
public interface DicomQueryClient {
    @GET
    @Path("/studies/{studyUID}")
    @Produces("application/json")
    List<String> getInstances(@PathParam("studyUID") String studyUID);

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    @Produces("application/json")
    List<String> getInstances(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID);

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}")
    @Produces("application/json")
    List<String> getInstances(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("instanceUID") String instanceUID);
}
