/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.AsyncStreamingOutput;

@ApplicationScoped
public class RenderService {
    private static final Logger LOG = Logger.getLogger(RenderService.class);

    private ImageReader reader = null;

    public AsyncStreamingOutput render(InputStream is) {
        try {
            DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
            ImageInputStream iis = ImageIO.createImageInputStream(is);
            reader.setInput(iis, false);
            BufferedImage bi = reader.read(0, param);
            iis.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpeg", baos);

            return new AsyncStreamingOutput() {
                @Override
                public CompletionStage<Void> asyncWrite(AsyncOutputStream output) {
                    return output.asyncWrite(baos.toByteArray());
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to render image");
        }
    }

    private ImageReader getDicomImageReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
        if (!readers.hasNext()) {
            ImageIO.scanForPlugins();
            readers = ImageIO.getImageReadersByFormatName("DICOM");
            if (!readers.hasNext())
                throw new RuntimeException("DICOM Image Reader not registered");
        }
        return readers.next();
    }

    @PostConstruct
    void init() {
        reader = getDicomImageReader();
        LOG.info("Init completed");
    }
}
