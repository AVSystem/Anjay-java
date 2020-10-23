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

import com.avsystem.anjay.AnjayFirmwareUpdate.InitialState.InitialResult;
import com.avsystem.anjay.impl.NativeFirmwareUpdate;
import java.util.Optional;

/** Default implementation of Firmware Update object (/5). */
public final class AnjayFirmwareUpdate {

    /** Information about the state to initialize the Firmware Update object in. */
    public static final class InitialState {
        /**
         * Possible values that control the State and Update Result resources at the time of
         * initialization of the Firmware Update object.
         */
        public enum InitialResult {
            /**
             * Corresponds to the "Updating" State and "Initial" Result. Shall be used when the
             * device rebooted as part of the update process, but the firmware image is not fully
             * applied yet. The application MUST use {@link AnjayFirmwareUpdate#setResult(Result)}
             * to set the result to success or failure after the update process is complete.
             */
            UPDATING,
            /**
             * Corresponds to the "Downloaded" State and "Initial" Result. Shall be used when the
             * device unexpectedly rebooted when the firmware image has already been downloaded into
             * some non-volatile memory.
             */
            DOWNLOADED,
            /**
             * Corresponds to the "Downloading" State and "Initial" Result. Shall be used when the
             * device can determine that it unexpectedly rebooted during the download of the
             * firmware image, and it has all the information necessary to resume the download. Such
             * information shall then be passed via other fields in the {@link InitialState} class.
             */
            DOWNLOADING,
            /**
             * Corresponds to the "Idle" State and "Initial" Result. Shall be used when the library
             * is initializing normally, not after a firmware update attempt.
             */
            NEUTRAL,
            /**
             * Corresponds to the "Idle" State and "Firmware updated successfully" Result. Shall be
             * used when the device has just rebooted after successfully updating the firmware.
             */
            SUCCESS,
            /**
             * Corresponds to the "Idle" State and "Integrity check failure" Result. Shall be used
             * when the device has just rebooted after an unsuccessful firmware update attempt that
             * failed due to failed integrity check of the firmware package.
             */
            INTEGRITY_FAILURE,
            /**
             * Corresponds to the "Idle" State "Firmware update failed" Result. Shall be used when
             * the device has just rebooted after a firmware upgrade attempt that was unsuccessful
             * for reason any other than integrity check.
             */
            FAILED
        }

        /**
         * Controls initialization of the State and Update Result resources. It is intended to be
         * used after a reboot caused by a firmware update attempt, to report the update result.
         */
        public InitialResult result = InitialResult.NEUTRAL;

        /**
         * Value to initialize the Package URI resource with.
         *
         * <p>Required when {@link #result} is set to {@link InitialState.InitialResult#DOWNLOADING
         * DOWNLOADING}; if it is not provided in such case, {@link
         * AnjayFirmwareUpdateHandlers#reset()} handler will be called from {@link
         * AnjayFirmwareUpdate#install(Anjay, AnjayFirmwareUpdateHandlers, InitialState) install} to
         * reset the Firmware Update object into the Idle state.
         *
         * <p>Optional when {@link #result} is {@link InitialState.InitialResult#DOWNLOADED
         * DOWNLOADED}; in this case it signals that the firmware was downloaded using the Pull
         * mechanism.
         *
         * <p>In all other cases it is ignored.
         */
        public String persistedUri;

        /**
         * Number of bytes that has been already successfully downloaded and are available at the
         * time of calling {@link AnjayFirmwareUpdate#install install}.
         *
         * <p>It is ignored unless {@link #result} is {@link InitialState.InitialResult#DOWNLOADING
         * DOWNLOADING}, in which case the following call to {@link
         * AnjayFirmwareUpdateHandlers#streamWrite} shall append the passed chunk of data at the
         * offset set here. If resumption from the set offset is impossible, the library will call
         * {@link AnjayFirmwareUpdateHandlers#reset} and {@link
         * AnjayFirmwareUpdateHandlers#streamOpen} to restart the download process.
         */
        public int resumeOffset;

        /**
         * ETag of the download process to resume.
         *
         * <p>Required when {@link #result} is {@link InitialState.InitialResult#DOWNLOADING
         * DOWNLOADING} and {@link #resumeOffset} &gt; 0; if it is not provided in such case, {@link
         * AnjayFirmwareUpdateHandlers#reset} handler will be called from {@link
         * AnjayFirmwareUpdate#install install} to reset the Firmware Update object into the Idle
         * state.
         */
        public Optional<byte[]> resumeEtag = Optional.empty();
    }

    /** Values of the Firmware Update Result resource. See LwM2M specification for details. */
    public enum Result {
        INITIAL,
        SUCCESS,
        NOT_ENOUGH_SPACE,
        OUT_OF_MEMORY,
        CONNECTION_LOST,
        INTEGRITY_FAILURE,
        UNSUPPORTED_PACKAGE_TYPE,
        INVALID_URI,
        FAILED,
        UNSUPPORTED_PROTOCOL
    }

    private final NativeFirmwareUpdate fwu;

    private AnjayFirmwareUpdate(
            Anjay anjay, AnjayFirmwareUpdateHandlers handlers, InitialState initialState)
            throws Exception {
        this.fwu = new NativeFirmwareUpdate(anjay, handlers, initialState);
    }

    /**
     * Installs the Firmware Update object in an Anjay object.
     *
     * @param anjay Anjay object for which the Firmware Update Object is installed.
     * @param handlers Implemented handlers used during firmware update process.
     * @param initialState Information about the state to initialize the Firmware Update object in.
     *     It is intended to be used after either an orderly reboot caused by a firmware update
     *     attempt to report the update result, or by an unexpected reboot in the middle of the
     *     download process.
     * @return {@link AnjayFirmwareUpdate} object which can be used to set result of the firmware
     *     update process.
     * @throws Exception In case of error.
     */
    public static AnjayFirmwareUpdate install(
            Anjay anjay, AnjayFirmwareUpdateHandlers handlers, InitialState initialState)
            throws Exception {
        return new AnjayFirmwareUpdate(anjay, handlers, initialState);
    }

    /**
     * Sets the Firmware Update Result, interrupting the update process.
     *
     * <p>A successful call to this function always sets Update State to Idle (0). If the function
     * fails, neither Update State nor Update Result are changed.
     *
     * <p>Some state transitions are disallowed and cause this function to fail:
     *
     * <ul>
     *   <li>{@link Result#INITIAL INITIAL} is never allowed and causes this function to fail.
     *   <li>{@link Result#SUCCESS SUCCESS} is only allowed if the firmware application process was
     *       started by the server (an Execute operation was already performed on the Update
     *       resource of the Firmware Update object or {@link InitialState.InitialResult#UPDATING
     *       UPDATING} was used in a call to {@link AnjayFirmwareUpdate#install install()}.
     *       Otherwise, the function fails.
     *   <li>Other values of <code>result</code> (various error codes) are only allowed if Firmware
     *       Update State is not Idle (0), i.e. firmware is being downloaded, was already downloaded
     *       or is being applied.
     * </ul>
     *
     * <p>WARNING: calling this in {@link AnjayFirmwareUpdateHandlers#performUpgrade} handler is
     * supported, but the result of using it from within any other of {@link
     * AnjayFirmwareUpdateHandlers} is undefined.
     *
     * @param result Value of the Update Result resource to set.
     * @throws Exception If the result can't be set.
     */
    public void setResult(Result result) throws Exception {
        fwu.setResult(result);
    }
}
