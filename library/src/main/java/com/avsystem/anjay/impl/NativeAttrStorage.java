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
import com.avsystem.anjay.AnjayAttributes.ObjectInstanceAttrs;
import com.avsystem.anjay.AnjayAttributes.ResourceAttrs;
import com.avsystem.anjay.AnjayException;
import java.io.InputStream;
import java.io.OutputStream;

public class NativeAttrStorage {
    private long self;

    private native void init(NativeAnjay anjay);

    private native void cleanup();

    private native void attrStoragePurge();

    private native void attrStorageSetObjectAttrs(int ssid, int oid, ObjectInstanceAttrs attrs);

    private native void attrStorageSetInstanceAttrs(
            int ssid, int oid, int iid, ObjectInstanceAttrs attrs);

    private native void attrStorageSetResourceAttrs(
            int ssid, int oid, int iid, int rid, ResourceAttrs attrs);

    private native int attrStoragePersist(OutputStream output_stream);

    private native int attrStorageRestore(InputStream input_stream);

    private native boolean attrStorageIsModified();

    public static native int getAttrPeriodNone();

    public static native double getAttrValueNone();

    public NativeAttrStorage(Anjay anjay) throws Exception {
        this.init(NativeUtils.getNativeAnjay(anjay));
    }

    public void purge() {
        this.attrStoragePurge();
    }

    public void setObjectAttrs(int ssid, int oid, ObjectInstanceAttrs attrs) {
        this.attrStorageSetObjectAttrs(ssid, oid, attrs);
    }

    public void setInstanceAttrs(int ssid, int oid, int iid, ObjectInstanceAttrs attrs) {
        this.attrStorageSetInstanceAttrs(ssid, oid, iid, attrs);
    }

    public void setResourceAttrs(int ssid, int oid, int iid, int rid, ResourceAttrs attrs) {
        this.attrStorageSetResourceAttrs(ssid, oid, iid, rid, attrs);
    }

    public void persist(OutputStream output_stream) {
        int result = this.attrStoragePersist(output_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to persist Attribute storage");
        }
    }

    public void restore(InputStream input_stream) {
        int result = this.attrStorageRestore(input_stream);
        if (result < 0) {
            throw new AnjayException(result, "failed to restore Attribute storage");
        }
    }

    public boolean isModified() {
        return this.attrStorageIsModified();
    }
}
