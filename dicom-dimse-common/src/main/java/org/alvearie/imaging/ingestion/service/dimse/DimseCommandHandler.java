/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Dimse;

public interface DimseCommandHandler {
    public byte[] onDimseRQ(Dimse dimse, SimpleAssociateRQ rq, SimplePresentationContext pc, Attributes cmd,
            Attributes data) throws Exception;

    public void onClose(SimpleAssociateRQ arq) throws Exception;
}
