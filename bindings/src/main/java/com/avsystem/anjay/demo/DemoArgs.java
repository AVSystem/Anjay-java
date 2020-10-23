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

package com.avsystem.anjay.demo;

import com.avsystem.anjay.Anjay.Lwm2mVersion;
import com.avsystem.anjay.AnjaySecurityObject.SecurityMode;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class DemoArgs {
    private static class Lwm2mVersionConverter implements IStringConverter<Lwm2mVersion> {
        @Override
        public Lwm2mVersion convert(String value) {
            for (Lwm2mVersion version : Lwm2mVersion.values()) {
                if (version.toString().equals(value)) {
                    return version;
                }
            }
            throw new ParameterException(
                    "incorrect value of version argument, expected one of: "
                            + Lwm2mVersion.values());
        }
    }

    private static class SecurityModeConverter implements IStringConverter<SecurityMode> {
        @Override
        public SecurityMode convert(String value) {
            for (SecurityMode mode : SecurityMode.values()) {
                if (mode.toString().equals(value)) {
                    return mode;
                }
            }
            throw new ParameterException(
                    "incorrect value of security mode argument, expected one of: "
                            + SecurityMode.values());
        }
    }

    private static class HexStringConverter implements IStringConverter<byte[]> {
        @Override
        public byte[] convert(String value) {
            try {
                return Hex.decodeHex(value.toCharArray());
            } catch (DecoderException e) {
                throw new ParameterException("malformed hexstring");
            }
        }
    }

    @Parameter(
            names = {"-u", "--server-uri"},
            description = "Server URI to connect to")
    public String serverUri = "coap://127.0.0.1:5683";

    @Parameter(
            names = {"-s", "--security-mode"},
            description = "Set security mode",
            converter = SecurityModeConverter.class)
    public SecurityMode securityMode = SecurityMode.NOSEC;

    @Parameter(
            names = {"-i", "--identity"},
            description =
                    "PSK identity (psk mode) or Public Certificate (cert mode). Both are specified as hexlified strings.",
            converter = HexStringConverter.class)
    public byte[] identityOrCert;

    @Parameter(
            names = {"-k", "--key"},
            description =
                    "PSK key (psk mode) or Private Key (cert mode). Both are specified as hexlified strings.",
            converter = HexStringConverter.class)
    public byte[] pskOrPrivKey;

    @Parameter(
            names = {"-e", "--endpoint-name"},
            description = "endpoint name to use")
    public String endpointName = "anjay-jni";

    @Parameter(
            names = "--dm-persistence-file",
            description =
                    "File to load Server, Security and Access Control object contents at startup, and store it at shutdown")
    public String dmPersistenceFile = null;

    @Parameter(
            names = "--attribute-storage-persistence-file",
            description =
                    "File to load attribute storage data from at startup, and store it at shutdown")
    public String attributeStoragePersistenceFile = null;

    @Parameter(
            names = {"-l", "--lifetime"},
            description = "registration lifetime in seconds")
    public Integer lifetime = 86400;

    @Parameter(
            names = {"-v", "--minimum-version"},
            description = "Lowest version of LwM2M Enabler to allow",
            converter = Lwm2mVersionConverter.class)
    public Lwm2mVersion minimumVersion = Lwm2mVersion.VERSION_1_0;

    @Parameter(
            names = {"-V", "--maximum-version"},
            description = "Highest version of LwM2M Enabler to allow",
            converter = Lwm2mVersionConverter.class)
    public Lwm2mVersion maximumVersion = Lwm2mVersion.VERSION_1_1;

    @Parameter(
            names = {"--fw-cert-file"},
            description =
                    "Require certificate validation against specified file when downloading firmware over encrypted channels")
    public String fwCertFile = null;

    @Parameter(
            names = {"-h", "--help"},
            description = "shows this message and exits",
            help = true)
    public boolean help;
}
