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

#include "./native_anjay_download.hpp"
#include "./util_classes/accessor_base.hpp"
#include "./util_classes/byte_buffer.hpp"
#include "./util_classes/download_result.hpp"
#include "./util_classes/download_result_details.hpp"
#include "./util_classes/security_config.hpp"

#include "global_context.hpp"

NativeAnjayDownload::NativeAnjayDownload(
        jni::JNIEnv &env,
        const jni::Object<NativeAnjay> &anjay,
        const jni::Object<utils::DownloadConfiguration> &config,
        const jni::Object<utils::DownloadHandlers> &handlers)
        : anjay_(), accessor_(env, handlers) {
    auto native_anjay = NativeAnjay::into_native(env, anjay);
    anjay_ = native_anjay->get_anjay();

    auto config_accessor =
            utils::DownloadConfiguration::Accessor{ env, config };

    auto url = config_accessor.get_url();
    if (!url) {
        avs_throw(IllegalArgumentException(env, "url MUST be set"));
    }
    auto start_offset = config_accessor.get_start_offset();

    anjay_download_config_t download_config{};
    download_config.url = url->c_str();
    download_config.start_offset = start_offset;
    download_config.on_next_block = next_block_handler;
    download_config.on_download_finished = download_finished_handler;
    download_config.user_data = this;

    auto etag = config_accessor.get_etag();
    if (etag) {
        download_config.etag = etag->data;
    }

    auto coap_tx_params = config_accessor.get_coap_tx_params();
    if (coap_tx_params) {
        download_config.coap_tx_params = &*coap_tx_params;
    }
    std::optional<utils::SecurityConfig> security_config;
    if (auto config = config_accessor.get_optional_value<utils::SecurityConfig>(
                "securityConfig")) {
        security_config.emplace(anjay_, env, *config);
    }

    if (auto locked = anjay_.lock()) {
        if (security_config) {
            download_config.security_config = security_config->get_config();
        }
        auto err = anjay_download(locked.get(), &download_config, &handle_);

        if (avs_is_err(err)) {
            avs_throw(AnjayException(
                    env,
                    (err.category << (CHAR_BIT * sizeof(err.code))) | err.code,
                    "Could not start download"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

NativeAnjayDownload::~NativeAnjayDownload() {
    if (auto locked = anjay_.lock()) {
        anjay_download_abort(locked.get(), handle_);
    }
}

avs_error_t NativeAnjayDownload::next_block_handler(anjay_t *,
                                                    const uint8_t *data,
                                                    size_t data_size,
                                                    const anjay_etag_t *etag,
                                                    void *user_data) try {
    return GlobalContext::call_with_env([=](jni::UniqueEnv &&env) {
        std::vector<jni::jbyte> buffer(data, data + data_size);

        auto make_etag = [&](const anjay_etag_t *etag) {
            if (etag) {
                std::vector<jni::jbyte> etag_vec(etag->value,
                                                 etag->value + etag->size);
                return utils::Optional::of(
                        *env,
                        jni::Make<jni::Array<jni::jbyte>>(*env, etag_vec));
            } else {
                return utils::Optional::empty(*env);
            }
        };

        NativeAnjayDownload *obj =
                static_cast<NativeAnjayDownload *>(user_data);

        std::vector<jni::jbyte> data_vec(data, data + data_size);
        obj->accessor_.get_method<void(jni::Array<jni::jbyte>,
                                       jni::Object<utils::Optional>)>(
                "onNextBlock")(jni::Make<jni::Array<jni::jbyte>>(*env,
                                                                 data_vec),
                               make_etag(etag).into_java());
        return AVS_OK;
    });
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return avs_errno(AVS_EIO);
}

void NativeAnjayDownload::download_finished_handler(
        anjay_t *, anjay_download_status_t status, void *user_data) try {
    NativeAnjayDownload *obj = static_cast<NativeAnjayDownload *>(user_data);
    obj->handle_ = nullptr;
    GlobalContext::call_with_env([=](jni::UniqueEnv &&env) {
        auto result = utils::DownloadResult::into_java(*env, status.result);
        auto make_details = [&](anjay_download_status_t status) {
            if (status.result == ANJAY_DOWNLOAD_ERR_FAILED) {
                return utils::Optional::of(
                        *env, utils::DownloadResultDetails::into_java(
                                      *env, status.details.error));
            } else if (status.result == ANJAY_DOWNLOAD_ERR_INVALID_RESPONSE) {
                return utils::Optional::of(
                        *env, utils::DownloadResultDetails::into_java(
                                      *env, status.details.status_code));
            } else {
                return utils::Optional::empty(*env);
            }
        };
        obj->accessor_.get_method<void(jni::Object<utils::DownloadResult>,
                                       jni::Object<utils::Optional>)>(
                "onDownloadFinished")(result, make_details(status).into_java());
    });
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
}

void NativeAnjayDownload::abort(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        anjay_download_abort(locked.get(), handle_);
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAnjayDownload::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeAnjayDownload>(
            env, jni::Class<NativeAnjayDownload>::Find(env), "self",
            jni::MakePeer<NativeAnjayDownload, jni::Object<NativeAnjay> &,
                          jni::Object<utils::DownloadConfiguration> &,
                          jni::Object<utils::DownloadHandlers> &>,
            "init",
            "cleanup",
            METHOD(&NativeAnjayDownload::abort, "downloadAbort"));
    // clang-format on

#undef METHOD
}
