/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.event;

public class Element {
    String group;
    String element;
    String name;
    String value;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
