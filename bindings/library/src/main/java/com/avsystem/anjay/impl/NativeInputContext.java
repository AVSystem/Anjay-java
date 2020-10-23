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

import com.avsystem.anjay.Anjay.Objlnk;
import com.avsystem.anjay.AnjayException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class NativeInputContext implements AutoCloseable {
    private long self;

    private native void init(NativeInputContextPointer context);

    private native void cleanup();

    private static final class BytesContext {
        public boolean messageFinished;
        // NOTE: actually used, on C++ side.
        @SuppressWarnings("unused")
        public ByteBuffer slice;

        public int bytesRead;

        public BytesContext(ByteBuffer slice) {
            this.messageFinished = false;
            this.slice = slice;
            this.bytesRead = 0;
        }
    }

    private static final class StringContext {
        public String value;
    }

    private static final class BooleanContext {
        public boolean value;
    }

    private static final class IntegerContext {
        public int value;
    }

    private static final class LongContext {
        public long value;
    }

    private static final class FloatContext {
        public float value;
    }

    private static final class DoubleContext {
        public double value;
    }

    private static final class ObjlnkContext {
        public Objlnk value;
    }

    private native int anjayGetBytes(BytesContext bytesContext);

    private native int anjayGetString(StringContext stringContext);

    private native int anjayGetI32(IntegerContext intContext);

    private native int anjayGetI64(LongContext intContext);

    private native int anjayGetFloat(FloatContext floatContext);

    private native int anjayGetDouble(DoubleContext floatContext);

    private native int anjayGetBool(BooleanContext boolContext);

    private native int anjayGetObjlnk(ObjlnkContext objlnkContext);

    public NativeInputContext(NativeInputContextPointer context) {
        init(context);
    }

    public int getInt() {
        final IntegerContext context = new IntegerContext();
        final int result = anjayGetI32(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_i32() failed");
        }
        return context.value;
    }

    public long getLong() {
        final LongContext context = new LongContext();
        final int result = anjayGetI64(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_i64() failed");
        }
        return context.value;
    }

    public float getFloat() {
        final FloatContext context = new FloatContext();
        final int result = anjayGetFloat(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_float() failed");
        }
        return context.value;
    }

    public double getDouble() {
        final DoubleContext context = new DoubleContext();
        final int result = anjayGetDouble(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_double() failed");
        }
        return context.value;
    }

    public boolean getBoolean() {
        final BooleanContext context = new BooleanContext();
        final int result = anjayGetBool(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_bool() failed");
        }
        return context.value;
    }

    public String getString() {
        final StringContext context = new StringContext();
        final int result = anjayGetString(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_string() failed");
        }
        return context.value;
    }

    public Objlnk getObjlnk() {
        final ObjlnkContext context = new ObjlnkContext();
        final int result = anjayGetObjlnk(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_objlnk() failed");
        }
        return context.value;
    }

    public boolean getBytes(ByteBuffer out) {
        final int position = out.position();
        final BytesContext context = new BytesContext(out.slice());
        int result = anjayGetBytes(context);
        if (result < 0) {
            throw new AnjayException(result, "anjay_get_bytes() failed");
        }
        out.position(position + context.bytesRead);
        return context.messageFinished;
    }

    public byte[] getAllBytes() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(4096);

        boolean finished = false;
        while (!finished) {
            buf.clear();
            finished = this.getBytes(buf);
            output.write(buf.array(), 0, buf.position());
        }

        return output.toByteArray();
    }

    @Override
    public void close() {
        this.cleanup();
    }
}
