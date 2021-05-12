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

import com.avsystem.anjay.impl.NativeAnjay;
import com.avsystem.anjay.impl.NativeLog;
import java.nio.channels.SelectableChannel;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Anjay object containing all information required for LwM2M communication. */
public final class Anjay implements AutoCloseable {
    private NativeAnjay anjay;

    /**
     * A constant that may be used in {@link #scheduleRegistrationUpdate(int)} call instead of Short
     * Server ID to send Update messages to all connected servers.
     */
    public static final int SSID_ANY = NativeAnjay.getSsidAny();

    /**
     * An SSID value reserved by LwM2M to refer to the Bootstrap Server.
     *
     * <p>NOTE: The value of a "Short Server ID" Resource in the Security Object Instance referring
     * to the Bootstrap Server is irrelevant and cannot be used to identify the Bootstrap Server.
     */
    public static final int SSID_BOOTSTRAP = NativeAnjay.getSsidBootstrap();

    /**
     * Value reserved by the LwM2M spec for all kinds of IDs (Object IDs, Object Instance IDs,
     * Resource IDs, Resource Instance IDs, Short Server IDs).
     */
    public static final int ID_INVALID = NativeAnjay.getIdInvalid();

    /** Initial configuration used when creating an Anjay object. */
    public static final class Configuration {
        /**
         * Endpoint name as presented to the LwM2M server. Must be non-null, or otherwise {@link
         * Anjay#Anjay} will fail.
         */
        public String endpointName;

        /**
         * UDP port number that all listening sockets will be bound to. It may be left at 0 - in
         * that case, connection with each server will use a freshly generated ephemeral port
         * number.
         */
        public int udpListenPort;

        /**
         * DTLS version to use for communication. {@link DtlsVersion#DEFAULT} will be automatically
         * mapped to {@link DtlsVersion#TLSv1_2}, which is the version mandated by LwM2M
         * specification.
         */
        public Optional<DtlsVersion> dtlsVersion = Optional.empty();

        /**
         * Maximum size of a single incoming CoAP message. Decreasing this value reduces memory
         * usage, but packets bigger than this value will be dropped.
         */
        public long inBufferSize;

        /**
         * Maximum size of a single outgoing CoAP message. If the message exceeds this size, the
         * library performs the block-wise CoAP transfer (https://tools.ietf.org/html/rfc7959).
         *
         * <p>NOTE: in case of block-wise transfers, this value limits the payload size for a single
         * block, not the size of a whole packet.
         */
        public long outBufferSize;

        /**
         * Number of bytes reserved for caching CoAP responses. If not 0, the library looks up
         * recently generated responses and reuses them to handle retransmitted packets (ones with
         * identical CoAP message ID).
         *
         * <p>NOTE: while a single cache is used for all LwM2M servers, cached responses are tied to
         * a particular server and not reused for other ones.
         */
        public long msgCacheSize;

        /**
         * Socket configuration to use when creating TCP/UDP sockets.
         *
         * <p>Note that:
         *
         * <ul>
         *   <li><code>reuse_addr</code> will be forced to true.
         *   <li>Value pointed to by the <code>preferred_endpoint</code> will be ignored.
         * </ul>
         */
        // TODO: avs_net_socket_configuration_t socket_config;

        /**
         * Configuration of the CoAP transmission params for UDP connection, as per RFC 7252.
         *
         * <p>The default configuration is {@link CoapUdpTxParams#DEFAULT}.
         */
        public Optional<CoapUdpTxParams> udpTxParams = Optional.empty();

        /**
         * Configuration of the DTLS handshake retransmission timeouts for UDP connection.
         *
         * <p>If not set, the default configuration <code>ANJAY_DTLS_DEFAULT_UDP_HS_TX_PARAMS</code>
         * will be selected internally.
         *
         * <p>NOTE: Parameters are copied during {@link Anjay#Anjay} and cannot be modified later
         * on.
         *
         * <p>IMPORTANT: In case of a need to adjust DTLS retransmission params to match the CoAP
         * retransmission params, the <code>udpDtlsHsTxParams</code> shall be initialized as
         * <strong>dtlsHsParams</strong> is in the following code snippet:
         *
         * <pre>
         *  final CoapUdpTxParams coapTxParams = ... // some initialization
         *
         *  // Without ACK_RANDOM_FACTOR = 1.0, it is impossible to create a DTLS
         *  // configuration that matches CoAP retransmission configuration perfectly.
         *  if (coapTxParams.ackRandomFactor != 1.0) {
         *      throw new IllegalStateException();
         *  }
         *
         *  final DtlsHandhshakeTimeouts = new DtlsHandshakeTimeouts(
         *      coapTxParams.ackTimeout,
         *      coapTxParams.ackTimeout.multipliedBy(1 &lt;&lt; coapTxParams.ackTimeout.maxRetransmit)
         *  );
         * </pre>
         */
        public Optional<DtlsHandshakeTimeouts> udpDtlsHsTxParams = Optional.empty();

        /**
         * Controls whether Notify operations are conveyed using Confirmable CoAP messages by
         * default.
         */
        public boolean confirmableNotifications;

        /**
         * If set to true, connection to the Bootstrap Server will be closed immediately after
         * making a successful connection to any regular LwM2M Server and only opened again if
         * (re)connection to a regular server is rejected.
         *
         * <p>If set to false, legacy Server-Initiated Bootstrap is possible, i.e. the Bootstrap
         * Server can reach the client at any time to re-initiate the bootstrap sequence.
         *
         * <p>NOTE: This parameter controls a legacy Server-Initiated Bootstrap mechanism based on
         * an interpretation of LwM2M 1.0 TS that is not universally accepted. Server-Initiated
         * Bootstrap as specified in LwM2M 1.1 TS is always supported, regardless of this setting.
         */
        public boolean disableLegacyServerInitiatedBootstrap;

        /**
         * If "Notification Storing When Disabled or Offline" resource is set to true and either the
         * client is in offline mode, or uses Queue Mode, Notify messages are enqueued and sent
         * whenever the client is online again. This value allows one to limit the size of said
         * notification queue. The limit applies to notifications queued for all servers.
         *
         * <p>If set to 0, size of the stored notification queue is only limited by the amount of
         * available RAM.
         *
         * <p>If set to a positive value, that much <strong>most recent</strong> notifications are
         * stored. Attempting to add a notification to the queue while it is already full drops the
         * oldest one to make room for new one.
         */
        public long storedNotificationLimit;

        /**
         * Sets the preference of the library for Content-Format used when responding to a request
         * without Accept option.
         *
         * <p>If set to true, the formats used would be:
         *
         * <ul>
         *   <li>for LwM2M 1.0: TLV,
         *   <li>for LwM2M 1.1: SenML CBOR, or if not compiled in, SenML JSON, or if not compiled in
         *       TLV.
         * </ul>
         */
        public boolean preferHierarchicalFormats;

        /** Enables support for DTLS connection_id extension for all DTLS connections. */
        public boolean useConnectionId;

        /**
         * (D)TLS ciphersuites to use if the "DTLS/TLS Ciphersuite" Resource (/0/x/16) is not
         * available or empty.
         *
         * <p>Passing an empty array or not setting at all will cause defaults of the TLS backend
         * library to be used.
         */
        public Optional<int[]> defaultTlsCiphersuites = Optional.empty();
    }

    /** DTLS handshake retransmission timeout limits for UDP connection. */
    public static class DtlsHandshakeTimeouts {
        public final Duration min;
        public final Duration max;

        public DtlsHandshakeTimeouts(Duration min, Duration max) {
            this.min = Objects.requireNonNull(min, "min duration MUST NOT be null");
            this.max = Objects.requireNonNull(max, "min duration MUST NOT be null");
        }
    }

    /** Enum defining available LwM2M versions to use. */
    public static enum Lwm2mVersion {
        /**
         * Lightweight Machine to Machine Technical Specification, Approved Version 1.0.2 - 09 Feb
         * 2018 (OMA-TS-LightweightM2M-V1_0_2-20180209-A).
         */
        VERSION_1_0("1.0"),
        /**
         * Lightweight Machine to Machine Technical Specification, Approved Version 1.1 -
         * 2018-07-10; Core (OMA-TS-LightweightM2M_Core-V1_1-20180710-A) and Transport Bindings
         * (OMA-TS-LightweightM2M_Transport-V1_1-20180710-A).
         */
        VERSION_1_1("1.1");

        private final String stringValue;

        private Lwm2mVersion(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return this.stringValue;
        }
    }

    /**
     * Configuration of LwM2M protocol versions to use when attempting to register to LwM2M servers.
     *
     * <ul>
     *   <li>Restricting the set of supported versions may speed up the Register operation, as less
     *       versions will be attempted for registration.
     *   <li>If {@link #minimumVersion} is set to a higher value than {@link #maximumVersion},
     *       {@link Anjay#Anjay} will fail.
     *   <li>If {@link #minimumVersion} is set to a version higher than LwM2M 1.0, {@link
     *       Configuration#disableLegacyServerInitiatedBootstrap} will be effectively implied even
     *       if that field is set to <code>false</code>.
     * </ul>
     */
    public static final class Lwm2mVersionConfig {
        /** The lowest version to attempt using when registering to LwM2M Servers. */
        public final Lwm2mVersion minimumVersion;
        /**
         * The highest version to attempt using when registering to LwM2M Servers. This is also the
         * version number sent in reponse to Bootstrap Discover.
         */
        public final Lwm2mVersion maximumVersion;

        /**
         * Constructor for LwM2M version configuration.
         *
         * @param minimum Minimum LwM2M version to use.
         * @param maximum Maximum LwM2M version to use.
         */
        public Lwm2mVersionConfig(Lwm2mVersion minimum, Lwm2mVersion maximum) {
            this.minimumVersion =
                    Objects.requireNonNull(minimum, "minimum LwM2M version MUST NOT be null");
            this.maximumVersion =
                    Objects.requireNonNull(maximum, "maximum LwM2M version MUST NOT be null");
        }
    }

    /**
     * CoAP transmission params object.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7252#section-4.8">RFC7252: Transmission
     *     Parameters</a>
     */
    public static final class CoapUdpTxParams {
        /** RFC 7252: ACK_TIMEOUT */
        public final Duration ackTimeout;
        /** RFC 7252: ACK_RANDOM_FACTOR */
        public final double ackRandomFactor;
        /** RFC 7252: MAX_RETRANSMIT */
        public final int maxRetransmit;
        /** RFC 7252: NSTART */
        public final int nstart;

        /** Default transmission params recommended by the CoAP specification (RFC 7252). */
        public static final CoapUdpTxParams DEFAULT =
                new CoapUdpTxParams(Duration.ofSeconds(2, 0), 1.5, 4, 1);

        /**
         * Constructor for transmission params object.
         *
         * @param ackTimeout RFC 7252: ACK_TIMEOUT. Must not be <code>null</code>.
         * @param ackRandomFactor RFC 7252: ACK_RANDOM_FACTOR
         * @param maxRetransmit RFC 7252: MAX_RETRANSMIT
         * @param nstart RFC 7252: NSTART
         */
        public CoapUdpTxParams(
                Duration ackTimeout, double ackRandomFactor, int maxRetransmit, int nstart) {
            this.ackTimeout = Objects.requireNonNull(ackTimeout, "ackTimeout MUST NOT be null");
            this.ackRandomFactor = ackRandomFactor;
            this.maxRetransmit = maxRetransmit;
            this.nstart = nstart;
        }
    }

    /** Object Link value referencing an Object Instance */
    public static final class Objlnk {
        /** Object ID */
        public final int oid;
        /** Instance ID */
        public final int iid;

        /**
         * Constructor for Object Link value.
         *
         * @param oid Object ID.
         * @param iid Instance ID.
         */
        public Objlnk(int oid, int iid) {
            this.oid = oid;
            this.iid = iid;
        }

        /** Checks if two Object Links are equal. */
        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || !(other instanceof Objlnk)) {
                return false;
            }
            return ((Objlnk) other).oid == oid && ((Objlnk) other).iid == iid;
        }

        /** Calculates hash of the Object Link. */
        @Override
        public int hashCode() {
            final int pair[] = {oid, iid};
            return Arrays.hashCode(pair);
        }
    }

    /** Type of transport used in connection to server. */
    public static enum Transport {
        /** UDP */
        UDP,
        /** TCP */
        TCP;
    }

    /** Available SSL versions that can be used by SSL sockets. */
    public static enum DtlsVersion {
        DEFAULT,
        SSLv2_OR_3,
        SSLv2,
        SSLv3,
        TLSv1,
        TLSv1_1,
        TLSv1_2
    }

    /** Details about established connection to the LwM2M Server. */
    public static final class SocketEntry {
        /** Channel used to communicate with server. */
        public final SelectableChannel channel;
        /** Used transport. */
        public final Transport transport;
        /** SSID of the server. */
        public final int ssid;
        /** Indicates if Queue Mode is enabled. */
        public final boolean queueMode;
        /** Used port. */
        public final int port;

        /** Constructor for SocketEntry - it is not intended to be called by user. */
        public SocketEntry(
                SelectableChannel channel,
                Transport transport,
                int ssid,
                boolean queueMode,
                int port) {
            this.channel = channel;
            this.transport = transport;
            this.ssid = ssid;
            this.queueMode = queueMode;
            this.port = port;
        }
    }

    /**
     * Creates a new Anjay object.
     *
     * @param config Initial configuration.
     */
    public Anjay(Configuration config) {
        NativeLog.initialize();
        this.anjay = new NativeAnjay(config);
    }

    /** Closes the Anjay object and deregisters the client from the servers. */
    @Override
    public void close() {
        this.anjay.close();
    }

    /**
     * Function returning the library version.
     *
     * @return String representing current version of the library.
     */
    public static String getVersion() {
        return NativeAnjay.getVersion();
    }

    /**
     * Runs all scheduled events which need to be invoked at or before the time of this function
     * invocation.
     */
    public void schedRun() {
        this.anjay.schedRun();
    }

    /**
     * Retrieves a list of sockets used for communication with LwM2M servers.
     *
     * <p>Example usage: <code>Selector</code>-based application loop
     *
     * <pre>{@code
     * try (Selector selector = Selector.open()) {
     *     while (true) {
     *         List<SelectableChannel> sockets = anjay.getSockets();
     *         for (SelectionKey key : selector.keys()) {
     *             if (!sockets.contains(key.channel())) {
     *                 key.cancel();
     *             }
     *         }
     *         for (SelectableChannel socket : sockets) {
     *             if (socket.keyFor(selector) == null) {
     *                 socket.register(selector, SelectionKey.OP_READ);
     *             }
     *         }
     *         selector.select(1000);
     *         for (Iterator<SelectionKey> it = selector.selectedKeys().iterator();
     *                 it.hasNext(); ) {
     *             anjay.serve(it.next().channel());
     *             it.remove();
     *         }
     *     }
     * }
     * }</pre>
     *
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @return A list of <code>SelectableChannel</code>s that correspond to valid server sockets.
     */
    public List<SelectableChannel> getSockets() {
        return this.getSocketEntries().stream()
                .map(entry -> entry.channel)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of objects that describe sockets used for communication with LwM2M servers.
     *
     * <p>The returned data is equivalent to the one that can be retrieved using {@link
     * #getSockets()} - but includes additional data that describes the socket in addition to the
     * socket itself.
     *
     * @return A list of valid server socket entries on success. If the the device is not connected
     *     to any server, the list is empty.
     */
    public List<SocketEntry> getSocketEntries() {
        return this.anjay.getSocketEntries();
    }

    /**
     * Reads a message from given <code>channel</code> and handles it appropriately.
     *
     * <p>Initially, the receive method on the underlying socket is called with receive timeout set
     * to zero. Subsequent receive requests may block with non-zero timeout values when e.g. waiting
     * for retransmissions or subsequent BLOCK chunks - this is necessary to hide this complexity
     * from the user callbacks in streaming mode.
     *
     * <p>This function may handle more than one request at once. Upon successful return, it is
     * guaranteed that there is no more data to be received on the socket at the moment.
     *
     * @param channel A channel to read the message from.
     * @throws Exception In case of failure.
     */
    public void serve(SelectableChannel channel) throws Exception {
        this.anjay.serve(channel);
    }

    /**
     * Determines time of next scheduled task.
     *
     * <p>May be used to determine how long the device may wait before calling {@link #schedRun()}.
     *
     * @return Relative time from now of next scheduled task, or an empty Optional, if no tasks are
     *     scheduled.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     */
    public Optional<Duration> timeToNext() {
        return this.anjay.timeToNext();
    }

    /**
     * Schedules sending an Update message to the server identified by given Short Server ID.
     *
     * <p>The Update will be sent during the next {@link #schedRun()} call.
     *
     * <p>Note: This method will not schedule registration update if Anjay is in offline mode.
     *
     * @param ssid Short Server ID of the server to send Update to, or {@link #SSID_ANY} to send
     *     Updates to all connected servers. NOTE: Since Updates are not useful for the Bootstrap
     *     server, this method does not send one for {@link #SSID_BOOTSTRAP}.
     * @throws IllegalArgumentException If <code>ssid</code> is not representable as a 16-bit
     *     unsigned integer.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the Update cannot be scheduled for any reason, which may include
     *     being in offline state, no active server with a given SSID, or out-of-memory condition.
     */
    public void scheduleRegistrationUpdate(int ssid) {
        this.anjay.scheduleRegistrationUpdate(ssid);
    }

    /**
     * Reconnects sockets associated with all servers and ongoing downloads over the specified
     * transports. Should be called if something related to the connectivity over those transports
     * changes.
     *
     * <p>The reconnection will be performed during the next {@link #schedRun()} call and will
     * trigger sending any messages necessary to maintain valid registration (DTLS session
     * resumption and/or Register or Update RPCs).
     *
     * <p>In case of ongoing downloads (started via {@link AnjayDownload} or the {@link
     * AnjayFirmwareUpdate} module), if the reconnection fails, the download will be aborted with an
     * error.
     *
     * <p>Note: This function puts all the transports in <code>transportSet</code> into online mode.
     *
     * <p>The reconnection will be performed during the next {@link #schedRun()} call and will
     * trigger Registration Update. Note: This method makes Anjay enter online mode.
     *
     * @param transportSet Set of transports whose sockets shall be reconnected.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the reconnection cannot be scheduled for any reason, which may
     *     include an out-of-memory condition.
     */
    public void scheduleReconnect(Set<Transport> transportSet) {
        this.anjay.scheduleReconnect(transportSet);
    }

    /**
     * Overload, that issues reconnection for all transports. See {@link #scheduleReconnect(Set)}
     * for more details.
     */
    public void scheduleReconnect() {
        this.scheduleReconnect(EnumSet.allOf(Transport.class));
    }

    /**
     * This method shall be called when an LwM2M Server Object shall be disabled. The standard case
     * for this is when Execute is performed on the Disable resource (/1/x/4).
     *
     * <p>The server will be disabled for the period of time determined by the value of the Disable
     * Timeout resource (/1/x/5). The resource is read soon after the invocation of this method
     * (during next {@link #schedRun()}) and is <strong>not</strong> updated upon any subsequent
     * Writes to that resource.
     *
     * @param ssid Short Server ID of the server to put in a disabled state. NOTE: disabling a
     *     server requires a Server Object Instance to be present for given <code>ssid</code>.
     *     Because the Bootstrap Server does not have one, this method does nothing when called with
     *     {@link #SSID_BOOTSTRAP}.
     * @throws IllegalArgumentException If <code>ssid</code> is not representable as a 16-bit
     *     unsigned integer.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the disable job cannot be scheduled for any reason, which may
     *     include an out-of-memory condition.
     */
    public void disableServer(int ssid) {
        this.anjay.disableServer(ssid);
    }

    /**
     * Schedules a job for re-enabling a previously disabled (with a call to {@link
     * #disableServerWithTimeout}) server. The server will be enabled during next {@link
     * #schedRun()} call.
     *
     * @param ssid Short Server ID of the server to enable.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the disable job cannot be scheduled for any reason, which may
     *     include an out-of-memory condition.
     */
    public void enableServer(int ssid) {
        this.anjay.enableServer(ssid);
    }

    /**
     * This function shall be called when an LwM2M Server Object shall be disabled. The standard
     * case for this is when Execute is performed on the Disable resource (/1/x/4). It may also be
     * used to prevent reconnections if the server becomes unreachable.
     *
     * <p>The server will become disabled during next {@link #schedRun()} call.
     *
     * <p>NOTE: disabling a server with dual binding (e.g. UDP+SMS trigger) closes both
     * communication channels. Shutting down only one of them requires changing the Binding Resource
     * in Server object.
     *
     * @param ssid Short Server ID of the server to put in a disabled state.
     * @param timeout Disable timeout. If set to {@link Optional#empty()}, the server will remain
     *     disabled until explicit call to {@link #enableServer}. Otherwise, the server will get
     *     enabled automatically after <code>timeout</code>.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     */
    public void disableServerWithTimeout(int ssid, Optional<Duration> timeout) {
        this.anjay.disableServerWithTimeout(ssid, timeout);
    }

    /**
     * Checks whether all the specified transports are in offline mode.
     *
     * @param transportSet Set of transports to check.
     * @return true if all of the transports specified by <code>transportSet</code> are in offline
     *     mode, false otherwise.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     */
    public boolean isOffline(Set<Transport> transportSet) {
        return this.anjay.isOffline(transportSet);
    }

    /**
     * Checks whether all transports are in offline mode. Overload of {@link #isOffline(Set)}, see
     * its documentation for more details.
     *
     * @return true if all transports are in offline mode.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     */
    public boolean isOffline() {
        return this.isOffline(EnumSet.allOf(Transport.class));
    }

    /**
     * Puts all the transports specified by <code>transportSet</code> into offline mode. This should
     * be done when the connectivity for these transports is deemed unavailable or lost.
     *
     * <p>During subsequent calls to {@link #schedRun()}, Anjay will close all of the sockets
     * corresponding to the specified transport and stop attempting to make any contact with remote
     * hosts over it, until a call to {@link #exitOffline()} for any of the corresponding
     * transports.
     *
     * <p>Note that offline mode also affects downloads. E.g., putting the TCP transport into
     * offline mode will pause all ongoing downloads over TCP and prevent new such download requests
     * from being performed.
     *
     * <p>User code shall still interface normally with the library, even if all the transports are
     * in the offline state. This include regular calls to {@link #schedRun()}. Notifications (as
     * reported using {@link #notifyChanged notifyChanged()} and {@link #notifyInstancesChanged
     * notifyInstancesChanged()}) continue to be tracked, and may be sent after reconnecting,
     * depending on values of the "Notification Storing When Disabled or Offline" resource.
     *
     * @param transportSet Set of transports to put into offline mode.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     */
    public void enterOffline(Set<Transport> transportSet) {
        this.anjay.enterOffline(transportSet);
    }

    /**
     * Puts all transports into offline mode. Overload of {@link #enterOffline(Set)}, see its
     * documentation for more details.
     */
    public void enterOffline() {
        this.enterOffline(EnumSet.allOf(Transport.class));
    }

    /**
     * Puts all the transports specified by <code>transportSet</code> back into online mode, if any
     * of them were previously put into offline mode using {@link #enterOffline()}.
     *
     * <p>Transports that are unavailable due to compile-time or runtime configuration are ignored.
     *
     * <p>During subsequent calls to {@link #schedRun()}, new connections to all LwM2M servers
     * disconnected due to offline mode will be attempted, and Register or Registration Update
     * messages will be sent as appropriate. Downloads paused due to offline mode will be resumed as
     * well.
     *
     * @param transportSet Set of transports to put into offline mode.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the job cannot be scheduled for any reason, which may include an
     *     out-of-memory condition.
     */
    public void exitOffline(Set<Transport> transportSet) {
        this.anjay.exitOffline(transportSet);
    }

    /**
     * Puts all transports back into online mode. Overload of {@link #exitOffline(Set)}, see its
     * documentation for more details.
     */
    public void exitOffline() {
        this.exitOffline(EnumSet.allOf(Transport.class));
    }

    /**
     * Notifies the library that the value of given Resource changed. It may trigger an LwM2M Notify
     * message, update server connections and perform other tasks, as required for the specified
     * Resource.
     *
     * <p>Needs to be called for any Resource after its value is changed by means other than LwM2M.
     *
     * <p>Note that it should not be called after a Write performed by the LwM2M server.
     *
     * @param oid Object ID of the changed Resource.
     * @param iid Object Instance ID of the changed Resource.
     * @param rid Resource ID of the changed Resource.
     * @throws IllegalArgumentException If any of <code>oid</code>, <code>iid</code> or <code>rid
     *     </code> is not representable as a 16-bit unsigned integer.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the notification cannot be scheduled for any reason, which may
     *     include an out-of-memory condition.
     */
    public void notifyChanged(int oid, int iid, int rid) {
        this.anjay.notifyChanged(oid, iid, rid);
    }

    /**
     * Notifies the library that the set of Instances existing in a given Object changed. It may
     * trigger an LwM2M Notify message, update server connections and perform other tasks, as
     * required for the specified Object ID.
     *
     * <p>Needs to be called for each Object, after an Instance is created or removed by means other
     * than LwM2M.
     *
     * <p>Note that it should not be called after a Create or Delete performed by the LwM2M server.
     *
     * @param oid Object ID of the changed Object.
     * @throws IllegalArgumentException If <code>oid</code> is not representable as a 16-bit
     *     unsigned integer.
     * @throws IllegalStateException If {@link #close()} has already been called on this object.
     * @throws AnjayException If the notification cannot be scheduled for any reason, which may
     *     include an out-of-memory condition.
     */
    public void notifyInstancesChanged(int oid) {
        this.anjay.notifyInstancesChanged(oid);
    }

    /**
     * Registers the Object in the data model, making it available for RPC calls.
     *
     * @param object Previously created Object instance.
     */
    public void registerObject(AnjayObject object) {
        this.anjay.registerObject(object);
    }

    /**
     * Queries security configuration appropriate for a specified URI.
     *
     * <p>Given a URI, the Security object is scanned for instances with Server URI resource
     * matching it in the following way:
     *
     * <ul>
     *   <li>if there is at least one instance with matching hostname, protocol and port number, and
     *       valid secure connection configuration, the first such instance (in the order as
     *       returned via Security Object's instance list handler) is used,
     *   <li>otherwise, if there is at least one instance with matching hostname and valid secure
     *       connection configuration, the first such instance (in the order as returned via
     *       Security Object's instance list handler) is used.
     * </ul>
     *
     * <p>The returned security information is exactly the same configuration that is used for LwM2M
     * connection with the server chosen with the rules described above.
     *
     * <p>IMPORTANT: The returned (if any) security configuration is valid only as long as no calls
     * to {@link #schedRun} or {@link #serve} (or equivalent) are made, and as long as the
     * underlying data model from which the security configuration is obtained does not change.
     * Otherwise it MAY be invalidated and {@link java.util.ConcurrentModificationException} may be
     * thrown in contexts where the returned instance is used.
     *
     * @param uri URI for which to find security configuration.
     * @return {@link AnjaySecurityConfigFromDm} instance or empty Optional if no security
     *     information for the specified url could be found.
     */
    public Optional<AnjaySecurityConfigFromDm> securityConfigFromDm(String uri) {
        if (this.anjay.hasSecurityConfigForUri(uri)) {
            return Optional.of(new AnjaySecurityConfigFromDm(uri));
        }
        return Optional.empty();
    }
}
