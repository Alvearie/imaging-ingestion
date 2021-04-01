/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import org.dcm4che3.data.Attributes;

import io.nats.client.impl.SocketDataPort;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { SocketDataPort.class, Attributes.class })
public class ReflectionConfiguration {

}
