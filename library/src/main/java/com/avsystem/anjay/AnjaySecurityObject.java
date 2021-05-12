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

import com.avsystem.anjay.impl.NativeSecurityObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/** Default implementation of LwM2M Security Object (/0). */
public final class AnjaySecurityObject {
    /**
     * Possible values of the Security Mode Resource, as described in the Security Object
     * definition.
     */
    public static enum SecurityMode {
        /** Pre-Shared Key mode */
        PSK("psk"),
        /** Raw Public Key mode */
        RPK("rpk"),
        /** Certificate mode */
        CERTIFICATE("cert"),
        /** NoSec mode */
        NOSEC("nosec"),
        /** Certificate mode with EST */
        EST("est");

        @Override
        public String toString() {
            return this.name;
        }

        private final String name;

        private SecurityMode(String s) {
            name = s;
        }
    }

    /**
     * Possible values of the SMS Security Mode Resource, as described in the Security Object
     * definition.
     */
    public static enum SmsSecurityMode {
        /** DTLS in PSK mode */
        DTLS_PSK,
        /** Secure Packet Structure */
        SECURE_PACKET,
        /** NoSec mode */
        NOSEC;
    }

    /** Instance of LwM2M Security Object. */
    public static final class Instance {
        /** Short Server ID. */
        public int ssid;
        /** Resource: LwM2M Server URI */
        public Optional<String> serverUri = Optional.empty();
        /** Resource: Bootstrap Server */
        public boolean bootstrapServer;
        /** Resource: Security Mode. MUST NOT be <code>null</code>. */
        public SecurityMode securityMode;
        /** Resource: Client Hold Off Time. */
        public Optional<Integer> clientHoldoffS = Optional.empty();
        /** Resource: Bootstrap Server Account Timeout. */
        public Optional<Integer> bootstrapTimeoutS = Optional.empty();
        /** Resource: Public Key Or Identity */
        public Optional<byte[]> publicCertOrPskIdentity = Optional.empty();
        /** Resource: Secret Key */
        public Optional<byte[]> privateCertOrPskKey = Optional.empty();
        /** Resource: Server Public Key */
        public Optional<byte[]> serverPublicKey = Optional.empty();
        /** Resource: SMS Security Mode. {@link SmsSecurityMode#NOSEC} by default. */
        public SmsSecurityMode smsSecurityMode = SmsSecurityMode.NOSEC;
        /** Resource: SMS Binding Key Parameters */
        public Optional<byte[]> smsKeyParameters = Optional.empty();
        /** Resource: SMS Binding Secret Key(s) */
        public Optional<byte[]> smsSecretKey = Optional.empty();
        /** Resource: LwM2M Server SMS Number */
        public Optional<String> serverSmsNumber = Optional.empty();
    }

    private final NativeSecurityObject security;

    private AnjaySecurityObject(Anjay anjay) throws Exception {
        this.security = new NativeSecurityObject(anjay);
    }

    /**
     * Installs the Security Object in an Anjay instance.
     *
     * @param anjay Anjay instance for which the Security Object is installed.
     * @return Security object which may be used to manage its instances.
     * @throws Exception In case of error.
     */
    public static AnjaySecurityObject install(Anjay anjay) throws Exception {
        return new AnjaySecurityObject(anjay);
    }

    /**
     * Adds new Instance of Security Object and returns newly created Instance ID.
     *
     * <p>Warning: calling this function during active communication with Bootstrap Server may yield
     * undefined behavior and unexpected failures may occur.
     *
     * @param instance Security Instance to insert.
     * @return ID of the added instance.
     * @throws Exception If instance can't be added.
     */
    public int addInstance(Instance instance) throws Exception {
        return this.addInstance(instance, Anjay.ID_INVALID);
    }

    /**
     * Overload of {@link #addInstance(Instance)}, but allows to specify preferred Instance ID.
     *
     * <p>Note: if <code>preferredIid</code> is set to {@link Anjay#ID_INVALID} then the Instance ID
     * is generated automatically.
     *
     * @param instance Security Instance to insert.
     * @param preferredIid Preferred Instance ID to be set.
     * @return ID of the added instance.
     * @throws Exception If instance can't be added.
     */
    public int addInstance(Instance instance, int preferredIid) throws Exception {
        return this.security.addInstance(instance, preferredIid);
    }

    /** Purges instances of Security Object leaving it in an empty state. */
    public void purge() {
        this.security.purge();
    }

    /**
     * Dumps Security Object Instances into the <code>outputStream</code>.
     *
     * @param outputStream Stream to write to.
     * @throws Exception If instances can't be persisted.
     */
    public void persist(OutputStream outputStream) throws Exception {
        this.security.persist(outputStream);
    }

    /**
     * Attempts to restore Security Object Instances from specified <code>inputStream</code>.
     *
     * <p>Note: if restore fails, then Security Object will be left untouched, on success though all
     * Instances stored within the Object will be purged.
     *
     * @param inputStream Stream to read from.
     * @throws Exception If instances can't be restored.
     */
    public void restore(InputStream inputStream) throws Exception {
        this.security.restore(inputStream);
    }

    /**
     * Checks whether the Security Object from Anjay instance has been modified since last
     * successful call to {@link #persist persist()} or {@link #restore restore}.
     *
     * @return true if Security Object has beed modified, false otherwise.
     */
    public boolean isModified() {
        return this.security.isModified();
    }
}
