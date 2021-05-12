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

package com.avsystem.anjay.impl;

import com.avsystem.anjay.Anjay;
import com.avsystem.anjay.AnjayDownload.Configuration;
import com.avsystem.anjay.AnjayDownload.Result;
import com.avsystem.anjay.AnjayDownload.ResultDetails;
import com.avsystem.anjay.AnjayDownloadHandlers;
import java.util.Optional;

public final class NativeAnjayDownload {
    private long self;
    private final AnjayDownloadHandlers handlers;

    private native void init(
            NativeAnjay anjay, Configuration config, AnjayDownloadHandlers handlers);

    private native void cleanup();

    private native void downloadAbort();

    public NativeAnjayDownload(Anjay anjay, Configuration config, AnjayDownloadHandlers handlers)
            throws Exception {
        this.handlers = handlers;
        this.init(NativeUtils.getNativeAnjay(anjay), config, handlers);
    }

    private void onNextBlock(byte[] data, Optional<byte[]> etag) throws Exception {
        handlers.onNextBlock(data, etag);
    }

    private void onDownloadFinished(Result result, Optional<ResultDetails> details) {
        handlers.onDownloadFinished(result, details);
    }

    public void abort() {
        downloadAbort();
    }
    ;
}
