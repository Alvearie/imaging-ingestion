/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.wado;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.jboss.logging.Logger;

@RequestScoped
public class RenderService {
    private static final Logger LOG = Logger.getLogger(RenderService.class);

    private ImageReader reader = null;

    public class Viewport {
        // image-width, image-height, origin-left, origin-top, region-width,
        // region-height
        public int vw = 0, vh = 0, sx = 0, sy = 0, sw = 0, sh = 0;

        public Viewport() {
        }

        @Override
        public String toString() {
            return String.format("vw: %d vh: %d sx: %d sy: %d sw: %d sh %d", vw, vh, sx, sy, sw, sh);
        }
    }

    public Viewport createViewport() {
        return new Viewport();
    }

    public byte[] render(InputStream is, Viewport viewport) {
        try {
            BufferedImage bi = null;
            try {
                DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
                ImageInputStream iis = ImageIO.createImageInputStream(is);
                reader.setInput(iis, false);
                bi = reader.read(0, param);
                iis.close();
            } catch (UnsupportedOperationException e) {
                LOG.error("Unable to load the image udue to limitation in GraalVM, try running in a non-native mode");
                throw e;
            }

            if (viewport != null) {
                LOG.info("Transforming to viewport " + viewport.toString());
                // Crop first
                if (viewport.sx != 0 || viewport.sy != 0 || viewport.sw != 0 || viewport.sh != 0) {
                    bi = crop(bi, viewport.sx, viewport.sy, viewport.sw, viewport.sh);

                }
                // scale
                if (viewport.vw != 0 && viewport.vh != 0) {
                    bi = scale(bi, viewport.vw, viewport.vh);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpeg", baos);

            return baos.toByteArray();
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

    private BufferedImage crop(BufferedImage sourceImage, int sx, int sy, int sw, int sh) {
        if (sw == 0) {
            sw = sourceImage.getWidth() - sx;
        }
        if (sh == 0) {
            sh = sourceImage.getHeight() - sy;
        }
        LOG.info(String.format("Cropping source of %dx%d to size %dx%d starting at %dx%d", sourceImage.getWidth(),
                sourceImage.getHeight(), sw, sh, sx, sy));
        BufferedImage croppedImage = new BufferedImage(sw, sh, sourceImage.getType());
        try {
            for (int x = sx; x < sx + sw; ++x) {
                for (int y = sy; y < sy + sh; ++y) {
                    int pixel = sourceImage.getRGB(x, y);
                    croppedImage.setRGB(x - sx, y - sy, pixel);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return sourceImage;
        }
        return croppedImage;
    }

    private BufferedImage scale(BufferedImage sourceImage, int width, int height) {
        if (sourceImage.getWidth() <= 0 || sourceImage.getHeight() <= 0 || width == 0 || height == 0) {
            return sourceImage;
        }
        BufferedImage scaledImage = new BufferedImage(width, height, sourceImage.getType());
        LOG.info(String.format("Scaling from %dx%d to %dx%d", sourceImage.getWidth(), sourceImage.getHeight(), width,
                height));
        AffineTransform transform = AffineTransform.getScaleInstance(
                ((double) scaledImage.getWidth()) / ((double) sourceImage.getWidth()),
                ((double) scaledImage.getHeight()) / ((double) sourceImage.getHeight()));
        AffineTransformOp transformOperation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        scaledImage = transformOperation.filter(sourceImage, scaledImage);
        return scaledImage;
    }

    @PostConstruct
    void init() {
        reader = getDicomImageReader();

        LOG.info("Init completed");
    }
}
