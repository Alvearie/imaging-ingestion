/*
 * (C) Copyright IBM Corp. 2021
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.alvearie.imaging.ingestion.service.nats.NatsMessageSubscriber;
import org.dcm4che3.net.Association;

@ApplicationScoped
public class ActiveAssociationHolder {
    private Map<String, Association> associations = new ConcurrentHashMap<>();
    private Map<String, NatsMessageSubscriber> subscribers = new ConcurrentHashMap<>();

    public boolean isActiveAssociation(String key) {
        return associations.containsKey(key);
    }

    public Association getAssociation(String key) {
        return associations.get(key);
    }

    public void addAssociation(String key, Association as) {
        associations.put(key, as);
    }

    public void removeAssociation(String key) {
        associations.remove(key);
    }

    public boolean isActiveSubscriber(String key) {
        return subscribers.containsKey(key);
    }

    public NatsMessageSubscriber getSubscriber(String key) {
        return subscribers.get(key);
    }

    public void addSubscriber(String key, NatsMessageSubscriber as) {
        subscribers.put(key, as);
    }

    public void removeSubscriber(String key) {
        NatsMessageSubscriber sub = subscribers.remove(key);
        if (sub != null) {
            sub.close();
        }
    }
}
