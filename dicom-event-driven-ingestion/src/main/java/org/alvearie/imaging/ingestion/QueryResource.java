/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    public Response getResults(@PathParam("studyUID") String studyUID, @NotBlank @QueryParam("source") String source) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID, source);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    public Response getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @NotBlank @QueryParam("source") String source) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID, seriesUID, source);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}")
    public Response getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("instanceUID") String instanceUID, @NotBlank @QueryParam("source") String source) {
        List<DicomEntityResult> instances = retrieveService.getResults(studyUID, seriesUID, instanceUID, source);
        if (instances == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(instances).build();
    }

    @POST
    @Path("/studies")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getResults(DicomQueryModel model, @NotBlank @QueryParam("source") String source) {
        LOG.info("QUERY MODEL " + model.toString());
        List<DicomEntityResult> instances = retrieveService.getResults(model, source);
        return Response.ok(instances).build();
    }
}
