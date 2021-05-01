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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.service.s3.S3Service;
import org.alvearie.imaging.ingestion.service.s3.SimpleStoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreContext;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.imageio.codec.ImageDescriptor;
import org.dcm4che3.imageio.codec.Transcoder;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.util.StreamUtils;
import org.dcm4che3.util.StringUtils;
import org.dcm4che3.ws.rs.MediaTypes;
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

    public final static int CACHE_DURATION = 3600 * 24;

    @ConfigProperty(name = "provider.name")
    String source;

    @Inject
    @RestClient
    DicomQueryClient queryClient;

    @Context
    private HttpServletRequest request;

    @Inject
    S3Service s3Service;

    int[] frameList;
    Date lastModified;
    String eTag;
    boolean clientUseCache = false;

    @GET
    @Path("/studies/{studyUID}")
    @Produces(MediaTypes.MULTIPART_RELATED)
    public void retrieveStudy(@PathParam("studyUID") String studyUID, @Suspended AsyncResponse ar) throws IOException {
        retrieve(queryClient.getResults(studyUID, source), Output.DICOM, ar);
    }

    @GET
    @Path("/studies/{studyUID}/metadata")
    @Produces(MediaTypes.APPLICATION_DICOM_JSON)
    public void retrieveStudyMetadata(@PathParam("studyUID") String studyUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, source), Output.METADATA_JSON, ar);
    }

    @GET
    @Path("/studies/{studyUID}/rendered")
    @Produces(MediaTypes.MULTIPART_RELATED)
    public void retrieveRenderedStudy(@PathParam("studyUID") String studyUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}")
    @Produces(MediaTypes.MULTIPART_RELATED)
    public void retrieveSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, source), Output.DICOM, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/metadata")
    @Produces(MediaTypes.APPLICATION_DICOM_JSON)
    public void retrieveSeriesMetadata(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, source), Output.METADATA_JSON, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/rendered")
    public void retrieveRenderedSeries(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}")
    public void retrieveInstance(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, objectUID, source), Output.DICOM, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/metadata")
    @Produces(MediaTypes.APPLICATION_DICOM_JSON)
    public void retrieveInstanceMetadata(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, objectUID, source), Output.METADATA_JSON, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/frames/{frameList}")
    public void retrieveFrames(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @PathParam("frameList") String frameList,
            @Suspended AsyncResponse ar) {
        this.frameList = createFrameListFromPathParam(frameList);
        retrieve(queryClient.getResults(studyUID, seriesUID, objectUID, source), Output.BULKDATA_FRAME, ar);
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/frames/{frameList}/rendered")
    public void retrieveRenderedFrame(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @PathParam("frameList") String frameList,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/frames/{frameList}/thumbnail")
    public void retrieveFrameThumbnail(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @PathParam("objectUID") String objectUID, @PathParam("frameList") String frameList,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/rendered")
    public void retrieveRenderedInstance(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("annotation") String annotation, @QueryParam("quality") int quality,
            @QueryParam("viewport") String viewportSpec, @QueryParam("window") String window,
            @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    @GET
    @Path("/studies/{studyUID}/series/{seriesUID}/instances/{objectUID}/thumbnail")
    public void retrieveInstanceThumbnail(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @QueryParam("viewport") String viewportSpec, @Suspended AsyncResponse ar) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_IMPLEMENTED);
        ar.resume(responseBuilder.build());
    }

    private enum Output {
        DICOM(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_TYPE, 3600 * 24) {

            @Override
            public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {

                Date lastModified = null;
                MultipartRelatedOutput output = new MultipartRelatedOutput();

                for (DicomEntityResult rslt : results) {
                    if (lastModified == null || lastModified.toInstant().isBefore(rslt.getLastModified().toInstant())) {
                        lastModified = Date.from(rslt.getLastModified().toInstant());
                    }
                    String tsuid = service.getTransferSyntaxUID(rslt);
                    if (tsuid == null) {
                        tsuid = UID.ExplicitVRLittleEndian;
                    }
                    output.addPart(service.getDicom(rslt.getResource().getObjectName()),
                            MediaTypes.forTransferSyntax(tsuid));
                }
                service.lastModified = lastModified;
                service.eTag = Integer.toString(lastModified.hashCode());
                return output;
            }
        },

        METADATA_JSON(MediaType.APPLICATION_JSON_TYPE, -1) {

            @Override
            public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {

                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    JsonGenerator gen = Json.createGenerator(baos);
                    JSONWriter writer = new JSONWriter(gen);
                    gen.writeStartArray();
                    for (DicomEntityResult rslt : results) {
                        writer.write(loadMetadata(service, rslt.getResource().getObjectName()));
                    }
                    gen.writeEnd();
                    gen.flush();

                    Date lastModified = new Date();
                    service.lastModified = lastModified;
                    service.eTag = Integer.toString(lastModified.hashCode());

                    return baos.toByteArray();
                } catch (IOException e) {
                    LOG.error(e);
                }
                return null;
            }

            private Attributes loadMetadata(WadoResource service, String objectKey) throws IOException {
                ByteArrayOutputStream baos = service.getDicomStream(objectKey);
                try (Transcoder transcoder = new Transcoder(new ByteArrayInputStream(baos.toByteArray()))) {
                    StoreContext ctx = new SimpleStoreContext();
                    transcoder.transcode(service.new TranscoderHandler(ctx));

                    Attributes metadata = ctx.getAttributes();
                    String url = determineUrl(service, metadata);
                    replacePixelDataWithBulkDataUrl(metadata, url);

                    return metadata;
                } catch (IOException e) {
                    throw e;
                }
            }

            private String determineUrl(WadoResource service, Attributes attr) {
                StringBuffer sb = new StringBuffer(service.request.getRequestURL().toString());
                sb.setLength(sb.lastIndexOf("/metadata"));
                if (sb.lastIndexOf("/instances/") < 0) {
                    if (sb.lastIndexOf("/series/") < 0) {
                        sb.append("/series/").append(attr.getString(Tag.SeriesInstanceUID));
                    }
                    sb.append("/instances/").append(attr.getString(Tag.SOPInstanceUID));
                }
                return sb.toString();
            }

            private void replacePixelDataWithBulkDataUrl(Attributes attrs, String retrieveURL) {
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
        },

        METADATA_XML(MediaType.APPLICATION_XML_TYPE, -1) {

        },
        BULKDATA_FRAME(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_TYPE, 3600 * 24) {
            @Override
            public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {
                if (results.size() != 1) {
                    return null;
                }

                DicomEntityResult result = results.get(0);
                Date lastModified = Date.from(result.getLastModified().toInstant());

                if (service.isClientCacheValid(service.request, lastModified)) {
                    service.clientUseCache = true;
                    return null;
                }

                MultipartRelatedOutput output = new MultipartRelatedOutput();
                try {
                    addDicomFramesPart(service, output, result.getResource().getObjectName(), service.frameList);
                } catch (IOException e) {
                    LOG.error(e);
                }
                return output;
            }
            
            private void addDicomFramesPart(WadoResource service, MultipartRelatedOutput output, String objectKey, int[] frameList)
                    throws IOException {
                ByteArrayOutputStream baos = service.getDicomStream(objectKey);
                DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(baos.toByteArray()));
                ImageDescriptor imgDesc = new ImageDescriptor(dis.readDatasetUntilPixelData());
                int frameLength = imgDesc.getFrameLength();
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
                    LOG.info(String.format("Extracting frame %d (%d bytes)from inputstream at position %d", frame, frameLength,
                            offset));
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
            }
        },
        ZIP(null, -1), BULKDATA(null, 3600 * 24), BULKDATA_PATH(null, 3600 * 24),

        // Do not support any rendering
        RENDER_MULTIPART(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_TYPE, 3600 * 24),
        RENDER_FRAME_MULTIPART(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_TYPE, 3600 * 24),
        RENDER(MediaTypes.IMAGE_JPEG_TYPE, 3600 * 24), RENDER_FRAME(MediaTypes.IMAGE_JPEG_TYPE, 3600 * 24),
        THUMBNAIL(MediaTypes.IMAGE_JPEG_TYPE, 3600 * 24);

        MediaType mediaType;
        int cacheDuration;

        Output(MediaType mediaType, int cacheDuration) {
            this.mediaType = mediaType;
            this.cacheDuration = cacheDuration;
        }

        public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {
            return null;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public int getCacheDuration() {
            return cacheDuration;
        }
    }

    private void retrieve(List<DicomEntityResult> results, final Output output, AsyncResponse ar) {
        if (results == null || results.size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);
            ar.resume(responseBuilder.build());
            return;
        }
        LOG.info(String.format("Found %d instances", results.size()));

        Object response = output.buildResponse(this, results, ar);
        if (response == null) {
            if (clientUseCache) {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_MODIFIED);
                ar.resume(responseBuilder.build());
            } else {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);
                ar.resume(responseBuilder.build());
            }
        } else {
            CacheControl cc = new CacheControl();
            cc.setNoCache(true);
            cc.setPrivate(true);
            cc.setMustRevalidate(true);
            cc.setMaxAge(output.getCacheDuration());
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(response)
                    .type(output.getMediaType()).cacheControl(cc).lastModified(lastModified).tag(eTag);
            ar.resume(responseBuilder.build());
        }
    }

    public int[] createFrameListFromPathParam(String frameList) {
        String[] split = StringUtils.split(frameList, ',');
        int[] frames = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            try {
                if ((frames[i] = Integer.parseInt(split[i])) <= 0) {
                    throw new IllegalArgumentException(frameList);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(frameList);
            }
        }
        return frames;
    }

    private Object getDicom(String objectKey) {
        ByteArrayOutputStream baos = s3Service.getObject(objectKey);
        return baos.toByteArray();
    }

    private ByteArrayOutputStream getDicomStream(String objectKey) {
        return s3Service.getObject(objectKey);
    }

    private String getTransferSyntaxUID(DicomEntityResult inst) {
        return getDicomAttributeValue(inst, Tag.TransferSyntaxUID);
    }

    private String getDicomAttributeValue(DicomEntityResult inst, int tag) {
        DicomAttribute attribute = inst.getAttributes().get(String.format("%08X", tag));
        if (attribute != null && attribute.getValue() != null) {
            return attribute.getValue().get(0);
        }
        return null;
    }

    private boolean isClientCacheValid(HttpServletRequest request, Date lastModified) {
        String clientModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
        // String clientEtag = request.getHeader(HEADER_IF_NONE_MATCH);
        if (clientModifiedSince != null) {
            try {
                Date clientMillis = new Date(Long.parseLong(clientModifiedSince));
                if (clientMillis.after(lastModified)) {
                    return true;
                }
            } catch (NumberFormatException e) {

            }
        }
        return false;
    }

    public final class TranscoderHandler implements Transcoder.Handler {
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
}
