/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.nats;

public interface Constants {
    public enum NatsSubjectChannel {
        A, B;

        public String getChannel() {
            return this.name();
        }

        public String getPublishChannel() {
            return this.equals(A) ? B.name() : A.name();
        }
    }
}
