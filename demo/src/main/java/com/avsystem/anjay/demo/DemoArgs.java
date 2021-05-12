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

package com.avsystem.anjay.demo;

import com.avsystem.anjay.Anjay.Lwm2mVersion;
import com.avsystem.anjay.AnjaySecurityObject.SecurityMode;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class DemoArgs {
    public static final class AccessEntry {
        public int ssid;
        public int oid;
        public int iid;
        public int accessMask;
    }

    public static AccessEntry convertAccessEntry(String value) {
        Pattern pattern = Pattern.compile("/(\\d+)/(\\d+),(\\d+),(\\d+)");
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            AccessEntry accessEntry = new AccessEntry();
            accessEntry.oid = Integer.parseInt(matcher.group(1));
            accessEntry.iid = Integer.parseInt(matcher.group(2));
            accessEntry.ssid = Integer.parseInt(matcher.group(3));
            accessEntry.accessMask = Integer.parseInt(matcher.group(4));
            return accessEntry;
        }
        return null;
    }

    private static class AccessEntryConverter implements IStringConverter<AccessEntry> {
        @Override
        public AccessEntry convert(String value) {
            AccessEntry accessEntry = DemoArgs.convertAccessEntry(value);
            if (accessEntry != null) {
                return accessEntry;
            }
            throw new ParameterException(
                    "incorrect value of access entry argument: \""
                            + value
                            + "\", the format is: /OID/IID,SSID,mask");
        }
    }

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
                            + Arrays.toString(Lwm2mVersion.values()));
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
                            + Arrays.toString(SecurityMode.values()));
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

    private static class DurationConverter implements IStringConverter<Duration> {
        @Override
        public Duration convert(String value) {
            return Duration.ofMillis((long) (Double.parseDouble(value) * 1000));
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
            names = {"-C", "--client-cert-file"},
            description =
                    "DER-formatted client certificate file to load. Mutually exclusive with -i.")
    public String clientCertFile;

    @Parameter(
            names = {"-K", "--key-file"},
            description =
                    "DER-formatted PKCS#8 private key complementary to the certificate specified with -C. Mutually exclusive with -k.")
    public String keyFile;

    @Parameter(
            names = {"-e", "--endpoint-name"},
            description = "endpoint name to use")
    public String endpointName = "anjay-jni";

    @Parameter(
            names = {"-b", "--bootstrap"},
            description = "treat first URI as Bootstrap Server")
    public boolean bootstrap;

    @Parameter(
            names = "--bootstrap=client-initiated-only",
            description =
                    "treat first URI as Bootstrap Server (the legacy LwM2M 1.0-style Server-Initiated bootstrap mode is not available)")
    public boolean bootstrapClientInitiatedOnly = false;

    @Parameter(
            names = {"-H", "--bootstrap-holdoff"},
            description = "number of seconds to wait before attempting Client Initiated Bootstrap")
    public Integer bootstrapHoldoff = 0;

    @Parameter(
            names = {"-T", "--bootstrap-timeout"},
            description =
                    "number of seconds to keep the Bootstrap Server Account for after successful bootstrapping, or 0 for infinity")
    public Integer bootstrapTimeout = 0;

    @Parameter(
            names = {"-a", "--access-entry"},
            description = "create ACL entry for specified /OID/IID and SSID",
            listConverter = AccessEntryConverter.class)
    public List<AccessEntry> accessEntries = null;

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
            names = "--cache-size",
            description =
                    "Size, in bytes, of a buffer reserved for caching sent responses to detect retransmissions. Setting it to 0 disables caching mechanism.")
    public Integer cacheSize = 0;

    @Parameter(
            names = {"--fw-cert-file"},
            description =
                    "Require certificate validation against specified file when downloading firmware over encrypted channels")
    public String fwCertFile = null;

    @Parameter(
            names = "--ack-random-factor",
            description = "Configures ACK_RANDOM_FACTOR (defined in RFC7252)")
    public Double ackRandomFactor = 1.5;

    @Parameter(
            names = "--ack-timeout",
            description = "Configures ACK_TIMEOUT (defined in RFC7252) in seconds",
            converter = DurationConverter.class)
    public Duration ackTimeout = Duration.ofSeconds(2);

    @Parameter(
            names = "--max-retransmit",
            description = "Configures MAX_RETRANSMIT (defined in RFC7252)")
    public Integer maxRetransmit = 4;

    @Parameter(names = "--nstart", description = "Configures NSTART (defined in RFC7252)")
    public Integer nstart = 1;

    @Parameter(
            names = {"-h", "--help"},
            description = "shows this message and exits",
            help = true)
    public boolean help;
}
