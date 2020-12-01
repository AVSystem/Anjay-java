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
import com.avsystem.anjay.AnjayFirmwareUpdate.InitialState;
import com.avsystem.anjay.AnjayFirmwareUpdate.Result;
import com.avsystem.anjay.AnjayFirmwareUpdateHandlers;

public final class NativeFirmwareUpdate {
    private long self;
    private final Anjay anjay;
    private final NativeFirmwareUpdateHandlers handlers;

    private native void init(
            NativeAnjay anjay, NativeFirmwareUpdateHandlers handlers, InitialState initialState);

    private native void cleanup();

    private native void nativeSetResult(Result result);

    public static native int getErrorNotEnoughSpace();

    public static native int getErrorOutOfMemory();

    public static native int getErrorIntegrityFailure();

    public static native int getErrorUnsupportedPackageType();

    public NativeFirmwareUpdate(
            Anjay anjay, AnjayFirmwareUpdateHandlers handlers, InitialState initialState)
            throws Exception {
        this.anjay = anjay;
        this.handlers = new NativeFirmwareUpdateHandlers(handlers);
        this.init(NativeUtils.getNativeAnjay(anjay), this.handlers, initialState);
    }

    public void setResult(Result result) throws Exception {
        this.nativeSetResult(result);
    }
}
