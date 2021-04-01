/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

@ApplicationScoped
public class LocalFileService extends PersistenceService {
    private static final Logger LOG = Logger.getLogger(LocalFileService.class);

    @Override
    ByteArrayOutputStream getObject(String objectKey) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        File storedFile = Paths.get(config.getLocalStoragePath(), config.getBucketName(), objectKey).toFile();
        if (storedFile.exists() && storedFile.canRead()) {
            FileInputStream fis = new FileInputStream(storedFile);
            try {
                fis.transferTo(baos);
            } finally {
                fis.close();
            }

        }
        return baos;
    }

    @Override
    void putObject(StoreContext ctx) throws NoSuchAlgorithmException, IOException {
        File file = new File(ctx.getFilePath());

        String sha256 = super.getContentChecksum(MessageDigest.getInstance("SHA-256"), file);
        String key = sha256 + ".dcm";

        ctx.setObjectName(key);

        File bucketDir = Paths.get(config.getLocalStoragePath(), config.getBucketName()).toFile();
        if (!bucketDir.exists()) {
            bucketDir.mkdir();
        }

        LOG.debugf("Storing %s to %s", Paths.get(ctx.getFilePath()), Paths.get(bucketDir.getPath(), key));

        Files.copy(Paths.get(ctx.getFilePath()), Paths.get(bucketDir.getPath(), key),
                StandardCopyOption.REPLACE_EXISTING);
    }

}
