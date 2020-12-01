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

import com.avsystem.anjay.Anjay.Objlnk;
import com.avsystem.anjay.impl.NativeInputContext;
import com.avsystem.anjay.impl.NativeInputContextPointer;
import java.nio.ByteBuffer;

/** Context from which values sent by LwM2M Server can be read. */
public final class AnjayInputContext implements AutoCloseable {
    private NativeInputContext context;

    /** Closes the context - it is not intended to be called by user. */
    @Override
    public void close() {
        this.context = null;
    }

    private NativeInputContext getContext() {
        if (this.context == null) {
            throw new IllegalStateException("Attempted to use closed AnjayInputContext");
        }
        return this.context;
    }

    /** Creates input context - it is not intended to be called by user. */
    public AnjayInputContext(NativeInputContextPointer pointer) {
        this.context = new NativeInputContext(pointer);
    }

    /**
     * Reads an integer from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public int getInt() throws AnjayException {
        return this.getContext().getInt();
    }

    /**
     * Reads a long integer from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public long getLong() throws AnjayException {
        return this.getContext().getLong();
    }

    /**
     * Reads a float value from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public float getFloat() throws AnjayException {
        return this.getContext().getFloat();
    }

    /**
     * Reads a double value from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public double getDouble() throws AnjayException {
        return this.getContext().getDouble();
    }

    /**
     * Reads a boolean value from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public boolean getBoolean() throws AnjayException {
        return this.getContext().getBoolean();
    }

    /**
     * Reads a string from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public String getString() throws AnjayException {
        return this.getContext().getString();
    }

    /**
     * Reads an Object Link value from the RPC request content.
     *
     * @return Value read.
     * @throws AnjayException In case of failure.
     */
    public Objlnk getObjlnk() throws AnjayException {
        return this.getContext().getObjlnk();
    }

    /**
     * Reads bytes chunk from the RPC request content. This function should be called until it
     * returns <code>true</code> to read all bytes.
     *
     * @param out Buffer to read bytes to.
     * @return <code>true</code> if all bytes have been read, false otherwise.
     * @throws AnjayException In case of failure.
     */
    public boolean getBytes(ByteBuffer out) throws AnjayException {
        return this.getContext().getBytes(out);
    }

    /**
     * Reads all bytes from the RPC request content.
     *
     * @return Array of bytes.
     * @throws AnjayException In case of failure.
     */
    public byte[] getAllBytes() throws AnjayException {
        return this.getContext().getAllBytes();
    }
}
