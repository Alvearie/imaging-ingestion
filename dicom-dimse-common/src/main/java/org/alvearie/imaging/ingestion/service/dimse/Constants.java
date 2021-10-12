/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

public interface Constants {
    public enum Actor {
        PROXY(0), SERVER(1);

        private int direction;

        Actor(int direction) {
            this.direction = direction;
        }

        public int getDirection() {
            return this.direction;
        }

        public int getPublishDirection() {
            return this.direction == 0 ? 1 : 0;
        }
    }
}
