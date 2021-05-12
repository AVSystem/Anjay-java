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

import com.avsystem.anjay.AnjayDownload.Result;
import com.avsystem.anjay.AnjayDownload.ResultDetails;
import java.util.Optional;

/** Interface specifying handlers to be called during download. */
public interface AnjayDownloadHandlers {
    /**
     * Called after receiving a chunk of data from remote server.
     *
     * @param data Received chunk of data.
     * @param etag ETag of the data.
     * @throws Exception If an error occurred, in which case the download will be terminated with
     *     {@link Result#FAILED FAILED} result.
     */
    void onNextBlock(byte[] data, Optional<byte[]> etag) throws Exception;

    /**
     * Called after the download is finished or aborted.
     *
     * @param result Result of the download.
     * @param details Contains details about failure if result is {@link Result#FAILED FAILED} or
     *     {@link Result#INVALID_RESPONSE INVALID_RESPONSE}.
     */
    void onDownloadFinished(Result result, Optional<ResultDetails> details);
}
