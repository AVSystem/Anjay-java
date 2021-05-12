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

import com.avsystem.anjay.Anjay;
import com.avsystem.anjay.AnjayException;
import java.io.InputStream;
import java.io.OutputStream;

public class NativeAccessControl {
    private long self;

    private native void init(NativeAnjay anjay);

    private native void cleanup();

    private native void accessControlPurge();

    private native boolean accessControlIsModified();

    private native int accessControlPersist(OutputStream output_stream);

    private native int accessControlRestore(InputStream input_stream);

    private native void accessControlSetAcl(int oid, int iid, int ssid, int mask);

    public void setAcl(int oid, int iid, int ssid, int mask) {
        this.accessControlSetAcl(oid, iid, ssid, mask);
    }

    public NativeAccessControl(Anjay anjay) throws Exception {
        this.init(NativeUtils.getNativeAnjay(anjay));
    }

    public void purge() {
        this.accessControlPurge();
    }

    public boolean isModified() {
        return this.accessControlIsModified();
    }

    public void persist(OutputStream output_stream) {
        int result = this.accessControlPersist(output_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to persist Access control");
        }
    }

    public void restore(InputStream input_stream) {
        int result = this.accessControlRestore(input_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to restore Attribute storage");
        }
    }
}
