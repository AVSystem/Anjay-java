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

import com.avsystem.anjay.Anjay.CoapUdpTxParams;
import com.avsystem.anjay.AnjayAbstractSecurityConfig;
import com.avsystem.anjay.AnjayFirmwareUpdateException;
import com.avsystem.anjay.AnjayFirmwareUpdateHandlers;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NativeFirmwareUpdateHandlers {
    private final AnjayFirmwareUpdateHandlers handlers;

    private static final Logger LOGGER =
            Logger.getLogger(NativeFirmwareUpdateHandlers.class.getName());

    private static int handleException(Throwable t) {
        LOGGER.log(Level.FINE, "Exception occurred", t);
        if (t instanceof AnjayFirmwareUpdateException) {
            return ((AnjayFirmwareUpdateException) t).errorCode();
        } else {
            return -1;
        }
    }

    public NativeFirmwareUpdateHandlers(AnjayFirmwareUpdateHandlers handlers) {
        this.handlers = handlers;
    }

    int streamOpen(Optional<String> packageUri, Optional<byte[]> etag) {
        try {
            this.handlers.streamOpen(packageUri, etag);
            return 0;
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    int streamWrite(byte[] data) {
        try {
            this.handlers.streamWrite(data);
            return 0;
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    int streamFinish() {
        try {
            this.handlers.streamFinish();
            return 0;
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    void reset() {
        this.handlers.reset();
    }

    String getName() {
        return this.handlers.getName();
    }

    String getVersion() {
        return this.handlers.getVersion();
    }

    int performUpgrade() {
        try {
            this.handlers.performUpgrade();
            return 0;
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    AnjayAbstractSecurityConfig getSecurityConfig(String uri) {
        return this.handlers.getSecurityConfig(uri);
    }

    CoapUdpTxParams getCoapTxParams(Optional<String> downloadUri) {
        return this.handlers.getCoapTxParams(downloadUri);
    }
}
