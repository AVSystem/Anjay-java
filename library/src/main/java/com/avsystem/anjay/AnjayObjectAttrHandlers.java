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

/**
 * Interface specifying handlers for reading and writing attributes.
 *
 * <p>LwM2M Object may implement them to override default implementation from {@link
 * AnjayAttrStorage}, if installed, or to make the attributes support working if {@link
 * AnjayAttrStorage} is not installed.
 */
public interface AnjayObjectAttrHandlers {
    /**
     * A handler that returns default attribute values set for the Object.
     *
     * @param ssid Short Server ID of the server requesting the RPC.
     * @return Attributes for the Object.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default ObjectInstanceAttrs objectReadDefaultAttrs(int ssid) throws Exception {
        throw new UnsupportedOperationException("objectReadDefaultAttrs is not implemented");
    }

    /**
     * A handler that sets default attribute values for the Object.
     *
     * @param ssid Short Server ID of the server requesting the RPC.
     * @param attrs Attributes to be set for the Object.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void objectWriteDefaultAttrs(int ssid, ObjectInstanceAttrs attrs) throws Exception {
        throw new UnsupportedOperationException("objectWriteDefaultAttrs is not implemented");
    }

    /**
     * A handler that returns default attributes set for the Object Instance.
     *
     * @param iid Checked Object Instance ID.
     * @param ssid Short Server ID of the server requesting the RPC.
     * @return Attributes for the Object Instance.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default ObjectInstanceAttrs instanceReadDefaultAttrs(int iid, int ssid) throws Exception {
        throw new UnsupportedOperationException("instanceReadDefaultAttrs is not implemented");
    }

    /**
     * A handler that sets default attributes for the Object Instance.
     *
     * @param iid Checked Object Instance ID.
     * @param ssid Short Server ID of the server requesting the RPC.
     * @param attrs Attributes to set for the Object Instance.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void instanceWriteDefaultAttrs(int iid, int ssid, ObjectInstanceAttrs attrs)
            throws Exception {
        throw new UnsupportedOperationException("instanceWriteDefaultAttrs is not implemented");
    }

    /**
     * A handler that returns Resource attributes.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param ssid Short Server ID of the LwM2M Server issuing the request.
     * @return Attributes for the resource.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default ResourceAttrs resourceReadAttrs(int iid, int rid, int ssid) throws Exception {
        throw new UnsupportedOperationException("resourceReadAttrs is not implemented");
    }

    /**
     * A handler that sets attributes for given Resource.
     *
     * @param iid Object Instance ID.
     * @param rid Resource ID.
     * @param ssid Short Server ID of the LwM2M Server issuing the request.
     * @param attrs Attributes to set for this Resource.
     * @throws Exception In case of error. If {@link AnjayException} is thrown with one of defined
     *     error codes, the response message will have an appropriate CoAP response code. Otherwise,
     *     the device will respond with an unspecified (but valid) error code.
     */
    default void resourceWriteAttrs(int iid, int rid, int ssid, ResourceAttrs attrs)
            throws Exception {
        throw new UnsupportedOperationException("resourceWriteAttrs is not implemented");
    }
}
