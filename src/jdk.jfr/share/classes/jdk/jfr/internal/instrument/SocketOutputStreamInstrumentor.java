/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.internal.instrument;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import jdk.jfr.events.EventConfigurations;
import jdk.jfr.events.SocketWriteEvent;
import jdk.jfr.internal.event.EventConfiguration;

/**
 * See {@link JITracer} for an explanation of this code.
 */
@JIInstrumentationTarget("java.net.Socket$SocketOutputStream")
final class SocketOutputStreamInstrumentor {

    private SocketOutputStreamInstrumentor() {
    }

    @JIInstrumentationMethod
    public void write(byte b[], int off, int len) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_WRITE;
        if (!eventConfiguration.isEnabled()) {
            write(b, off, len);
            return;
        }
        int bytesWritten = 0;
        long start = 0;
        try {
            start = EventConfiguration.timestamp();
            write(b, off, len);
            bytesWritten = len;
        } finally {
            long duration = EventConfiguration.timestamp() - start;
            if (eventConfiguration.shouldCommit(duration)) {
                InetAddress remote = parent.getInetAddress();
                SocketWriteEvent.commit(
                        start,
                        duration,
                        remote.getHostName(),
                        remote.getHostAddress(),
                        parent.getPort(),
                        bytesWritten);
            }
        }
    }

    // private field in java.net.Socket$SocketOutputStream
    private Socket parent;
}
