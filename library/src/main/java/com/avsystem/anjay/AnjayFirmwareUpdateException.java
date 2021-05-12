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

import com.avsystem.anjay.impl.NativeFirmwareUpdate;

/** Exception which may be thrown from some methods from AnjayFirmwareUpdateHandlers. */
public class AnjayFirmwareUpdateException extends RuntimeException {
    public static final int NOT_ENOUGH_SPACE = NativeFirmwareUpdate.getErrorNotEnoughSpace();
    public static final int OUT_OF_MEMORY = NativeFirmwareUpdate.getErrorOutOfMemory();
    public static final int INTEGRITY_FAILURE = NativeFirmwareUpdate.getErrorIntegrityFailure();
    public static final int UNSUPPORTED_PACKAGE_TYPE =
            NativeFirmwareUpdate.getErrorUnsupportedPackageType();

    private final int errorCode;

    /**
     * Construct new <code>AnjayFirmwareUpdateException</code>.
     *
     * @param errorCode Error code that comes from or into native code, corresponding to a return
     *     value from a C function.
     * @param message The detail message. It is saved for later retrieval by the <code>getMessage()
     *     </code> method.
     */
    public AnjayFirmwareUpdateException(int errorCode, String message) {
        super(message + " (code: " + errorCode + ")");
        this.errorCode = errorCode;
    }

    /**
     * Retrieves the error code corresponding to a return value of a C function.
     *
     * @return The error code. Usually a negative value.
     */
    public int errorCode() {
        return errorCode;
    }
}
