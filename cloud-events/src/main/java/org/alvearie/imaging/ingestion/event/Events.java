/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public interface Events {
    public static final String EventSpecVersion = "1.0";
    public static final String ImageStoredEvent = "ImageStoredEvent";
    public static final String DicomAvailableEvent = "DicomAvailableEvent";
    public static final String StudyRevisionEvent = "StudyRevisionEvent";
    public static final String EventSource = "ImagingIngestion";
}
