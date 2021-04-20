/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/query")
public class QueryResource {
    private static final Logger LOG = Logger.getLogger(QueryResource.class);

    @Inject
    RetrieveService retrieveService;

    @GET
    @Path("/studies/{studyUID}")
    public Response getResults(@PathParam("studyUID") String studyUID) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    public Response getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID, seriesUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}")
    public Response getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("instanceUID") String instanceUID) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID, seriesUID, instanceUID);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }
    
    @POST
    @Path("/studies")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getResults(DicomQueryModel model) {
        LOG.info("QUERY MODEL " + model.toString());
        List<DicomEntityResult> instances = retrieveService.getResults(model);
        return Response.ok(instances).build(); 
    }
}
