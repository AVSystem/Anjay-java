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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Object used to configure Certificate security mode in Anjay. */
public final class AnjaySecurityInfoCert implements AnjaySecurityInfo {
    /** Certificate object. */
    public static final class Certificate {
        @SuppressWarnings("unused") // used in C++ code
        private final byte[] rawCertificate;

        /**
         * Constructor for Certificate object.
         *
         * @param rawCertificate Certificate content. MUST NOT be <code>null</code>.
         */
        public Certificate(byte[] rawCertificate) {
            this.rawCertificate =
                    Objects.requireNonNull(rawCertificate, "rawCertificate MUST NOT be null");
        }
    }

    /** Certificate Revocation List. */
    public static final class CertificateRevocationList {
        @SuppressWarnings("unused") // used in C++ code
        private final byte[] rawCrl;

        /**
         * Constructor for Certificate Revocation List.
         *
         * @param rawCrl Certificate Revocation List content. MUST NOT be <code>null</code>.
         */
        public CertificateRevocationList(byte[] rawCrl) {
            this.rawCrl = Objects.requireNonNull(rawCrl, "rawCrl MUST NOT be null");
        }
    }

    /** Certificate's Private Key object. */
    public static final class PrivateKey {
        @SuppressWarnings("unused") // used in C++ code
        private final byte[] rawKey;

        @SuppressWarnings("unused") // used in C++ code
        private final Optional<String> password;

        /**
         * Constructor for Private Key object.
         *
         * @param rawKey Private Key content. MUST NOT be <code>null</code>.
         */
        public PrivateKey(byte[] rawKey) {
            this.rawKey = Objects.requireNonNull(rawKey, "rawKey MUST NOT be null");
            this.password = Optional.empty();
        }

        /**
         * Constructor for Private Key object.
         *
         * @param rawKey Private Key content. MUST NOT be <code>null</code>.
         * @param password Password to decrypt <code>rawKey</code>.
         */
        public PrivateKey(byte[] rawKey, String password) {
            this.rawKey = Objects.requireNonNull(rawKey, "rawKey MUST NOT be null");
            this.password = Optional.ofNullable(password);
        }
    }

    /**
     * Enables validation of peer certificate chain. If disabled, {@link #trustedCerts} are ignored.
     */
    public boolean serverCertValidation;

    /**
     * Store of trust anchor certificates. This field is optional and can be left zero-initialized.
     */
    public List<Certificate> trustedCerts;

    /**
     * Local certificate to use for authenticating with the peer. This field is optional and can be
     * left zero-initialized.
     */
    public List<Certificate> clientCert;

    /**
     * Store of certificate revocation lists. This field is optional and can be left
     * zero-initialized.
     */
    public List<CertificateRevocationList> certRevocationLists;

    /**
     * Private key matching {@link #clientCert} to use for authenticating with the peer. This field
     * is optional and can be left zero-initialized, unless {@link #clientCert} is also specified.
     */
    public PrivateKey clientKey;
}
