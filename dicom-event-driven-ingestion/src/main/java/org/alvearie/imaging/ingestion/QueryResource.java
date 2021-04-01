/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/query")
public class QueryResource {
    private static final Logger log = Logger.getLogger(QueryResource.class);

    @Inject
    RetrieveService retrieveService;

    @GET
    @Path("/studies/{studyUID}")
    public Response getInstances(@PathParam("studyUID") String studyUID) {
        List<String> instances = retrieveService.getInstances(studyUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    public Response getInstances(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID) {
        List<String> instances = retrieveService.getInstances(studyUID, seriesUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}")
    public Response getInstances(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("instanceUID") String instanceUID) {
        List<String> instances = retrieveService.getInstances(studyUID, seriesUID, instanceUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }
}
