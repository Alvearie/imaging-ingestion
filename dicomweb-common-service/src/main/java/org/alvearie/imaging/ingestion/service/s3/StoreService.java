/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import java.io.IOException;
import java.io.InputStream;

public interface StoreService {
    void store(StoreContext ctx, InputStream data) throws IOException;
}
