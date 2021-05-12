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

#pragma once

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/download_configuration.hpp"
#include "./util_classes/download_handlers.hpp"

#include <anjay/download.h>

class NativeAnjayDownload {
    std::weak_ptr<anjay_t> anjay_;
    anjay_download_handle_t handle_;
    utils::AccessorBase<utils::DownloadHandlers> accessor_;

    static avs_error_t next_block_handler(anjay_t *anjay,
                                          const uint8_t *data,
                                          size_t data_size,
                                          const anjay_etag_t *etag,
                                          void *user_data);

    static void download_finished_handler(anjay_t *anjay,
                                          anjay_download_status_t status,
                                          void *user_data);

    NativeAnjayDownload(const NativeAnjayDownload &) = delete;
    NativeAnjayDownload &operator=(const NativeAnjayDownload &) = delete;

    NativeAnjayDownload(NativeAnjayDownload &&) = delete;
    NativeAnjayDownload &operator=(NativeAnjayDownload &&) = delete;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeAnjayDownload";
    }

    NativeAnjayDownload(jni::JNIEnv &env,
                        const jni::Object<NativeAnjay> &anjay,
                        const jni::Object<utils::DownloadConfiguration> &config,
                        const jni::Object<utils::DownloadHandlers> &handlers);

    ~NativeAnjayDownload();

    static void register_native(jni::JNIEnv &env);

    void abort(jni::JNIEnv &env);
};
