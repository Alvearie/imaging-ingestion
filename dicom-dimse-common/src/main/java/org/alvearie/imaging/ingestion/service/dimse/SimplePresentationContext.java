/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.io.Serializable;
import java.util.Arrays;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class SimplePresentationContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int pcid;
    private final int result;
    private final String as;
    private final String[] tss;

    public SimplePresentationContext(int pcid, int result, String as, String... tss) {
        this.pcid = pcid;
        this.result = result;
        this.as = as;
        this.tss = tss;
    }

    public final int getPCID() {
        return pcid;
    }

    public final int getResult() {
        return result;
    }

    public final String getAbstractSyntax() {
        return as;
    }

    public final String[] getTransferSyntaxes() {
        return tss;
    }

    public String getTransferSyntax() {
        return tss[0];
    }

    @Override
    public String toString() {
        return "SimplePresentationContext [pcid=" + pcid + ", result=" + result + ", as=" + as + ", tss="
                + Arrays.toString(tss) + "]";
    }
}
