/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Dimse;

@ApplicationScoped
public class DimseCommandRegistry implements DimseCommandHandler {
    @Inject
    CEchoHandler echo;

    @Inject
    CStoreHandler store;

    private final HashMap<Dimse, DimseCommandHandler> handlers = new HashMap<>();

    @Override
    public byte[] onDimseRQ(Dimse dimse, SimpleAssociateRQ rq, SimplePresentationContext pc, Attributes cmd,
            Attributes data) throws Exception {
        DimseCommandHandler handler = handlers.get(dimse);
        if (handler == null) {
            throw new Exception("Handler not found");
        }

        return handler.onDimseRQ(dimse, rq, pc, cmd, data);
    }

    @PostConstruct
    public void init() {
        handlers.put(Dimse.C_ECHO_RQ, echo);
        handlers.put(Dimse.C_STORE_RQ, store);
    }
}
