/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StoreConfiguration {
    private static final Logger LOG = Logger.getLogger(StoreConfiguration.class);

    @ConfigProperty(name = "bucket.config.path")
    String bucketConfigPath;

    @ConfigProperty(name = "bucket.secret.path")
    String bucketSecretPath;

    @ConfigProperty(name = "persistence.local", defaultValue = "false")
    Boolean localStorage;

    private String localStoragePath;
    private String awsAcessKeyID;
    private String awsSecretAccessKey;
    private String bucketHost;
    private String bucketPort;
    private String bucketRegion;
    private String bucketName;

    @PostConstruct
    void init() throws IOException {
        this.bucketName = readConfigFromFile(bucketConfigPath, "BUCKET_NAME");
        if (localStorage) {
            this.localStoragePath = readConfigFromFile(bucketConfigPath, "LOCAL_STORAGE_PATH");
        } else {
            this.awsAcessKeyID = readConfigFromFile(bucketSecretPath, "AWS_ACCESS_KEY_ID");
            this.awsSecretAccessKey = readConfigFromFile(bucketSecretPath, "AWS_SECRET_ACCESS_KEY");

            this.bucketHost = readConfigFromFile(bucketConfigPath, "BUCKET_HOST");
            this.bucketPort = readConfigFromFile(bucketConfigPath, "BUCKET_PORT");
            this.bucketRegion = readConfigFromFile(bucketConfigPath, "BUCKET_REGION");
        }
    }

    private String readConfigFromFile(String base, String name) throws IOException {
        LOG.info("Reading configuration from " + name);
        return Files.readString(Paths.get(base, name)).trim();
    }

    public boolean isLocalStorage() {
        return localStorage;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }

    public String getAwsAcessKeyID() {
        return awsAcessKeyID;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public String getBucketHost() {
        return bucketHost;
    }

    public String getBucketPort() {
        return bucketPort;
    }

    public String getBucketRegion() {
        return bucketRegion;
    }

    public String getBucketName() {
        return bucketName;
    }
}
