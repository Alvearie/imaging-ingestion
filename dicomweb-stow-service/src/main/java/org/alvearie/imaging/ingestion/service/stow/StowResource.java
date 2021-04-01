/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.stow;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alvearie.imaging.ingestion.service.s3.SimpleStoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreContext;
import org.alvearie.imaging.ingestion.service.s3.StoreService;
import org.dcm4che3.mime.MultipartInputStream;
import org.dcm4che3.mime.MultipartParser;
import org.dcm4che3.ws.rs.MediaTypes;
import org.jboss.logging.Logger;

@RequestScoped
@Path("/stow-rs")
public class StowResource {
    private static final Logger LOG = Logger.getLogger(StowResource.class);

    @Inject
    StoreService storeService;

    @HeaderParam("Content-Type")
    MediaType contentType;

    @Context
    HttpServletRequest request;

    @POST
    @Path("/studies")
    @Consumes("multipart/related;type=application/dicom")
    @Produces("application/dicom+xml")
    public void storeInstances(@Suspended AsyncResponse ar, InputStream in) throws Exception {
        store(ar, in, Input.DICOM);
    }

    private void store(AsyncResponse ar, InputStream in, final Input input) throws Exception {
        logRequest();

        new MultipartParser(boundary()).parse(new BufferedInputStream(in), (partNumber, multipartInputStream) -> {
            Map<String, List<String>> headerParams = multipartInputStream.readHeaderParams();
            LOG.infof("storeInstances: Extract Part #%d%s", partNumber, headerParams);
            String contentLocation = getHeaderParamValue(headerParams, "content-location");
            String contentType = getHeaderParamValue(headerParams, "content-type");
            MediaType mediaType = normalize(MediaType.valueOf(contentType));

            try {
                if (!input.readBodyPart(StowResource.this, multipartInputStream, mediaType, contentLocation)) {
                    LOG.infof("Ignore Part with Content-Type=%s", mediaType);
                    multipartInputStream.skipAll();
                }
            } catch (Exception e) {
                throw new WebApplicationException(
                        errResponseAsTextPlain(exceptionAsString(e), Response.Status.INTERNAL_SERVER_ERROR));
            }
        });

        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK);
        ar.resume(responseBuilder.build());
    }

    private static String getHeaderParamValue(Map<String, List<String>> headerParams, String key) {
        List<String> list = headerParams.get(key);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    private static MediaType normalize(MediaType mediaType) {
        return MediaTypes.isSTLType(mediaType) ? MediaTypes.MODEL_STL_TYPE : mediaType;
    }

    private String boundary() {
        String boundary = contentType.getParameters().get("boundary");
        if (boundary == null)
            throw new WebApplicationException(errResponse("Missing Boundary Parameter", Response.Status.BAD_REQUEST));

        return boundary;
    }

    private Response errResponse(String errorMessage, Response.Status status) {
        return errResponseAsTextPlain("{\"errorMessage\":\"" + errorMessage + "\"}", status);
    }

    private static Response errResponseAsTextPlain(String errorMsg, Response.Status status) {
        LOG.warnf("Response %s caused by %s", status, errorMsg);
        return Response.status(status).entity(errorMsg).type("text/plain").build();
    }

    private static String exceptionAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void logRequest() {
        LOG.infof("Process %s %s?%s from %s@%s", request.getMethod(), request.getRequestURI(), request.getQueryString(),
                request.getRemoteUser(), request.getRemoteHost());
    }

    private enum Input {
        DICOM {
            @Override
            boolean readBodyPart(StowResource stowResource, MultipartInputStream in, MediaType mediaType,
                    String contentLocation) throws Exception {
                if (!MediaTypes.equalsIgnoreParameters(mediaType, MediaTypes.APPLICATION_DICOM_TYPE))
                    return false;

                stowResource.storeDicomObject(in);
                return true;
            }
        };

        abstract boolean readBodyPart(StowResource stowResource, MultipartInputStream in, MediaType mediaType,
                String contentLocation) throws Exception;
    }

    private void storeDicomObject(MultipartInputStream in) throws IOException {
        StoreContext ctx = new SimpleStoreContext();
        storeService.store(ctx, in);
    }
}
