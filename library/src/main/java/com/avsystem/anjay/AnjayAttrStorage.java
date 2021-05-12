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

import com.avsystem.anjay.AnjayAttributes.ObjectInstanceAttrs;
import com.avsystem.anjay.AnjayAttributes.ResourceAttrs;
import com.avsystem.anjay.impl.NativeAttrStorage;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Attribute Storage module. Manages attributes for LwM2M Objects, their instances and resources.
 */
public final class AnjayAttrStorage {
    private final NativeAttrStorage attrStorage;

    private AnjayAttrStorage(Anjay anjay) throws Exception {
        this.attrStorage = new NativeAttrStorage(anjay);
    }

    /**
     * Installs the Attribute Storage handlers in an Anjay object, making it possible to
     * automatically manage attributes for LwM2M Objects, their instances and resources.
     *
     * <p>In accordance to the LwM2M specification, there are three levels on which attributes may
     * be stored:
     *
     * <ul>
     *   <li>Resource level
     *   <li>Instance level
     *   <li>Object level
     * </ul>
     *
     * <p>Attribute Storage is used for the object only if it doesn't implement {@link
     * AnjayObjectAttrHandlers} interface.
     *
     * @param anjay Anjay instance to install the Attribute Storage for.
     * @return Attribute Storage object.
     * @throws Exception In case of failure.
     */
    public static AnjayAttrStorage install(Anjay anjay) throws Exception {
        return new AnjayAttrStorage(anjay);
    }

    /**
     * Removes all attributes from all entities, leaving the Attribute Storage in an empty state.
     */
    public void purge() {
        this.attrStorage.purge();
    }

    /**
     * Sets Object level attributes for the specified SSID.
     *
     * @param ssid SSID for which given Attributes shall be set (must be a valid SSID corresponding
     *     to one of the non-Bootstrap LwM2M Servers).
     * @param oid Object ID for which given Attributes shall be set.
     * @param attrs Attributes to be set (MUST NOT be <code>null</code>).
     * @throws Exception If setting the attributes failed or the object implements {@link
     *     AnjayObjectAttrHandlers} interface.
     */
    public void setObjectAttrs(int ssid, int oid, ObjectInstanceAttrs attrs) throws Exception {
        if (attrs == null) {
            throw new IllegalArgumentException("attrs MUST NOT be null");
        }
        this.attrStorage.setObjectAttrs(ssid, oid, attrs);
    }

    /**
     * Sets Instance level attributes for the specified SSID.
     *
     * @param ssid SSID for which given Attributes shall be set (must be a valid SSID corresponding
     *     to one of the non-Bootstrap LwM2M Servers).
     * @param oid Object ID for which given Attributes shall be set.
     * @param iid Instance ID for which given Attributes shall be set.
     * @param attrs Attributes to be set (MUST NOT be <code>null</code>).
     * @throws Exception If setting the attributes failed or the object implements {@link
     *     AnjayObjectAttrHandlers} interface.
     */
    public void setInstanceAttrs(int ssid, int oid, int iid, ObjectInstanceAttrs attrs)
            throws Exception {
        if (attrs == null) {
            throw new IllegalArgumentException("attrs MUST NOT be null");
        }
        this.attrStorage.setInstanceAttrs(ssid, oid, iid, attrs);
    }

    /**
     * Sets Resource level attributes for the specified SSID.
     *
     * @param ssid SSID for which given Attributes shall be set (must be a valid SSID corresponding
     *     to one of the non-Bootstrap LwM2M Servers).
     * @param oid Object ID owning the specified Instance.
     * @param iid Instance ID owning the specified Resource.
     * @param rid Resource ID for which given Attributes shall be set.
     * @param attrs Attributes to be set (MUST NOT be <code>null</code>).
     * @throws Exception If setting the attributes failed or the object implements {@link
     *     AnjayObjectAttrHandlers} interface.
     */
    public void setResourceAttrs(int ssid, int oid, int iid, int rid, ResourceAttrs attrs)
            throws Exception {
        if (attrs == null) {
            throw new IllegalArgumentException("attrs MUST NOT be null");
        }
        this.attrStorage.setResourceAttrs(ssid, oid, iid, rid, attrs);
    }

    /**
     * Dumps all set attributes to the output stream.
     *
     * @param outputStream Stream to write to.
     * @throws Exception If the persist operation failed.
     */
    public void persist(OutputStream outputStream) throws Exception {
        this.attrStorage.persist(outputStream);
    }

    /**
     * Attempts to restore attribute storage from specified input stream.
     *
     * <p>Note: before attempting restoration, the Attribute Storage is cleared, so no previously
     * set attributes will be retained. In particular, if restore fails, then the Attribute Storage
     * will be completely cleared and {@link #isModified()} will return <code>true</code>.
     *
     * <p><strong>NOTE:</strong> For historical reasons, this function behaves differently than all
     * other restore methods.
     *
     * <ul>
     *   <li>On failed restoration, the storage is cleared, rather than left untouched
     *   <li>Zero-length stream is treated as valid, and causes the storage to be cleared without
     *       throwing an exception
     * </ul>
     *
     * <p>Relying on these behaviours is <strong>DEPRECATED</strong>. Future versions of Anjay may
     * change the semantics of this function so that it retains the contents of Attribute Storage on
     * failure, and does not treat empty streams as valid. It is <strong>RECOMMENDED</strong> that
     * any new code involving this function is written to work properly with both semantics.
     *
     * @param inputStream Stream to read from.
     * @throws Exception If the restoration failed.
     */
    public void restore(InputStream inputStream) throws Exception {
        this.attrStorage.restore(inputStream);
    }

    /**
     * Checks whether the Attribute Storage has been modified since last successful call to {@link
     * #persist persist()} or {@link #restore restore()}.
     *
     * @return true if Attribute Storage has been modified, false otherwise.
     */
    public boolean isModified() {
        return this.attrStorage.isModified();
    }
}
