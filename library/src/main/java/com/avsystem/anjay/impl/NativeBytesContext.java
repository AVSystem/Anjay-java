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

import com.avsystem.anjay.AnjayException;
import java.nio.ByteBuffer;

public final class NativeBytesContext implements AutoCloseable {
    private long self;
    private int remaining;

    private native void init(NativeBytesContextPointer context);

    private native void cleanup();

    private native int anjayRetBytesAppend(byte[] slice, int length);

    public NativeBytesContext(NativeBytesContextPointer context, int remaining) {
        init(context);
        this.remaining = remaining;
    }

    public void append(ByteBuffer buffer) {
        if (remaining < buffer.remaining()) {
            throw new IllegalStateException("Too many bytes passed to bytes context");
        }
        ByteBuffer slice = buffer.slice();
        // TODO: is there a better way to do this?
        byte[] chunk = new byte[Math.min(4096, slice.capacity())];
        while (slice.hasRemaining()) {
            int length = Math.min(chunk.length, slice.remaining());
            slice.get(chunk, 0, length);
            this.remaining -= length;

            int result = anjayRetBytesAppend(chunk, length);
            if (result < 0) {
                throw new AnjayException(result, "anjay_ret_bytes_append() failed");
            }
        }
    }

    @Override
    public void close() {
        this.cleanup();
    }
}
