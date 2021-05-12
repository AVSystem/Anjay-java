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

package com.avsystem.anjay;

import com.avsystem.anjay.impl.NativeAccessControl;
import java.io.InputStream;
import java.io.OutputStream;

/** Access Control module. */
public final class AnjayAccessControl {
    /**
     * Possible values of an ACL Resource Instance which can be combined together, as described in
     * the Access Control definition.
     */
    public static final class AccessMask {
        /** 1st LSB set - Read, Observe and Write-Attributes rights */
        public static final int READ = (1 << 0);
        /** 2nd LSB set - Write right */
        public static final int WRITE = (1 << 1);
        /** 3rd LSB set - Execute right */
        public static final int EXECUTE = (1 << 2);
        /** 4th LSB set - Delete right */
        public static final int DELETE = (1 << 3);
        /** 5th LSB set - Create right */
        public static final int CREATE = (1 << 4);

        /** All bits set - full rights */
        public static final int FULL = READ | WRITE | EXECUTE | DELETE | CREATE;
        /** No bits set - no rights */
        public static final int NONE = 0;
    }

    private final NativeAccessControl accessControl;

    private AnjayAccessControl(Anjay anjay) throws Exception {
        this.accessControl = new NativeAccessControl(anjay);
    }

    /**
     * Installs the Access Control Object in an Anjay instance.
     *
     * @param anjay Anjay instance for which the Access Control Object is installed.
     * @return Access Control Object.
     * @throws Exception In case of failure.
     */
    public static AnjayAccessControl install(Anjay anjay) throws Exception {
        return new AnjayAccessControl(anjay);
    }

    /**
     * Creates a new instance of the Access Control Object or updates an existing instance if access
     * rights of a particular Object Instance have already been set.
     *
     * @param oid Object ID of an Object to create an ACL for.
     * @param iid Instance ID of an Object Instance to create an ACL for.
     * @param ssid SSID of a Server Object for which access rights are valid.
     * @param mask A number which defines access rights for a specified Object Instance.
     */
    public void setAcl(int oid, int iid, int ssid, int mask) {
        this.accessControl.setAcl(oid, iid, ssid, mask);
    }

    /** Removes all existing access rights leaving Access Control Object without any instances. */
    public void purge() {
        this.accessControl.purge();
    }

    /**
     * Dumps all set access rights to the output stream.
     *
     * @param outputStream Stream to write to.
     * @throws Exception If the persist operation failed.
     */
    public void persist(OutputStream outputStream) throws Exception {
        this.accessControl.persist(outputStream);
    }

    /**
     * Attempts to restore access rights from specified input stream.
     *
     * @param inputStream Stream to read from.
     * @throws Exception If the restoration failed.
     */
    public void restore(InputStream inputStream) throws Exception {
        this.accessControl.restore(inputStream);
    }

    /**
     * Checks whether the Access Control has been modified since last successful call to {@link
     * #persist persist()} or {@link #restore restore()}.
     *
     * @return true if Access Control has been modified, false otherwise.
     */
    public boolean isModified() {
        return this.accessControl.isModified();
    }
}
