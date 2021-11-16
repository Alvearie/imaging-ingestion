/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dcm4che3.net.pdu.PresentationContext;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class SimpleAssociateRQ implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<SimplePresentationContext> pcs = new ArrayList<>();

    public SimpleAssociateRQ(String id, List<PresentationContext> pcs) {
        this.id = id;
        if (pcs != null) {
            for (PresentationContext pc : pcs) {
                this.pcs.add(new SimplePresentationContext(pc.getPCID(), pc.getResult(), pc.getAbstractSyntax(),
                        pc.getTransferSyntaxes()));
            }
        }
    }

    public String getId() {
        return id;
    }

    public List<SimplePresentationContext> getPresentationContexts() {
        return Collections.unmodifiableList(pcs);
    }
}
