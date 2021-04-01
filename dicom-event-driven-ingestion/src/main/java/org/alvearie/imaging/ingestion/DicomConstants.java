/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion;

public interface DicomConstants {
    public static final String STUDY_INSTANCE_UID_GROUP = "0020";
    public static final String STUDY_INSTANCE_UID_ELEMENT = "000D";

    public static final String SERIES_INSTANCE_UID_GROUP = "0020";
    public static final String SERIES_INSTANCE_UID_ELEMENT = "000E";

    public static final String SOP_INSTANCE_UID_GROUP = "0008";
    public static final String SOP_INSTANCE_UID_ELEMENT = "0018";

    public static final String SOP_CLASS_UID_GROUP = "0008";
    public static final String SOP_CLASS_UID_ELEMENT = "0016";

    public static final String SERIES_NUMBER_GROUP = "0020";
    public static final String SERIES_NUMBER_ELEMENT = "0011";

    public static final String INSTANCE_NUMBER_GROUP = "0020";
    public static final String INSTANCE_NUMBER_ELEMENT = "0013";

    public static final String MODALITY_GROUP = "0008";
    public static final String MODALITY_ELEMENT = "0060";

    public static final String BODY_PART_EXAMINED_GROUP = "0018";
    public static final String BODY_PART_EXAMINED_ELEMENT = "0015";

    public static final String MANUFACTURER_GROUP = "0008";
    public static final String MANUFACTURER_ELEMENT = "0070";
}
