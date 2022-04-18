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
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DimseCommandRegistry implements DimseCommandHandler {
    private static final Logger LOG = Logger.getLogger(DimseCommandRegistry.class);

    @Inject
    CEchoHandler echo;

    @Inject
    CStoreHandler store;

    @Inject
    ActiveAssociationHolder holder;

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

    @Override
    public void onClose(SimpleAssociateRQ arq) throws Exception {
        if (holder.isActiveAssociation(arq.getId())) {
            Association as = holder.getAssociation(arq.getId());
            if (as != null) {
                try {
                    if (as.isReadyForDataTransfer()) {
                        as.release();
                    }
                    as.waitForSocketClose();
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }
}
