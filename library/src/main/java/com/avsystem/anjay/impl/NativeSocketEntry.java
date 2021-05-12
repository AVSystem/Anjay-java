/*
 * Copyright 2020-2021 AVSystem <avsystem@avsystem.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.avsystem.anjay.impl;

import com.avsystem.anjay.Anjay.SocketEntry;
import com.avsystem.anjay.Anjay.Transport;
import java.nio.channels.SelectableChannel;

public final class NativeSocketEntry {
    private final Transport transport;
    private final SelectableChannel channel;
    private final long socketPtr;
    private final int ssid;
    private final boolean queueMode;
    private final int port;

    public long getSocketPtr() {
        return this.socketPtr;
    }

    public SocketEntry intoSocketEntry() {
        return new SocketEntry(this.channel, this.transport, this.ssid, this.queueMode, this.port);
    }

    private NativeSocketEntry(
            Transport transport,
            SelectableChannel channel,
            long socketPtr,
            int ssid,
            boolean queueMode,
            int port) {
        this.transport = transport;
        this.channel = channel;
        this.socketPtr = socketPtr;
        this.ssid = ssid;
        this.queueMode = queueMode;
        this.port = port;
    }
}
