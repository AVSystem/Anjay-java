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

import com.avsystem.anjay.impl.NativeAnjay;

/**
 * An exception that encapsulates any error condition reported by the underlying native Anjay
 * library.
 *
 * <p>It may also be thrown from certain callback interfaces to pass an error condition into the
 * native library.
 *
 * <p>The static fields of this class may be used as {@link #errorCode()} to represent a certain
 * CoAP-level error code.
 */
public class AnjayException extends RuntimeException {
    public static final int BAD_REQUEST = NativeAnjay.getErrorBadRequest();
    public static final int UNAUTHORIZED = NativeAnjay.getErrorUnauthorized();
    public static final int BAD_OPTION = NativeAnjay.getErrorBadOption();
    public static final int NOT_FOUND = NativeAnjay.getErrorNotFound();
    public static final int METHOD_NOT_ALLOWED = NativeAnjay.getErrorMethodNotAllowed();
    public static final int NOT_ACCEPTABLE = NativeAnjay.getErrorNotAcceptable();
    public static final int REQUEST_ENTITY_INCOMPLETE =
            NativeAnjay.getErrorRequestEntityIncomplete();
    public static final int INTERNAL = NativeAnjay.getErrorInternal();
    public static final int NOT_IMPLEMENTED = NativeAnjay.getErrorNotImplemented();
    public static final int SERVICE_UNAVAILABLE = NativeAnjay.getErrorServiceUnavailable();

    private final int errorCode;

    /**
     * Construct new <code>AnjayException</code>.
     *
     * @param errorCode Error code that comes from or into native code, corresponding to a return
     *     value from a C function.
     * @param message The detail message. It is saved for later retrieval by the <code>getMessage()
     *     </code> method.
     */
    public AnjayException(int errorCode, String message) {
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
