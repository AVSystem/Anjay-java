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
import com.avsystem.anjay.impl.NativeOutputContext;
import com.avsystem.anjay.impl.NativeOutputContextPointer;
import java.nio.ByteBuffer;

/** Context which is used to send values to the LwM2M Server. */
public final class AnjayOutputContext implements AutoCloseable {
    private NativeOutputContext context;

    /** Closes the context - it is not intended to be called by user. */
    @Override
    public void close() {
        this.context = null;
    }

    private NativeOutputContext getContext() {
        if (this.context == null) {
            throw new IllegalStateException("Attempted to use closed AnjayOutputContext");
        }
        return this.context;
    }

    /** Creates output context - it is not intended to be called by user. */
    public AnjayOutputContext(NativeOutputContextPointer pointer) {
        this.context = new NativeOutputContext(pointer);
    }

    /**
     * Returns an integer from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retInt(int value) throws AnjayException {
        this.getContext().retInt(value);
    }

    /**
     * Returns a long integer from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retLong(long value) throws AnjayException {
        this.getContext().retLong(value);
    }

    /**
     * Returns a float value from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retFloat(float value) throws AnjayException {
        this.getContext().retFloat(value);
    }

    /**
     * Returns a double value from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retDouble(double value) throws AnjayException {
        this.getContext().retDouble(value);
    }

    /**
     * Returns a boolean value from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retBoolean(boolean value) throws AnjayException {
        this.getContext().retBoolean(value);
    }

    /**
     * Returns a string from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retString(String value) throws AnjayException {
        this.getContext().retString(value);
    }

    /**
     * Returns an Object Link value from the data model handler.
     *
     * @param value The value to return.
     * @throws AnjayException In case of failure.
     */
    public void retObjlnk(Objlnk value) throws AnjayException {
        this.getContext().retObjlnk(value);
    }

    /**
     * Creates {@link AnjayBytesContext} which may be used to return bytes from the data model.
     *
     * @param length Number of bytes to return.
     * @throws AnjayException In case of failure.
     */
    public AnjayBytesContext retBytes(int length) throws AnjayException {
        return new AnjayBytesContext(this.getContext().retBytes(length));
    }

    /**
     * Returns bytes from the data model handler.
     *
     * @param buffer Bytes to return.
     * @throws AnjayException In case of failure.
     */
    public void retBytes(ByteBuffer buffer) throws AnjayException {
        ByteBuffer slice = buffer.slice();
        try (AnjayBytesContext context = retBytes(slice.capacity())) {
            context.append(slice);
        }
    }

    /**
     * Returns bytes from the data model handler.
     *
     * @param array Array of bytes.
     * @param offset Offset in <code>array</code> to start reading from.
     * @param length Length of the data to return.
     * @throws AnjayException In case of failure.
     */
    public void retBytes(byte[] array, int offset, int length) throws AnjayException {
        this.retBytes(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Returns bytes from the data model handler.
     *
     * @param array Array of bytes to return.
     * @throws AnjayException In case of failure.
     */
    public void retBytes(byte[] array) throws AnjayException {
        this.retBytes(ByteBuffer.wrap(array));
    }
}
