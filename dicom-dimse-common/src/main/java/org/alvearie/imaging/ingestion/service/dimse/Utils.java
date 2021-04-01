/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.service.dimse;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Dimse;
import org.jboss.logging.Logger;

import io.nats.client.Message;

public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class);

    public static List<byte[]> divideArray(byte[] source, int chunkSize) {
        List<byte[]> result = new ArrayList<>();
        int start = 0;
        while (start < source.length) {
            int end = Math.min(source.length, start + chunkSize);
            result.add(Arrays.copyOfRange(source, start, end));
            start += chunkSize;
        }

        return result;
    }

    public static byte[] combineData(List<Message> source) {
        int length = 0;
        for (Message m : source) {
            length += m.getData().length;
        }

        LOG.info("Total len from parts: " + length);

        byte[] arr = new byte[length];
        ByteBuffer buff = ByteBuffer.wrap(arr);
        for (Message m : source) {
            buff.put(m.getData());
        }
        return buff.array();
    }

    public static Dimse getDimse(Attributes cmd) {
        return Dimse.valueOf(cmd.getInt(Tag.CommandField, 0));
    }

    public static boolean isServerListening(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
