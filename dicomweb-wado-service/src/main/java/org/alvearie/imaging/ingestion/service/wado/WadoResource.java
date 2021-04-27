/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.alvearie.imaging.ingestion.service.s3.SimpleStoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreContext;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.imageio.codec.ImageDescriptor;
import org.dcm4che3.imageio.codec.Transcoder;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.util.StreamUtils;
import org.dcm4che3.util.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedOutput;

@RequestScoped
@Path("/wado-rs")
@GZIP
public class WadoResource {
    private static final Logger LOG = Logger.getLogger(WadoResource.class);

    public final static MediaType MULTIPART_RELATED_TYPE = new MediaType("multipart", "related");
    public final static String MULTIPART_RELATED = "multipart/related";
    public final static MediaType APPLICATION_DICOM_TYPE = new MediaType("application", "dicom");
    public final static String IMAGE_JPEG = "image/jpeg";
    public final static MediaType IMAGE_JPEG_TYPE = new MediaType("image", "jpeg");
    public final static MediaType APPLICATION_DICOM_JSON_TYPE = new MediaType("application", "dicom+json");
    public final static String APPLICATION_DICOM_JSON = "application/dicom+json";

    public final static int THUMBNAIL_WIDTH = 100;
    public final static int THUMBNAIL_HEIGHT = 150;

    @ConfigProperty(name = "provider.name")
    String source;

    @Inject
    S3Service s3Service;

    @Inject
    RenderService renderService;

    @Inject
    @RestClient
    DicomQueryClient queryClient;

    @Context
    private HttpServletRequest request;

    @GET
    @Path("/studies/{studyUID}")
    @Produces("multipart/related")
    public void retrieveStudy(@PathParam("studyUID") String studyUID, @Suspended AsyncResponse ar) throws IOException {
        buildDicomResponse(queryClient.getResults(studyUID, source), null, ar);
    }

    @GET
    @Path("/studies/{studyUID}/metadata")
    @Produces(APPLICATION_DICOM_JSON)
    public void retrieveStudyMetadata(@PathParam("studyUID") String studyUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        buildMetadataResponse(queryClient.getResults(studyUID, source), ar);
    }

    @GET
    @Path("/studies/{studyUID}/rendered")
    @Produces("multipart/related")
    public void retrieveRenderedStudy(@PathParam("studyUID") String studyUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getResults(studyUID, source), viewport, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    @Produces("multipart/related")
    public void retrieveSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @Suspended AsyncResponse ar) {
        buildDicomResponse(queryClient.getResults(studyUID, seriesUID), null, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/metadata")
    @Produces(APPLICATION_DICOM_JSON)
    public void retrieveSeriesMetadata(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        buildMetadataResponse(queryClient.getResults(studyUID, seriesUID), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/rendered")
    public void retrieveRenderedSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getResults(studyUID, seriesUID), viewport, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}")
    public void retrieveInstance(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @Suspended AsyncResponse ar) {
        buildDicomResponse(queryClient.getResults(studyUID, seriesUID, objectUID), null, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/metadata")
    @Produces(APPLICATION_DICOM_JSON)
    public void retrieveInstanceMetadata(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @Suspended AsyncResponse ar) {
        buildMetadataResponse(queryClient.getResults(studyUID, seriesUID, objectUID), ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/frames/{frameList}")
    public void retrieveFrames(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @PathParam("frameList") String frameList,
            @Suspended AsyncResponse ar) {
        buildDicomResponse(queryClient.getResults(studyUID, seriesUID, objectUID), new FrameList(frameList).frames, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/frames/{frameList}/rendered")
    public void retrieveRenderedFrame(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @PathParam("frameList") String frameList,
            @Suspended AsyncResponse ar) {
        buildRenderedFrameResponse(queryClient.getResults(studyUID, seriesUID, objectUID), null,
                new FrameList(frameList).frames, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/rendered")
    public void retrieveRenderedInstance(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        buildRenderedResponse(queryClient.getResults(studyUID, seriesUID, objectUID), viewport, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/thumbnail")
    public void retrieveRenderedThumbnailInstance(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("viewport") String viewportSpec, @Suspended AsyncResponse ar) {
        RenderService.Viewport viewport = creatViewportFromQueryParam(viewportSpec);
        if (viewport == null) {
            viewport = renderService.createViewport();
            viewport.vw = THUMBNAIL_WIDTH;
            viewport.vh = THUMBNAIL_HEIGHT;
        }
        viewport.sx = viewport.sy = viewport.sw = viewport.sh = 0;
        buildRenderedResponse(queryClient.getResults(studyUID, seriesUID, objectUID), viewport, ar);
    }

    private void buildRenderedResponse(List<DicomEntityResult> results, RenderService.Viewport viewport,
            AsyncResponse ar) {
        if (results == null || results.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }

        LOG.info(String.format("Found %d instances", results.size()));

        Date lastModified = new Date();
        if (results.size() == 1) {
            try {
                Object output = getRenderedImage(results.get(0).getResource().getObjectName(), viewport);
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
                        .lastModified(lastModified).tag(String.valueOf(lastModified.hashCode())).entity(output)
                        .type(IMAGE_JPEG_TYPE);
                ar.resume(responseBuilder.build());
            } catch (UnsupportedOperationException e) {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
                ar.resume(responseBuilder.build());
            }
        } else {

            MultipartRelatedOutput output = new MultipartRelatedOutput();

            for (DicomEntityResult rslt : results) {
                addRenderedPart(output, rslt.getResource().getObjectName(), viewport);
            }

            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).lastModified(lastModified)
                    .tag(String.valueOf(lastModified.hashCode())).entity(output).type(MULTIPART_RELATED_TYPE);
            ar.resume(responseBuilder.build());
        }
    }

    private void buildDicomResponse(List<DicomEntityResult> results, int[] frameList, AsyncResponse ar) {
        if (results == null || results.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }

        LOG.info(String.format("Found %d instances", results.size()));

        Date lastModified = new Date();
        MultipartRelatedOutput output = new MultipartRelatedOutput();

        try {
            for (DicomEntityResult rslt : results) {
                addDicomPart(output, rslt.getResource().getObjectName(), frameList);
            }
        } catch (IOException e) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NO_CONTENT);
            ar.resume(responseBuilder.build());
            return;
        }

        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).lastModified(lastModified)
                .tag(String.valueOf(lastModified.hashCode())).entity(output).type(MULTIPART_RELATED_TYPE);
        ar.resume(responseBuilder.build());
    }

    private void buildMetadataResponse(List<DicomEntityResult> results, AsyncResponse ar) {
        if (results == null || results.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }

        LOG.info(String.format("Found %d instances", results.size()));

        Object output = writeMetadataJSON(results);
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(output)
                .type(MediaType.APPLICATION_JSON);
        ar.resume(responseBuilder.build());
    }

    private Object writeMetadataJSON(List<DicomEntityResult> results) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            JsonGenerator gen = Json.createGenerator(baos);
            JSONWriter writer = new JSONWriter(gen);
            gen.writeStartArray();
            for (DicomEntityResult rslt : results) {
                writer.write(loadMetadata(rslt.getResource().getObjectName()));
            }
            gen.writeEnd();
            gen.flush();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new WebApplicationException(exceptionAsString(e), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Attributes loadMetadata(String objectKey) throws IOException {
        ByteArrayOutputStream baos = s3Service.getObject(objectKey);
        try (Transcoder transcoder = new Transcoder(new ByteArrayInputStream(baos.toByteArray()))) {
            StoreContext ctx = new SimpleStoreContext();
            transcoder.transcode(new TranscoderHandler(ctx));

            Attributes metadata = ctx.getAttributes();
            StringBuffer sb = request.getRequestURL();
            sb.setLength(sb.lastIndexOf("/metadata"));
            mkInstanceURL(sb, metadata);
            setBulkdataURI(metadata, sb.toString());

            return metadata;
        } catch (IOException e) {
            throw e;
        }
    }

    private void setBulkdataURI(Attributes attrs, String retrieveURL) {
        try {
            attrs.accept(new Attributes.ItemPointerVisitor() {
                @Override
                public boolean visit(Attributes attrs, int tag, VR vr, Object value) {
                    if (tag == Tag.PixelData) {
                        attrs.setValue(tag, vr, new BulkData(null, retrieveURL, false));
                    }
                    return true;
                }
            }, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mkInstanceURL(StringBuffer sb, Attributes attr) {
        if (sb.lastIndexOf("/instances/") < 0) {
            if (sb.lastIndexOf("/series/") < 0) {
                sb.append("/series/").append(attr.getString(Tag.SeriesInstanceUID));
            }
            sb.append("/instances/").append(attr.getString(Tag.SOPInstanceUID));
        }
    }

    private void addDicomPart(MultipartRelatedOutput output, String objectKey, int[] frameList) throws IOException {
        if (frameList == null || frameList.length == 0) {
            output.addPart(getDicom(objectKey), APPLICATION_DICOM_TYPE);
        } else {
            addDicomFramesPart(output, objectKey, frameList);
        }
    }

    private void addDicomFramesPart(MultipartRelatedOutput output, String objectKey, int[] frameList)
            throws IOException {
        try {
            ByteArrayOutputStream baos = s3Service.getObject(objectKey);
            DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(baos.toByteArray()));

            int frameLength = new ImageDescriptor(dis.readDatasetUntilPixelData()).getFrameLength();
            if (dis.tag() != Tag.PixelData) {
                throw new IOException("Missing pixel data in requested object");
            }

            int frame = 1;
            for (int nextFrame : frameList) {
                while (frame < nextFrame) {
                    dis.skip(frameLength);
                    frame++;
                }

                long offset = dis.getPosition();
                LOG.info(String.format("Extracting frame %d (%d bytes)from inputstream at position %d", frame,
                        frameLength, offset));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    StreamUtils.copy(dis, out, frameLength);
                } catch (EOFException e) {
                    LOG.error(String.format(
                            "Error loading data frame, more data expected. Current offset %d. Expected length of at least %d",
                            dis.getPosition(), offset + frameLength));
                    throw e;
                }
                frame++;

                output.addPart(out.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE);
            }
        } catch (Exception e) {
            throw new WebApplicationException(exceptionAsString(e), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void buildRenderedFrameResponse(List<DicomEntityResult> results, RenderService.Viewport viewport,
            int[] frameList, AsyncResponse ar) {
        if (results == null || results.size() == 0 || frameList == null) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }
        if (results.size() == 1) {
            Date lastModified = new Date();
            try {
                ByteArrayOutputStream baos = s3Service.getObject(results.get(0).getResource().getObjectName());
                DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(baos.toByteArray()));

                int frameLength = new ImageDescriptor(dis.readDatasetUntilPixelData()).getFrameLength();
                if (dis.tag() != Tag.PixelData) {
                    throw new IOException("Missing pixel data in requested object");
                }

                int frame = 1;
                while (frame < frameList[0]) {
                    dis.skip(frameLength);
                    frame++;
                }
                if (frameList.length == 1) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    StreamUtils.copy(dis, out, frameLength);
                    Object frameOutput = renderService.render(new ByteArrayInputStream(out.toByteArray()), viewport);
                    Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
                            .lastModified(lastModified).tag(String.valueOf(lastModified.hashCode())).entity(frameOutput)
                            .type(IMAGE_JPEG_TYPE);
                    ar.resume(responseBuilder.build());
                } else {
                    MultipartRelatedOutput output = new MultipartRelatedOutput();
                    for (int requestedFrame : frameList) {
                        while (frame < requestedFrame) {
                            dis.skip(frameLength);
                            frame++;
                        }
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        StreamUtils.copy(dis, out, frameLength);
                        Object frameOutput = renderService.render(new ByteArrayInputStream(out.toByteArray()),
                                viewport);

                        output.addPart(frameOutput, IMAGE_JPEG_TYPE);
                    }
                    Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
                            .lastModified(lastModified).tag(String.valueOf(lastModified.hashCode())).entity(output)
                            .type(MULTIPART_RELATED_TYPE);
                    ar.resume(responseBuilder.build());
                }
            } catch (IOException e) {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NO_CONTENT);
                ar.resume(responseBuilder.build());
            }
        }
    }

    private Object getDicom(String objectKey) {
        ByteArrayOutputStream baos = s3Service.getObject(objectKey);
        return baos.toByteArray();
    }

    private void addRenderedPart(MultipartRelatedOutput output, String objectKey, RenderService.Viewport viewport) {
        output.addPart(getRenderedImage(objectKey, viewport), IMAGE_JPEG_TYPE);
    }

    private Object getRenderedImage(String objectKey, RenderService.Viewport viewport) {
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

    private static String exceptionAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private final class TranscoderHandler implements Transcoder.Handler {
        private final StoreContext storeContext;

        private TranscoderHandler(StoreContext storeContext) {
            this.storeContext = storeContext;
        }

        @Override
        public OutputStream newOutputStream(Transcoder transcoder, Attributes dataset) throws IOException {
            storeContext.setAttributes(dataset);
            return OutputStream.nullOutputStream();
        }
    }

    public static class FrameList {
        final int[] frames;

        public FrameList(String s) {
            String[] split = StringUtils.split(s, ',');
            int[] frames = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                if ((frames[i] = Integer.parseInt(split[i])) <= 0)
                    throw new IllegalArgumentException(s);
            }
            this.frames = frames;
        }
    }
}
