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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.transform.stream.StreamResult;

import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.service.s3.StoreService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.imageio.codec.ImageDescriptor;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.SAXTransformer;
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

    @Context
    private Request req;

    @Inject
    StoreService storeService;
    
    int[] frameList;
    Date lastModified;
    boolean clientUseCache = false;

    @GET
    @Path("/studies/{studyUID}")
    @Produces(MediaTypes.MULTIPART_RELATED)
    public void retrieveStudy(@PathParam("studyUID") String studyUID, @Suspended AsyncResponse ar) throws IOException {
        retrieve(queryClient.getResults(studyUID, source), Output.DICOM, ar);
    }

    @GET
    @Path("/studies/{studyUID}/metadata")
    @Produces({ MediaTypes.APPLICATION_DICOM_JSON, MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML })
    public void retrieveStudyMetadata(@PathParam("studyUID") String studyUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, source), getMetadataOutputType(), ar);
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
    @Produces({ MediaTypes.APPLICATION_DICOM_JSON, MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML })
    public void retrieveSeriesMetadata(@PathParam("studyUID") String studyUID, @PathParam("seriesUID") String seriesUID,
            @QueryParam("includefields") String includefields, @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, source), getMetadataOutputType(), ar);
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
    @Produces({ MediaTypes.APPLICATION_DICOM_JSON, MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML })
    public void retrieveInstanceMetadata(@PathParam("studyUID") String studyUID,
            @PathParam("seriesUID") String seriesUID, @PathParam("objectUID") String objectUID,
            @Suspended AsyncResponse ar) {
        retrieve(queryClient.getResults(studyUID, seriesUID, objectUID, source), getMetadataOutputType(), ar);
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
                try {
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
                    return output;
                } catch (IOException e) {
                    LOG.error(e);
                }
                return null;
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
                        writer.write(service.loadMetadata(rslt.getResource().getObjectName()));
                    }
                    gen.writeEnd();
                    gen.flush();

                    Date lastModified = new Date();
                    service.lastModified = lastModified;

                    return baos.toByteArray();
                } catch (IOException e) {
                    LOG.error(e);
                }
                return null;
            }
        },

        METADATA_XML(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML_TYPE, -1) {
            @Override
            public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {
                MultipartRelatedOutput output = new MultipartRelatedOutput();

                for (DicomEntityResult rslt : results) {
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Attributes metadata = service.loadMetadata(rslt.getResource().getObjectName());
                        SAXTransformer.getSAXWriter(new StreamResult(out)).write(metadata);
                        output.addPart(out.toByteArray(), MediaTypes.APPLICATION_DICOM_XML_TYPE);
                    } catch (Exception e) {
                        throw new WebApplicationException("Error retrieving metadata", Response.Status.NOT_FOUND);
                    }
                }
                Date lastModified = new Date();
                service.lastModified = lastModified;
                return output;
            }
        },
        BULKDATA_FRAME(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_TYPE, 3600 * 24) {
            @Override
            public Object buildResponse(WadoResource service, List<DicomEntityResult> results, AsyncResponse ar) {
                if (results.size() != 1) {
                    return null;
                }

                DicomEntityResult result = results.get(0);
                Date lastModified = Date.from(result.getLastModified().toInstant());

                MultipartRelatedOutput output = new MultipartRelatedOutput();
                try {
                    addDicomFramesPart(service, output, result.getResource().getObjectName(), service.frameList);
                } catch (IOException e) {
                    LOG.error(e);
                }
                service.lastModified = lastModified;
                return output;
            }

            private void addDicomFramesPart(WadoResource service, MultipartRelatedOutput output, String objectKey,
                    int[] frameList) throws IOException {
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
        Response.ResponseBuilder responseBuilder = null;
        if (response == null) {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        } else {
            if (lastModified != null) {
                responseBuilder = req.evaluatePreconditions(lastModified,
                        new EntityTag(String.valueOf(lastModified.hashCode())));
            }
            if (responseBuilder == null) {
                CacheControl cc = new CacheControl();
                cc.setNoCache(true);
                cc.setPrivate(true);
                cc.setMustRevalidate(true);
                cc.setMaxAge(output.getCacheDuration());
                responseBuilder = Response.status(Response.Status.OK).entity(response).type(output.getMediaType())
                        .cacheControl(cc).lastModified(lastModified).tag(String.valueOf(lastModified.hashCode()));
            }
        }
        ar.resume(responseBuilder.build());
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

    private Object getDicom(String objectKey) throws IOException {
        ByteArrayOutputStream baos = storeService.retrieve(objectKey);
        return baos.toByteArray();
    }

    private ByteArrayOutputStream getDicomStream(String objectKey) throws IOException {
        return storeService.retrieve(objectKey);
    }

    private Attributes loadMetadata(String objectKey) throws IOException {
        ByteArrayOutputStream baos = getDicomStream(objectKey);
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            Attributes metadata = dis.readDatasetUntilPixelData();
            String url = determineUrl(metadata);
            if (dis.tag() == Tag.PixelData) {
                metadata.setValue(Tag.PixelData, dis.vr(), new BulkData(null, url, dis.bigEndian()));
            }
            return metadata;
        }
    }

    private String determineUrl(Attributes attr) {
        StringBuffer sb = new StringBuffer(request.getRequestURL().toString());
        sb.setLength(sb.lastIndexOf("/metadata"));
        if (sb.lastIndexOf("/instances/") < 0) {
            if (sb.lastIndexOf("/series/") < 0) {
                sb.append("/series/").append(attr.getString(Tag.SeriesInstanceUID));
            }
            sb.append("/instances/").append(attr.getString(Tag.SOPInstanceUID));
        }
        return sb.toString();
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

    private Output getMetadataOutputType() {
        MediaType accept = MediaType.valueOf(request.getHeader("Accept"));

        Output output = null;
        if (MediaTypes.equalsIgnoreParameters(MediaType.WILDCARD_TYPE, accept)
                || MediaTypes.equalsIgnoreParameters(MediaTypes.APPLICATION_DICOM_JSON_TYPE, accept)) {
            output = Output.METADATA_JSON;
        } else if (MediaTypes.equalsIgnoreParameters(MediaTypes.MULTIPART_RELATED_APPLICATION_DICOM_XML_TYPE, accept)) {
            output = Output.METADATA_XML;
        } else {
            throw new WebApplicationException("Invalid Accept Header", Response.Status.BAD_REQUEST);
        }

        return output;
    }
}
