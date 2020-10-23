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
import com.avsystem.anjay.AnjayException;
import com.avsystem.anjay.AnjaySecurityObject.Instance;
import java.io.InputStream;
import java.io.OutputStream;

public final class NativeSecurityObject {
    private long self;

    private native void init(NativeAnjay anjay);

    private native void cleanup();

    private native int anjaySecurityObjectAddInstance(Instance instance, int preferredIid);

    private native void anjaySecurityObjectPurge();

    private native int anjaySecurityObjectPersist(OutputStream output_stream);

    private native int anjaySecurityObjectRestore(InputStream input_stream);

    private native boolean anjaySecurityObjectIsModified();

    public NativeSecurityObject(Anjay anjay) throws Exception {
        this.init(NativeUtils.getNativeAnjay(anjay));
    }

    public int addInstance(Instance instance, int preferredIid) {
        int result = this.anjaySecurityObjectAddInstance(instance, preferredIid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_security_add_instance() failed");
        }
        return result;
    }

    public void purge() {
        this.anjaySecurityObjectPurge();
    }

    public void persist(OutputStream output_stream) {
        int result = this.anjaySecurityObjectPersist(output_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to persist Security object");
        }
    }

    public void restore(InputStream input_stream) {
        int result = this.anjaySecurityObjectRestore(input_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to restore Security object");
        }
    }

    public boolean isModified() {
        return this.anjaySecurityObjectIsModified();
    }
}
