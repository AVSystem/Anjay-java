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

import java.util.Objects;

/** Object used to configure PSK security mode in Anjay. */
public class AnjaySecurityInfoPsk implements AnjaySecurityInfo {
    @SuppressWarnings("unused") // used in C++ code
    private final byte[] identity;

    @SuppressWarnings("unused") // used in C++ code
    private final byte[] key;

    /**
     * Constructor for PSK security info.
     *
     * @param identity DTLS Identity.
     * @param key Pre-shared key (PSK)
     */
    public AnjaySecurityInfoPsk(byte[] identity, byte[] key) {
        this.identity = Objects.requireNonNull(identity, "identity MUST NOT be null");
        this.key = Objects.requireNonNull(key, "key MUST NOT be null");
    }
}
