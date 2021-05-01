/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.s3;

import org.dcm4che3.data.Attributes;

public interface StoreContext {
    Attributes getAttributes();

    void setAttributes(Attributes dataset);

    String getFilePath();

    void setFilePath(String path);

    String getObjectName();

    void setObjectName(String name);
    
    String getTransferSyntaxUID();
    
    void setTransferSyntaxUID(String tsuid);
}
