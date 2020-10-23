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

import com.avsystem.anjay.Anjay.CoapUdpTxParams;
import com.avsystem.anjay.impl.NativeAnjayDownload;
import java.util.ConcurrentModificationException;
import java.util.Optional;

/** Manages downloads started by user. */
public final class AnjayDownload {

    /** Configuration for {@link AnjayDownload}. */
    public static final class Configuration {
        /**
         * <code>coap://</code>, <code>coaps://</code>, <code>http://</code> or <code>https://
         * </code> URL. Required.
         */
        public String url;

        /**
         * If {@link #startOffset} is not 0, etag should be set to a value returned by the server
         * during the transfer before it got interrupted.
         */
        public Optional<byte[]> etag = Optional.empty();

        /**
         * If the download gets interrupted for some reason, and the client is aware of how much
         * data it managed to successfully download, it can resume the transfer from a specific
         * offset.
         */
        public int startOffset;

        /**
         * DTLS security configuration. Required if <code>coaps://</code> is used, ignored for
         * <code>coap://</code> transfers.
         */
        public Optional<AnjayAbstractSecurityConfig> securityConfig = Optional.empty();

        /**
         * CoAP transmission parameters object. If empty, downloader will inherit parameters from
         * Anjay.
         */
        public Optional<CoapUdpTxParams> coapTxParams = Optional.empty();
    }

    /** Result of the download process. */
    public static enum Result {
        /** Download finished successfully. */
        FINISHED,
        /** Download failed due to a local failure or a network error. */
        FAILED,
        /**
         * The remote server responded in a way that is permitted by the protocol, but does not
         * indicate a success (e.g. a 4xx or 5xx HTTP status).
         */
        INVALID_RESPONSE,
        /** Downloaded resource changed while transfer was in progress. */
        EXPIRED,
        /** Download was aborted by calling {@link AnjayDownload#abort()} */
        ABORTED
    };

    /** Details about error occured during download. */
    public static enum ResultDetails {
        DNS_RESOLUTION_FAILED,
        REMOTE_RESOURCE_NO_LONGER_VALID,
        SERVER_RESPONDED_WITH_RESET,
        CONNECTION_LOST,
        FAILED_TO_PARSE_RESPONSE,
        INTERNAL_ERROR,
        MESSAGE_TOO_LARGE,
        OUT_OF_MEMORY,
        TIMEOUT,
        NOT_FOUND,
        NOT_IMPLEMENTED,
        UNKNOWN
    };

    private final NativeAnjayDownload download;

    private AnjayDownload(Anjay anjay, Configuration config, AnjayDownloadHandlers handlers)
            throws Exception {
        this.download = new NativeAnjayDownload(anjay, config, handlers);
    }

    /**
     * Requests asynchronous download of an external resource.
     *
     * <p>Download will create a new socket that will be later included in the list returned by
     * {@link Anjay#getSockets}. Calling {@link Anjay#serve serve} on such socket may cause calling
     * {@link AnjayDownloadHandlers#onNextBlock onNextBlock} if received packet is the next expected
     * chunk of downloaded data and {@link AnjayDownloadHandlers#onDownloadFinished
     * onDownloadFinished} if the transfer completes or fails. Request packet retransmissions are
     * managed by Anjay scheduler, and sent by {@link Anjay#serve serve}.
     *
     * @param anjay Anjay instance to be used for performing download.
     * @param config Download configuration.
     * @param handlers Implementation of handlers to be called by downloader.
     * @return {@link AnjayDownload} object which may be used to abort the download process.
     * @throws Exception If config is invalid or download can't be started.
     * @throws ConcurrentModificationException if the security configuration used {@link
     *     AnjaySecurityConfigFromDm} and the configuration expired due to {@link Anjay#serve} or
     *     {@link Anjay#schedRun} calls happening in between.
     */
    public static AnjayDownload startDownload(
            Anjay anjay, Configuration config, AnjayDownloadHandlers handlers)
            throws Exception, ConcurrentModificationException {
        return new AnjayDownload(anjay, config, handlers);
    }

    /** Abort the ongoing download. Does nothing if the download is already finished. */
    public void abort() {
        this.download.abort();
    }
}
