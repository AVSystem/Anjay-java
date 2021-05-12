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

import com.avsystem.anjay.Anjay.Objlnk;
import com.avsystem.anjay.AnjayException;

public final class NativeOutputContext implements AutoCloseable {
    private long self;

    private native void init(NativeOutputContextPointer context);

    private native void cleanup();

    private native int anjayRetString(String value);

    private native int anjayRetI32(int value);

    private native int anjayRetI64(long value);

    private native int anjayRetFloat(float value);

    private native int anjayRetDouble(double value);

    private native int anjayRetBool(boolean value);

    private native int anjayRetObjlnk(Objlnk objlnk);

    private native NativeBytesContextPointer anjayRetBytesBegin(int length);

    public NativeOutputContext(NativeOutputContextPointer context) {
        init(context);
    }

    public void retInt(int value) {
        final int result = anjayRetI32(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_i32() failed");
        }
    }

    public void retLong(long value) {
        final int result = anjayRetI64(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_i64() failed");
        }
    }

    public void retFloat(float value) {
        final int result = anjayRetFloat(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_float() failed");
        }
    }

    public void retDouble(double value) {
        final int result = anjayRetDouble(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_double() failed");
        }
    }

    public void retBoolean(boolean value) {
        final int result = anjayRetBool(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_bool() failed");
        }
    }

    public void retString(String value) {
        final int result = anjayRetString(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_string() failed");
        }
    }

    public NativeBytesContext retBytes(int length) {
        NativeBytesContextPointer pointer = anjayRetBytesBegin(length);
        if (pointer.isNull()) {
            throw new AnjayException(-1, "anjay_ret_bytes_begin() failed");
        }
        return new NativeBytesContext(pointer, length);
    }

    public void retObjlnk(Objlnk value) {
        final int result = anjayRetObjlnk(value);
        if (result < 0) {
            throw new AnjayException(result, "anjay_ret_objlnk() failed");
        }
    }

    @Override
    public void close() {
        this.cleanup();
    }
}
