/*
 * Copyright 2020 AVSystem <avsystem@avsystem.com>
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

import com.avsystem.anjay.Anjay;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Duration;

public final class NativeUtils {
    public static NativeAnjay getNativeAnjay(Anjay anjay) throws Exception {
        Field nativeAnjayField = Anjay.class.getDeclaredField("anjay");
        nativeAnjayField.setAccessible(true);
        NativeAnjay nativeAnjay = (NativeAnjay) nativeAnjayField.get(anjay);
        nativeAnjay.ensureValidState();
        return nativeAnjay;
    }

    public static class ReadyState {
        public boolean read;
        public boolean write;
        public boolean connect;
        public boolean accept;

        public ReadyState(boolean read, boolean write, boolean connect, boolean accept) {
            this.read = read;
            this.write = write;
            this.connect = connect;
            this.accept = accept;
        }

        public ReadyState() {
            this(false, false, false, false);
        }
    }

    public static ReadyState waitUntilReady(
            SelectableChannel channel, Duration timeout, ReadyState waitStates) throws IOException {
        try (Selector selector = Selector.open()) {
            int waitMask = 0;
            if (waitStates.read) {
                waitMask |= SelectionKey.OP_READ;
            }
            if (waitStates.write) {
                waitMask |= SelectionKey.OP_WRITE;
            }
            if (waitStates.connect) {
                waitMask |= SelectionKey.OP_CONNECT;
            }
            if (waitStates.accept) {
                waitMask |= SelectionKey.OP_ACCEPT;
            }

            SelectionKey key = channel.register(selector, waitMask);
            // NOTE: Java doesn't seem to have any higher level APIs around monotonic
            // clock... The standard available monotonic time source is System.nanoTime().
            final long deadlineNs = System.nanoTime() + timeout.toNanos();
            long remainingMs;
            int readySockets;
            do {
                remainingMs = (deadlineNs - System.nanoTime()) / 1_000_000;
                if (remainingMs <= 0) {
                    readySockets = selector.selectNow();
                } else {
                    readySockets = selector.select(remainingMs);
                }
            } while (readySockets == 0 && remainingMs > 0);

            ReadyState result = new ReadyState();
            if (selector.selectedKeys().contains(key)) {
                result.read = key.isReadable();
                result.write = key.isWritable();
                result.connect = key.isConnectable();
                result.accept = key.isAcceptable();
            }
            return result;
        }
    }
}
