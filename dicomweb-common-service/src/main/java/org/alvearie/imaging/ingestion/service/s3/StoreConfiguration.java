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
    
    public enum StorageType {EPHEMERAL, S3, AZURE_BLOB};
    
    private static final String DEFAULT_EPHEMERAL_STORAGE_PATH = "/data";

    // Local storage
    private String localStoragePath;
    
    // S3
    private String awsAcessKeyID;
    private String awsSecretAccessKey;
    private String bucketHost;
    private String bucketPort;
    private String bucketRegion;
    private String bucketName;
    
    // Azure Blob
    private String azureConnectionString;
    private String azureStorageAccountName;
    private String azureStorageAccountKey;
    private String azureContainerName;
    
    private StorageType configuredStorage;

    @PostConstruct
    void init() throws IOException {
        this.bucketHost = readConfigFromFile(bucketConfigPath, "BUCKET_HOST");
        if (bucketHost != null) {
            configuredStorage = StorageType.S3;
            this.bucketPort = readConfigFromFile(bucketConfigPath, "BUCKET_PORT");
            this.bucketRegion = readConfigFromFile(bucketConfigPath, "BUCKET_REGION");
            this.bucketName = readConfigFromFile(bucketConfigPath, "BUCKET_NAME");
            this.awsAcessKeyID = readConfigFromFile(bucketSecretPath, "AWS_ACCESS_KEY_ID");
            this.awsSecretAccessKey = readConfigFromFile(bucketSecretPath, "AWS_SECRET_ACCESS_KEY");
        } else {
            this.azureConnectionString = readConfigFromFile(bucketConfigPath, "AZURE_STORAGE_CONNECTION_STRING");
            if (azureConnectionString != null) {
                configuredStorage = StorageType.AZURE_BLOB;
                this.azureContainerName = readConfigFromFile(bucketConfigPath, "AZURE_CONTAINER_NAME");
                this.azureStorageAccountName = readConfigFromFile(bucketSecretPath, "AZURE_STORAGE_ACCOUNT_NAME");
                this.azureStorageAccountKey = readConfigFromFile(bucketSecretPath, "AZURE_STORAGE_ACCOUNT_KEY");
            } else {
                configuredStorage = StorageType.EPHEMERAL;
                this.localStoragePath = readConfigFromFile(bucketConfigPath, "LOCAL_STORAGE_PATH");
                if (localStoragePath != null) {
                    this.localStoragePath = DEFAULT_EPHEMERAL_STORAGE_PATH;
                }
            }
        } 
            
    }

    private String readConfigFromFile(String base, String name) throws IOException {
        LOG.info("Reading configuration for " + name);
        String value = null;
        try {
            value = Files.readString(Paths.get(base, name));
        } catch (IOException e) {
           // ignore, no value or unreadable value, will set as an empty string 
        }
        return value == null || value.isEmpty() ? null : value.trim();
    }
    
    public StorageType getStorageType() {
        return configuredStorage;
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
    
    public String getAzureConnectionString() {
        return azureConnectionString;
    }
    
    public String getAzureStorageAccountName() {
        return azureStorageAccountName;
    }
    
    public String getAzureStorageAccountKey() {
        return azureStorageAccountKey;
    }
    
    public String getAzureContainerName() {
        return azureContainerName;
    }
}
