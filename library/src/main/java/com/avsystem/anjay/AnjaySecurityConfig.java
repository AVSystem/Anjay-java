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

/** Security configuration for {@link Anjay}. */
public final class AnjaySecurityConfig implements AnjayAbstractSecurityConfig {
    public final AnjaySecurityInfo securityInfo;

    /**
     * Creates security configuration object.
     *
     * @param securityInfo {@link AnjaySecurityInfoPsk} or {@link AnjaySecurityInfoCert} with
     *     security details. MUST NOT be <code>null</code>.
     */
    public AnjaySecurityConfig(AnjaySecurityInfo securityInfo) {
        this.securityInfo = Objects.requireNonNull(securityInfo, "securityInfo MUST NOT be null");
    }
}
