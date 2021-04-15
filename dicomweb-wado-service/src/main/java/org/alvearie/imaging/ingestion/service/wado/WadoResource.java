/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedOutput;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.AsyncStreamingOutput;

@RequestScoped
@Path("/wado-rs")
public class WadoResource {
    private static final Logger LOG = Logger.getLogger(WadoResource.class);

    public final static MediaType MULTIPART_RELATED_TYPE = new MediaType("multipart", "related");
    public final static String MULTIPART_RELATED = "multipart/related";
    public final static MediaType APPLICATION_DICOM_TYPE = new MediaType("application", "dicom");
    public final static String IMAGE_JPEG = "image/jpeg";
    public final static MediaType IMAGE_JPEG_TYPE = new MediaType("image", "jpeg");
    
    public final static int THUMBNAIL_WIDTH = 100;
    public final static int THUMBNAIL_HEIGHT = 150;

    @Inject
    S3Service s3Service;

    @Inject
    RenderService renderService;

    @Inject
    @RestClient
    DicomQueryClient queryClient;

    @GET
    @Path("/studies/{studyUID}")
    @Produces("multipart/related")
    public void retrieveStudy(@PathParam("studyUID") String studyUID, @Suspended AsyncResponse ar) throws IOException {
        buildDicomResponse(queryClient.getInstances(studyUID), ar);
    }

    @GET
    @Path("/studies/{studyUID}/rendered")
    @Produces("multipart/related")
    public void retrieveRenderedStudy(@PathParam("studyUID") String studyUID, @QueryParam("annotation") String annotation,
            @QueryParam("quality") int quality, @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window, @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getInstances(studyUID), viewport, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    @Produces("multipart/related")
    public void retrieveSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @Suspended AsyncResponse ar) {
        buildDicomResponse(queryClient.getInstances(studyUID, seriesUID), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/rendered")
    public void retrieveRenderedSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window, @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getInstances(studyUID, seriesUID), viewport, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}")
    public void retrieveInstance(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @Suspended AsyncResponse ar) {
        buildDicomResponse(queryClient.getInstances(studyUID, seriesUID, objectUID), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/rendered")
    public void retrieveRenderedInstance(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window, @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getInstances(studyUID, seriesUID, objectUID), viewport, ar);
    }
    
    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/thumbnail")
    public void retrieveRenderedThumbnailInstance(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("viewport") String viewportSpec,
            @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        if (viewport == null) {
            viewport = renderService.createViewport();
            viewport.vw = THUMBNAIL_WIDTH;
            viewport.vh = THUMBNAIL_HEIGHT;
        } else {
            viewport.sx = viewport.sy = viewport.sw = viewport.sh = 0;
        }
        buildRenderedResponse(queryClient.getInstances(studyUID, seriesUID, objectUID), viewport, ar);
    }

    private void buildRenderedResponse(List<String> instances, RenderService.Viewport viewport, AsyncResponse ar) {
        if (instances == null || instances.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }

        LOG.info(String.format("Found %d instances", instances.size()));

        Date lastModified = new Date();
        if (instances.size() == 1) {
            try {
                Object output = getRenderedImage(instances.get(0), viewport);
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).lastModified(lastModified)
                        .tag(String.valueOf(lastModified.hashCode())).entity(output).type(IMAGE_JPEG_TYPE);
                ar.resume(responseBuilder.build());
            } catch (UnsupportedOperationException e) {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
                ar.resume(responseBuilder.build());
            }
        } else {

            MultipartRelatedOutput output = new MultipartRelatedOutput();

            for (String inst : instances) {
                addRenderedPart(output, inst, viewport);
            }

            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).lastModified(lastModified)
                    .tag(String.valueOf(lastModified.hashCode())).entity(output).type(MULTIPART_RELATED_TYPE);
            ar.resume(responseBuilder.build());
        }
    }

    private void buildDicomResponse(List<String> instances, AsyncResponse ar) {
        if (instances == null || instances.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }

        LOG.info(String.format("Found %d instances", instances.size()));

        Date lastModified = new Date();
        MultipartRelatedOutput output = new MultipartRelatedOutput();

        for (String inst : instances) {
            addDicomPart(output, inst);
        }

        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).lastModified(lastModified)
                .tag(String.valueOf(lastModified.hashCode())).entity(output).type(MULTIPART_RELATED_TYPE);
        ar.resume(responseBuilder.build());
    }

    private void addDicomPart(MultipartRelatedOutput output, String objectKey) {
        output.addPart(getDicom(objectKey), APPLICATION_DICOM_TYPE);
    }

    private Object getDicom(String objectKey) {
        return new AsyncStreamingOutput() {
            @Override
            public CompletionStage<Void> asyncWrite(AsyncOutputStream output) {
                ByteArrayOutputStream baos = s3Service.getObject(objectKey);
                return output.asyncWrite(baos.toByteArray());
            }
        };
    }

    private void addRenderedPart(MultipartRelatedOutput output, String objectKey, RenderService.Viewport viewport) {
        output.addPart(getRenderedImage(objectKey, viewport), IMAGE_JPEG_TYPE);
    }
    
    private Object getRenderedImage(String objectKey,  RenderService.Viewport viewport) {
        ByteArrayOutputStream baos = s3Service.getObject(objectKey);
        return renderService.render(new ByteArrayInputStream(baos.toByteArray()), viewport);
    }
    
    public RenderService.Viewport creatViewportFromQueryParam(String viewPortSpec) {
        RenderService.Viewport viewport = null;
        if (viewPortSpec != null) {
            String[] segments = viewPortSpec.split(",");
            if (segments.length >= 2) {
                viewport = renderService.createViewport();
                try {
                 viewport.vw = Integer.valueOf(segments[0]);
                 viewport.vh = Integer.valueOf(segments[1]);
                 if (segments.length == 4) {
                     viewport.sx = Integer.valueOf(segments[2]);
                     viewport.sy = Integer.valueOf(segments[3]);
                 } else if (segments.length == 6) {
                     viewport.sw = Integer.valueOf(segments[4]);
                     viewport.sh = Integer.valueOf(segments[5]);
                     if (segments[2] != null) {
                         viewport.sx = Integer.valueOf(segments[2]);
                     }
                     if (segments[3] != null) {
                         viewport.sy = Integer.valueOf(segments[3]);
                     }
                 }
                } catch (NumberFormatException e) {
                    viewport = null;
                }
            }
        }
        return viewport;
    }
}
