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

import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Interface specifying handlers for operations on Object's Instances and Resources.
 *
 * <p>LwM2M Object may also implement {@link AnjayObjectAttrHandlers} interface.
 */
public interface AnjayObject {
    /** Kind of a Resource - indicates allowed operations. */
    public static enum ResourceKind {
        /**
         * Read-only Single-Instance Resource. Bootstrap Server might attempt to write to it anyway.
         */
        R,

        /** Write-only Single-Instance Resource. */
        W,

        /** Read/Write Single-Instance Resource. */
        RW,

        /**
         * Read-only Multiple Instance Resource. Bootstrap Server might attempt to write to it
         * anyway.
         */
        RM,

        /** Write-only Multiple Instance Resource. */
        WM,

        /** Read/Write Multiple Instance Resource. */
        RWM,

        /** Executable Resource. */
        E,

        /** Resource that can be read/written only by Bootstrap server. */
        BS_RW;
    }

    /** Object's Resource definition. */
    public static final class ResourceDef implements Comparable<ResourceDef> {
        /** Resource ID. */
        public final int rid;
        /** Kind of the resource, indicating allowed operations on resource. */
        public final ResourceKind kind;
        /** Indicates if the resource is currently present. */
        public final boolean present;

        /**
         * Creates Resource definition object.
         *
         * @param rid Resource ID.
         * @param kind Resource kind.
         * @param present True if resource is present.
         */
        public ResourceDef(int rid, ResourceKind kind, boolean present) {
            this.rid = rid;
            this.kind = kind;
            this.present = present;
        }

        /**
         * Compares this object with the specified object for order. Returns a negative integer,
         * zero, or a positive integer as this object is less than, equal to, or greater than the
         * specified object.
         */
        @Override
        public int compareTo(ResourceDef other) {
            return this.rid - other.rid;
        }

        /** Checks if this object and the specified object are equal. */
        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || !(other instanceof ResourceDef)) {
                return false;
            }
            ResourceDef def = (ResourceDef) other;
            return def.rid == this.rid && def.kind.equals(this.kind) && def.present == this.present;
        }

        /** Returns hash code value of the object. */
        @Override
        public int hashCode() {
            int hash = 31 * this.rid;
            hash = 31 * hash + kind.hashCode();
            hash = 31 * hash + Boolean.hashCode(this.present);
            return hash;
        }
    }

    /**
     * A handler that returns Object ID.
     *
     * @return Object ID.
     */
    int oid();

    /**
     * A handler that returns version of the object.
     *
     * @return Version of the object as string. If not implemented, <code>"1.0"</code> is returned
     *     by default.
     */
    default String version() {
        return "1.0";
    }

    /**
     * A handler that returns all Object Instances for the Object.
     *
     * @return {@link SortedSet} with Instance IDs.
     */
    SortedSet<Integer> instances();

    /**
     * A handler that shall reset Object Instance to its default (after creational) state.
     *
     * <p>Note: if this handler is not implemented, then non-partial write on the Object Instance
     * will not succeed.
     *
     * @param iid Instance ID to reset.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void instanceReset(int iid) throws Exception {
        throw new UnsupportedOperationException("instanceReset is not implemented");
    }

    /**
     * A handler that creates an Object Instance.
     *
     * <p>Note: if this handler is not implemented, LwM2M Server won't be able to create a new
     * object instance.
     *
     * @param iid Instance ID to create, chosen either by the server or the library. An ID that has
     *     been previously checked to not be PRESENT is guaranteed to be passed.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void instanceCreate(int iid) throws Exception {
        throw new UnsupportedOperationException("instanceCreate is not implemented");
    }

    /**
     * A handler that removes an Object Instance with given Instance ID.
     *
     * <p>Note: if this handler is not implemented, LwM2M Server won't be able to remove an object
     * instance.
     *
     * @param iid Checked Object Instance ID.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void instanceRemove(int iid) throws Exception {
        throw new UnsupportedOperationException("instanceRemove is not implemented");
    }

    /**
     * A handler that returns SUPPORTED Resources for an Object Instance, called only if the Object
     * Instance is PRESENT (has recently been returned via {@link #instances}).
     *
     * @param iid Object Instance ID.
     * @return {@link SortedSet} with Resource definitions.
     */
    default SortedSet<ResourceDef> resources(int iid) {
        return null;
    }

    /**
     * A handler that reads the Resource value, called only if the Resource is PRESENT and is one of
     * the {@link ResourceKind#R} or {@link ResourceKind#RW} kinds (as returned by {@link
     * resources}).
     *
     * <p>If not implemented, Method Not Allowed CoAP code is sent in response.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param context Output context to write the resource value to. One of the <code>ret*</code>
     *     methods <strong>MUST</strong> be called in this handler before returning successfully.
     *     Failure to do so will result in 5.00 Internal Server Error being sent to the server.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceRead(int iid, int rid, AnjayOutputContext context) throws Exception {
        throw new UnsupportedOperationException("resourceRead on Resource is not implemented");
    }

    /**
     * A handler that reads the Resource Instance value, called only if the Resource is PRESENT and
     * is one of the {@link ResourceKind#RM} or {@link ResourceKind#RWM} kinds (as returned by
     * {@link resources}) and <code>riid</code> was returned by {@link resourceInstances}.
     *
     * <p>If not implemented, Method Not Allowed CoAP code is sent in response.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param riid Resource Instance ID.
     * @param context Output context to write the resource value to. One of the <code>ret*</code>
     *     methods <strong>MUST</strong> be called in this handler before returning successfully.
     *     Failure to do so will result in 5.00 Internal Server Error being sent to the server.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceRead(int iid, int rid, int riid, AnjayOutputContext context)
            throws Exception {
        throw new UnsupportedOperationException(
                "resourceRead on Resource Instance is not implemented");
    }

    /**
     * A handler that writes the Resource value, called only if the Resource is SUPPORTED and not of
     * the {@link ResourceKind#E} kind (as returned by {@link resources}). Note that it may be
     * called on nominally read-only Resources if the write is performed by the Bootstrap Server.
     *
     * <p>If not implemented, Method Not Allowed CoAP code is sent in response.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param context Input context to read the resource value from using the <code>get*</code>
     *     methods.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceWrite(int iid, int rid, AnjayInputContext context) throws Exception {
        throw new UnsupportedOperationException("resourceWrite on Resource is not implemented");
    }

    /**
     * A handler that writes the Resource Instance value, called only if the Resource is SUPPORTED
     * and is one of the {@link ResourceKind#RM} or {@link ResourceKind#RWM} kind (as returned by
     * {@link resources}) and <code>riid</code> was returned by {@link resourceInstances}. Note that
     * it may be called on nominally read-only Resources if the write is performed by the Bootstrap
     * Server.
     *
     * <p>If not implemented, Method Not Allowed CoAP code is sent in response.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param riid Resource Instance ID.
     * @param context Input context to read the resource value from using the <code>get*</code>
     *     methods.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceWrite(int iid, int rid, int riid, AnjayInputContext context)
            throws Exception {
        throw new UnsupportedOperationException(
                "resourceWrite on Resource Instance is not implemented");
    }

    /**
     * A handler that performs the Execute action on given Resource, called only if the Resource is
     * PRESENT and of the {@link ResourceKind#E} kind (as returned by {@link resources}).
     *
     * <p>If not implemented, Method Not Allowed CoAP code is sent in response.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param args Arguments for the execute command.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceExecute(int iid, int rid, Map<Integer, Optional<String>> args)
            throws Exception {
        throw new UnsupportedOperationException("resourceExecute is not implemented");
    }

    /**
     * A handler that shall reset a Resource to its default (after creational) state. In particular,
     * for any writeable optional resource, it shall remove it; for any writeable mandatory Multiple
     * Resource, it shall remove all its instances.
     *
     * <p>NOTE: If this handler is not implemented for a Multiple Resource, then non-partial write
     * on it will not succeed.
     *
     * <p>NOTE: In the current version of Anjay, this handler is only ever called on Multiple
     * Resources. It is REQUIRED so that after calling this handler, any Multiple Resource is either
     * not PRESENT, or PRESENT, but contain zero Resource Instances.
     *
     * @param iid Object Instance ID.
     * @param rid ID of the Resource to reset.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceReset(int iid, int rid) throws Exception {
        throw new UnsupportedOperationException("resourceReset is not implemented");
    }

    /**
     * A handler that returns all Resource Instances of a Multiple Resource, called only if the
     * Resource is PRESENT and is of either {@link ResourceKind#RM}, {@link ResourceKind#WM} or
     * {@link ResourceKind#RWM} kind (as returned by {@link resources}).
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @return {@link SortedSet} with Resource Instance IDs.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default SortedSet<Integer> resourceInstances(int iid, int rid) throws Exception {
        return null;
    }

    /**
     * A handler that is called when there is a request that might modify an Object and fail. Such
     * situation often requires to rollback changes, and this handler shall implement logic that
     * prepares for possible failure in the future.
     *
     * <p>Handlers listed below are NOT called without beginning transaction in the first place
     * (note that if an Object does not implement transaction handlers, then it will not be possible
     * to perform operations listed below):
     *
     * <ul>
     *   <li>{@link instanceCreate},
     *   <li>{@link instanceRemove},
     *   <li>{@link instanceReset},
     *   <li>{@link resourceWrite(int, int, AnjayInputContext)},
     *   <li>{@link resourceWrite(int, int, int, AnjayInputContext)},
     *   <li>{@link resourceReset},
     *   <li>{@link transactionCommit},
     *   <li>{@link transactionRollback}.
     * </ul>
     *
     * <p>Note: if an error occurs during a transaction (i.e. after successful call of this
     * function) then the rollback handler {@link transactionRollback} will be executed by the
     * library.
     *
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void transactionBegin() throws Exception {
        throw new UnsupportedOperationException("transactionBegin is not implemented");
    }

    /**
     * A handler that is called after transaction is finished, but before {@link transactionCommit}
     * is called. It is used to check whether the commit operation may be successfully performed.
     *
     * <p>Any validation of the object's state shall be performed in this function, rather than in
     * the commit handler. If there is a need to commit changes to multiple objects at once, this
     * handler is called on all modified objects first, to avoid potential inconsistencies that may
     * arise from a failing commit operation.
     *
     * <p>Successfully returning from this handler means that the corresponding commit function
     * shall subsequently execute successfully. The commit handler may nevertheless fail, but if and
     * only if a fatal, unpredictable and irrecoverable error (e.g. physical write error) occurs.
     *
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void transactionValidate() throws Exception {
        throw new UnsupportedOperationException("transactionValidate is not implemented");
    }

    /**
     * A handler that is called after transaction is finished. If it fails then {@link
     * transactionRollback} handler must be called by the user code if it is necessary.
     *
     * <p>NOTE: If this function fails, the data model will be left in an inconsistent state. For
     * this reason, it may return an error value if and only if a fatal, unpredictable and
     * irrecoverable error (e.g. physical write error) occurs. All other errors (such as invalid
     * object state) shall be reported via {@link transactionValidate}.
     *
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void transactionCommit() throws Exception {
        throw new UnsupportedOperationException("transcationCommit is not implemented");
    }

    /**
     * A handler that is called whenever there is a need to restore previous Object state during a
     * transaction or during committing a transaction.
     *
     * @throws Exception In case of error.
     */
    default void transactionRollback() throws Exception {
        throw new UnsupportedOperationException("transactionRollback is not implemented");
    }
}
