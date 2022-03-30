/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ApplicationScoped
public class S3Service extends PersistenceService {
    private static final Logger LOG = Logger.getLogger(S3Service.class);

    S3Client s3;

    @Override
    public ByteArrayOutputStream getObject(String objectKey) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        GetObjectRequest request = GetObjectRequest.builder().bucket(config.getBucketName()).key(objectKey).build();
        s3.getObject(request, ResponseTransformer.toOutputStream(baos));

        return baos;
    }

    @Override
    public void putObject(StoreContext ctx) throws NoSuchAlgorithmException, IOException {
        File file = new File(ctx.getFilePath());

        String sha256 = super.getContentChecksum(MessageDigest.getInstance("SHA-256"), file);
        String key = sha256 + ".dcm";

        ctx.setObjectName(key);

        LOG.infof("Put %s to object store", key);
        PutObjectRequest request = PutObjectRequest.builder().bucket(config.getBucketName()).key(key)
                .contentType("application/octet-stream").build();
        PutObjectResponse response = s3.putObject(request, Paths.get(ctx.getFilePath()));

        if (response == null) {
            throw new IOException("Error storing to object store");
        }
        LOG.infof("%s stored in object store", key);
    }

    @PostConstruct
    void init() throws URISyntaxException {
        AwsCredentials credentials = AwsBasicCredentials.create(config.getAwsAcessKeyID(),
                config.getAwsSecretAccessKey());
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        String hostPort = config.getBucketHost() + ":" + config.getBucketPort();
        if (!hostPort.startsWith("http://") && !hostPort.startsWith("https://")) {
            if ("443".equals(config.getBucketPort())) {
                hostPort = "https://" + hostPort;
            } else if ("80".equals(config.getBucketPort())) {
                hostPort = "http://" + hostPort;
            } else {
                hostPort = "https://" + hostPort;
            }
        }

        s3 = S3Client.builder().credentialsProvider(credentialsProvider).endpointOverride(new URI(hostPort))
                .region(Region.of(config.getBucketRegion())).build();
    }
}
