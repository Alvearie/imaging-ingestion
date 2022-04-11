/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.cstore.binding;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;

@ApplicationScoped
public class RestClientHelper {
    public WadoClient getClient(String url) throws IllegalStateException, RestClientDefinitionException,
            URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        return RestClientBuilder.newBuilder().baseUri(new URI(url)).followRedirects(true).build(WadoClient.class);
    }
}
