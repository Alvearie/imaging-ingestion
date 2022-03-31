/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.StorageSharedKeyCredential;

import org.jboss.logging.Logger;

@ApplicationScoped
public class AzureBlobService extends PersistenceService {
    private static final Logger LOG = Logger.getLogger(AzureBlobService.class);

   
    BlobContainerClient blobContainerClient;

    @Override
    ByteArrayOutputStream getObject(String objectKey) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            LOG.infof("Beginning retrieval of %s from blob store", objectKey);
            BlockBlobClient blobClient = blobContainerClient.getBlobClient(objectKey).getBlockBlobClient();
            blobClient.downloadStream(baos);
            LOG.infof("Completed retrieval of %s from blob store", objectKey);
        } finally {
            baos.close();
            LOG.infof("Failed retrieving of %s from blob store", objectKey);
        }
        return baos;
    }

    @Override
    void putObject(StoreContext ctx) throws NoSuchAlgorithmException, IOException {
        File file = new File(ctx.getFilePath());

        String sha256 = super.getContentChecksum(MessageDigest.getInstance("SHA-256"), file);
        String objectKey = sha256 + ".dcm";

        ctx.setObjectName(objectKey);

        LOG.infof("Beginning storage of %s to blob store", objectKey);
        BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);
        blobClient.uploadFromFile(ctx.getFilePath(), true);
        LOG.infof("Completed storage of %s to blob store", objectKey);
    }

    @PostConstruct
    void init() throws URISyntaxException {
        String endpoint = config.getAzureConnectionString();
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(config.getAzureStorageAccountName(),
                config.getAzureStorageAccountKey());

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(credential)
                .buildClient();
        blobContainerClient = blobServiceClient.getBlobContainerClient(config.getAzureContainerName());
    }

}
