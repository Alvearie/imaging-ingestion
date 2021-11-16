/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.dcm4che3.data.Attributes;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class RequestWrapper implements Serializable {
    private static final Logger LOG = Logger.getLogger(RequestWrapper.class);

    private static final long serialVersionUID = 1L;

    private SimpleAssociateRQ rq;
    private SimplePresentationContext pc;
    private Attributes cmd;
    private Attributes data;

    public RequestWrapper(SimpleAssociateRQ rq, SimplePresentationContext pc, Attributes cmd, Attributes data) {
        this.rq = rq;
        this.pc = pc;
        this.cmd = cmd;
        this.data = data;
    }

    public RequestWrapper(byte[] bytes) {
        RequestWrapper in = fromBytes(bytes);
        if (in != null) {
            this.rq = in.getAssociateRQ();
            this.pc = in.getPresentationContext();
            this.cmd = in.getCmd();
            this.data = in.getData();
        } else {
            LOG.error("De-serialization failed for RequestWrapper");
        }
    }

    public SimpleAssociateRQ getAssociateRQ() {
        return rq;
    }

    public SimplePresentationContext getPresentationContext() {
        return pc;
    }

    public Attributes getCmd() {
        return cmd;
    }

    public Attributes getData() {
        return data;
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }

    private RequestWrapper fromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            RequestWrapper o = (RequestWrapper) ois.readObject();
            return o;
        } catch (Exception e) {
            LOG.error("Failed to read object", e);
            return null;
        }
    }
}
