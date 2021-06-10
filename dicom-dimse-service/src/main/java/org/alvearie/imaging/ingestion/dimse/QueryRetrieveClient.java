/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
@ApplicationScoped
public interface QueryRetrieveClient {
    
    @GET
    @Path("/studies/{studyUID}")
    @Produces("application/json")
    List<DicomEntityResult> getResults(@PathParam("studyUID") String studyUID, @QueryParam("source") String source);

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    @Produces("application/json")
    List<DicomEntityResult> getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("source") String source);

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}")
    @Produces("application/json")
    List<DicomEntityResult> getResults(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("instanceUID") String instanceUID, @QueryParam("source") String source);

    @POST
    @Path("/studies")
    @Produces("application/json")
    List<DicomEntityResult> getResults(DicomQueryModel model, @QueryParam("source") String source);

}
