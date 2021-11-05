/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse.proxy;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.pdu.AAssociateAC;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.UserIdentityAC;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DimseAssociationHandler extends AssociationHandler {
    private static final Logger LOG = Logger.getLogger(DimseAssociationHandler.class);

    @ConfigProperty(name = "dimse.target.ae")
    String targetAe;

    @Inject
    NatsAssociationPublisher associationPublisher;

    @Override
    protected AAssociateAC makeAAssociateAC(Association as, AAssociateRQ rq, UserIdentityAC userIdentity)
            throws IOException {
        LOG.info("Association ID: " + as.getSerialNo());

        associationPublisher.onAssociation(as);

        return super.makeAAssociateAC(as, rq, userIdentity);
    }

}
