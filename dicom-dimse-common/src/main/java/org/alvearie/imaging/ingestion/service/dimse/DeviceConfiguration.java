/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DeviceConfiguration {
    private static final Logger LOG = Logger.getLogger(DeviceConfiguration.class);

    @ConfigProperty(name = "dimse.config.path")
    String configPath;
    
    public static final String DEFAULT_UID_SET = "/etc/config/dimse-default";

    private List<String> imageCuids;
    private List<String> imageTsuids;

    @PostConstruct
    void init() throws IOException {
        LOG.info("Inside init");
        if (Files.notExists(Paths.get(configPath), LinkOption.NOFOLLOW_LINKS)) {
            LOG.warn("No CUIDS or TSUIDS provided.  Will try and use the default set from " + DEFAULT_UID_SET);
            configPath = DEFAULT_UID_SET;
        }
        try {
            this.imageCuids = readUIDsFromFile(configPath, "IMAGE_CUIDS");
            this.imageTsuids = readUIDsFromFile(configPath, "IMAGE_TSUIDS");
        } catch (IOException e) {
            LOG.error("Unable to load the CUIDS and TSUIDS sets");
            throw e;
        }
    }

    private List<String> readUIDsFromFile(String base, String name) throws IOException {
        List<String> result = new ArrayList<>();
        String content = Files.readString(Paths.get(base, name));
        if (content != null) {
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        LOG.info("Adding " + parts[1]);
                        result.add(parts[1]);
                    }
                }
            }
        }

        return result;
    }

    public List<String> getImageCuids() {
        return imageCuids;
    }

    public List<String> getImageTsuids() {
        return imageTsuids;
    }
}
