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

import com.avsystem.anjay.impl.NativeServerObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/** Default implementation of LwM2M Server Object (/1). */
public final class AnjayServerObject {

    /** Instance of LwM2M Server Object. */
    public static class Instance {
        /** Resource: Short Server ID */
        public int ssid;
        /** Resource: Lifetime */
        public int lifetime;
        /** Resource: Default Minimum Period */
        public Optional<Integer> defaultMinPeriod = Optional.empty();
        /** Resource: Default Maximum Period */
        public Optional<Integer> defaultMaxPeriod = Optional.empty();
        /** Resource: Disable Timeout */
        public Optional<Integer> disableTimeout = Optional.empty();
        /** Resource: Binding. MUST NOT be <code>null</code>. */
        public String binding;
        /** Resource: Notification Storing When Disabled or Offline */
        public boolean notificationStoring;
    }

    private final NativeServerObject server;

    private AnjayServerObject(Anjay anjay) throws Exception {
        this.server = new NativeServerObject(anjay);
    }

    /**
     * Installs the Server Object in an Anjay instance.
     *
     * @param anjay Anjay instance for which the Server Object is installed.
     * @return Server object which may be used to manage its instances.
     * @throws Exception In case of error.
     */
    public static AnjayServerObject install(Anjay anjay) throws Exception {
        return new AnjayServerObject(anjay);
    }

    /**
     * Adds new Instance of Server Object and returns newly created Instance ID.
     *
     * @param instance Server Instance to insert.
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
     * @param instance Server Instance to insert.
     * @param preferredIid Preferred Instance ID to be set.
     * @return ID of the added instance.
     * @throws Exception If instance can't be added.
     */
    public int addInstance(Instance instance, int preferredIid) throws Exception {
        return this.server.addInstance(instance, preferredIid);
    }

    /** Removes all instances of Server Object leaving it in an empty state. */
    public void purge() {
        this.server.purge();
    }

    /**
     * Dumps Server Object Instances into the <code>outputStream</code>.
     *
     * @param outputStream Stream to write to.
     * @throws Exception If instances can't be persisted.
     */
    public void persist(OutputStream outputStream) throws Exception {
        this.server.persist(outputStream);
    }

    /**
     * Attempts to restore Server Object Instances from specified <code>inputStream</code>.
     *
     * <p>Note: if restore fails, then Server Object will be left untouched, on success though all
     * Instances stored within the Object will be purged.
     *
     * @param inputStream Stream to read from.
     * @throws Exception If instances can't be restored.
     */
    public void restore(InputStream inputStream) throws Exception {
        this.server.restore(inputStream);
    }

    /**
     * Checks whether the Server Object from Anjay instance has been modified since last successful
     * call to {@link #persist persist()} or {@link #restore restore}.
     *
     * @return true if Server Object has beed modified, false otherwise.
     */
    public boolean isModified() {
        return this.server.isModified();
    }
}
