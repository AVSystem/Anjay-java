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

package com.avsystem.anjay;

import com.avsystem.anjay.impl.NativeBytesContext;
import java.nio.ByteBuffer;

/** Bytes context which may be used to return bytes from read handlers in {@link AnjayObject}. */
public final class AnjayBytesContext implements AutoCloseable {
    private NativeBytesContext context;

    /** Closes the context. */
    @Override
    public void close() {
        this.context.close();
        this.context = null;
    }

    private NativeBytesContext getContext() {
        if (this.context == null) {
            throw new IllegalStateException("Attempted to use closed AnjayBytesContext");
        }
        return this.context;
    }

    /**
     * Creates bytes context - it is not intended to be called by user. Use {@link
     * AnjayOutputContext#retBytes(int)} instead.
     */
    public AnjayBytesContext(NativeBytesContext context) {
        this.context = context;
    }

    /** Appends buffer to the context. */
    public void append(ByteBuffer buffer) {
        this.getContext().append(buffer);
    }

    /**
     * Appends part of the array of bytes to the context.
     *
     * @param array Array of bytes.
     * @param offset Offset in <code>array</code> to start reading from.
     * @param length Length of the data to append.
     */
    public void append(byte[] array, int offset, int length) {
        append(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Appends array of bytes to the context.
     *
     * @param array Array of bytes to append.
     */
    public void append(byte[] array) {
        append(ByteBuffer.wrap(array));
    }
}
