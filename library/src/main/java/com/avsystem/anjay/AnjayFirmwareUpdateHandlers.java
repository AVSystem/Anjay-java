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

import com.avsystem.anjay.Anjay.CoapUdpTxParams;
import java.util.Optional;

/**
 * Handler callbacks that shall implement the platform-specific part of firmware update process.
 *
 * <p>The Firmware Update object logic may be in one of the following states:
 *
 * <ul>
 *   <li><strong>Idle</strong>. This is the state in which the object is just after creation (unless
 *       initialized with either {@link AnjayFirmwareUpdate.InitialState.InitialResult#DOWNLOADED
 *       DOWNLOADED} or {@link AnjayFirmwareUpdate.InitialState.InitialResult#DOWNLOADING
 *       DOWNLOADING}. The following handlers may be called in this state:
 *       <ul>
 *         <li>{@link #streamOpen streamOpen()} - shall open the download stream; moves the object
 *             into the <em>Downloading</em> state
 *         <li>{@link #getSecurityConfig} - shall fill in security info that shall be used for a
 *             given URL
 *         <li>{@link #reset} - shall cleanup the object state if neccessary
 *       </ul>
 *   <li><strong>Downloading</strong>. The object might be initialized directly into this state by
 *       using {@link AnjayFirmwareUpdate.InitialState.InitialResult#DOWNLOADING DOWNLOADING}. In
 *       this state, the download stream is open and data may be transferred. The following handlers
 *       may be called in this state:
 *       <ul>
 *         <li>{@link #streamWrite streamWrite()} - shall write a chunk of data into the download
 *             stream; it normally does not change state - however, if it fails, it will be
 *             immediately followed by a call to {@link #reset()}
 *         <li>{@link #streamFinish()} - shall close the download stream and perform integrity check
 *             on the downloaded image; if successful, this moves the object into the
 *             <em>Downloaded</em> state. If failed - into the <em>Idle</em> state; note that {@link
 *             #reset()} will NOT be called in that case
 *         <li>{@link #reset()} - shall remove all downloaded data; moves the object into the
 *             <em>Idle</em> state
 *       </ul>
 *   <li><strong>Downloaded</strong>. The object might be initialized directly into this state by
 *       using {@link AnjayFirmwareUpdate.InitialState.InitialResult#DOWNLOADED DOWNLOADED}. In this
 *       state, the firmware package has been downloaded and checked and is ready to be flashed. The
 *       following handlers may be called in this state:
 *       <ul>
 *         <li>{@link #reset()} - shall reset all downloaded data; moves the object into the
 *             <em>Idle</em> state
 *         <li>{@link #getName()} - shall return the package name, if available
 *         <li>{@link #getVersion()} - shall return the package version, if available
 *         <li>{@link #performUpgrade()} - shall perform the actual upgrade; if it fails, it does
 *             not cause a state change and may be called again; upon success, it may be treated as
 *             a transition to a "terminal" state, after which the device is expected to reboot
 *       </ul>
 * </ul>
 */
public interface AnjayFirmwareUpdateHandlers {
    /**
     * Opens the stream that will be used to write the firmware package to.
     *
     * <p>The intended way of implementing this handler is to open a temporary file using or
     * allocate some memory buffer that may then be used to store the downloaded data in. The
     * library will not attempt to call {@link #streamWrite streamWrite} without having previously
     * called {@link #streamOpen streamOpen()}.
     *
     * <p>Note that this handler will NOT be called after initializing the object with the {@link
     * AnjayFirmwareUpdate.InitialState.InitialResult#DOWNLOADING DOWNLOADING} state, so any
     * necessary resources shall be already open before calling @ref {@link
     * AnjayFirmwareUpdate#install install()}.
     *
     * @param packageUri URI of the package from which a Pull-mode download is performed, or empty
     *     if it is a Push-mode download. This argument may either be ignored, or persisted in
     *     non-volatile storage if the client supports download resumption after an unexpected
     *     reboot (see {@link AnjayFirmwareUpdate.InitialState} and its fields).
     * @param etag ETag of the data being downloaded in Pull mode, or empty if it is a Push-mode
     *     download or ETags are not supported by the remote server. This argument may either be
     *     ignored, or persisted in non-volatile storage if the client supports download resumption
     *     after an unexpected reboot (see {@link AnjayFirmwareUpdate.InitialState} and its fields).
     * @throws Exception In case of failure.
     */
    void streamOpen(Optional<String> packageUri, Optional<byte[]> etag) throws Exception;

    /**
     * Writes data to the download stream.
     *
     * <p>May be called multipled times after {@link #streamOpen streamOpen()}, once for each
     * consecutive chunk of downloaded data.
     *
     * @param data Chunk of the firmware package being downloaded. Guaranteed to be non-<code>null
     *     </code>.
     * @throws AnjayFirmwareUpdateException In case of failure. The corresponding value will be set
     *     in the Update Result Resource.
     */
    void streamWrite(byte[] data) throws AnjayFirmwareUpdateException;

    /**
     * Closes the download stream and prepares the firmware package to be applied.
     *
     * <p>Will be called after a series of {@link #streamWrite(byte[]) streamWrite()} calls after
     * the whole package is downloaded.
     *
     * <p>The intended way of implementing this handler is to e.g. close the file and perform
     * integrity check on it. It might also be uncompressed or decrypted as necessary, so that it is
     * ready to be applied. The exact split of responsibility between {@link #streamFinish()} and
     * {@link #performUpgrade()} is not clearly defined and up to the implementor.
     *
     * <p>Note that regardless of the exception thrown, the stream is considered to be closed. That
     * is, upon successful return, the Firmware Update object is considered to be in the
     * <em>Downloaded</em> state, and upon throwing an exception - in the <em>Idle</em> state.
     *
     * @throws AnjayFirmwareUpdateException In case of failure. The corresponding value will be set
     *     in the Update Result Resource.
     */
    void streamFinish() throws AnjayFirmwareUpdateException;

    /**
     * Resets the firmware update state and performs any applicable cleanup of temporary storage if
     * necessary.
     *
     * <p>Will be called at request of the server, or after a failed download. Note that it may be
     * called without previously calling {@link #streamFinish()}, so it shall also close the
     * currently open download stream, if any.
     */
    void reset();

    /**
     * Returns the name of downloaded firmware package.
     *
     * <p>The name will be exposed in the data model as the PkgName Resource. If this callback
     * returns <code>null</code> or is not implemented at all, that Resource will not be present in
     * the data model.
     *
     * <p>It only makes sense for this handler to return non-<code>null</code> values if there is a
     * valid package already downloaded. The library will not call this handler in any state other
     * than <em>Downloaded</em>.
     *
     * @return The callback shall return the package name, or <code>null</code> if it is not
     *     currently available.
     */
    String getName();

    /**
     * Returns the version of downloaded firmware package.
     *
     * <p>The version will be exposed in the data model as the PkgVersion Resource. If this callback
     * returns <code>null</code> or is not implemented at all, that Resource will not be present in
     * the data model.
     *
     * <p>It only makes sense for this handler to return non-<code>null</code> values if there is a
     * valid package already downloaded. The library will not call this handler in any state other
     * than <em>Downloaded</em>.
     *
     * @return The callback shall return the package version, or <code>null</code> if it is not
     *     currently available.
     */
    String getVersion();

    /**
     * Performs the actual upgrade with previously downloaded package.
     *
     * <p>Will be called at request of the server, after a package has been downloaded.
     *
     * <p>Most users will want to implement firmware update in a way that involves a reboot. In such
     * case, it is expected that this callback will do either one of the following:
     *
     * <ul>
     *   <li>return, causing the outermost event loop to terminate, shutdown the library and then
     *       perform the firmware upgrade and then the device to reboot
     *   <li>perform the firmware upgrade internally and never return, causing a reboot in the
     *       process.
     * </ul>
     *
     * <p>After rebooting, the result of the upgrade process may be passed to the library during
     * initialization via the {@link AnjayFirmwareUpdate.InitialState.InitialResult InitialResult}
     * argument to {@link AnjayFirmwareUpdate#install install()}.
     *
     * <p>Alternatively, if the update can be performed without reinitializing Anjay, you can use
     * {@link AnjayFirmwareUpdate#setResult(Result)} (either from within the handler or some time
     * after returning from it) to pass the update result.
     *
     * @throws AnjayFirmwareUpdateException The callback shall throw an exception if it can be
     *     determined without a reboot that the firmware upgrade cannot be successfully performed.
     *     The corresponding value will be set in the Update Result Resource.
     */
    void performUpgrade() throws AnjayFirmwareUpdateException;

    /**
     * Queries security information that shall be used for an encrypted connection with a PULL-mode
     * download server.
     *
     * <p>May be called before {@link #streamOpen} if the download is to be performed in PULL mode
     * and the connection needs to use TLS or DTLS encryption.
     *
     * <p>If this handler is not implemented at all or <code>null</code> is returned, Anjay will
     * attempt to obtain security information from Data Model (in a way equivalent to how {@link
     * Anjay#securityConfigFromDm} works).
     * <!--
     * TODO: T3265
     * In that (no user-defined handler) case, in the commercial version,
     * <c>anjay_security_config_pkix()</c> will be used as an additional fallback if
     * <c>ANJAY_WITH_LWM2M11</c> is enabled and a valid trust store is available
     * (either specified through <c>use_system_trust_store</c>,
     * <c>trust_store_certs</c> or <c>trust_store_crls</c> fields in
     * <c>anjay_configuration_t</c>, or obtained via <c>/est/crts</c> request if
     * <c>est_cacerts_policy</c> is set to
     * <c>ANJAY_EST_CACERTS_IF_EST_CONFIGURED</c> or
     * <c>ANJAY_EST_CACERTS_ALWAYS</c>).
     * -->
     * You may also use this function yourself, for example as a fallback mechanism.
     *
     * @param uri URI on which the download will be started.
     * @return instance of {@link AnjaySecurityConfig} or <code>null</code>.
     */
    default AnjaySecurityConfig getSecurityConfig(String uri) {
        return null;
    }

    /**
     * Returns CoAP transmission parameters used to override default ones.
     *
     * <p>If this handler is not implemented at all or <code>null</code> is returned, tx params from
     * {@link Anjay} object are used.
     *
     * @param downloadUri Target firmware URI.
     * @return Object with CoAP transmission parameters.
     */
    default CoapUdpTxParams getCoapTxParams(Optional<String> downloadUri) {
        return null;
    }
}
