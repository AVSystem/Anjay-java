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

#pragma once

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"
#include "./util_classes/firmware_update_handlers.hpp"
#include "./util_classes/firmware_update_initial_state.hpp"
#include "./util_classes/firmware_update_result.hpp"
#include "./util_classes/security_config.hpp"

#include <anjay/fw_update.h>

class NativeFirmwareUpdate {
    static int stream_open(void *user_ptr,
                           const char *package_uri,
                           const struct anjay_etag *package_etag);

    static int stream_write(void *user_ptr, const void *data, size_t length);

    static int stream_finish(void *user_ptr);

    static void reset(void *user_ptr);

    static const char *get_name(void *user_ptr);

    static const char *get_version(void *user_ptr);

    static int perform_upgrade(void *user_ptr);

    static int get_security_config(void *user_ptr,
                                   anjay_security_config_t *out_security_info,
                                   const char *download_uri);

    static avs_coap_udp_tx_params_t
    get_coap_tx_params(void *user_ptr, const char *download_uri);

    std::weak_ptr<anjay_t> anjay_;
    anjay_fw_update_handlers_t handlers_;
    utils::FirmwareUpdateHandlers::Accessor accessor_;
    std::optional<std::string> name_;
    std::optional<std::string> version_;
    std::unique_ptr<utils::SecurityConfig> security_config_;
    avs_coap_udp_tx_params_t anjay_tx_params_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeFirmwareUpdate";
    }

    NativeFirmwareUpdate(
            jni::JNIEnv &env,
            const jni::Object<NativeAnjay> &anjay,
            const jni::Object<utils::FirmwareUpdateHandlers> &handlers,
            const jni::Object<utils::FirmwareUpdateInitialState>
                    &initial_state);

    static void register_native(jni::JNIEnv &env);

    void set_result(jni::JNIEnv &env,
                    const jni::Object<utils::FirmwareUpdateResult> &result);

    static jni::jint
    get_error_not_enough_space(jni::JNIEnv &,
                               jni::Class<NativeFirmwareUpdate> &) {
        return ANJAY_FW_UPDATE_ERR_NOT_ENOUGH_SPACE;
    }

    static jni::jint
    get_error_out_of_memory(jni::JNIEnv &, jni::Class<NativeFirmwareUpdate> &) {
        return ANJAY_FW_UPDATE_ERR_OUT_OF_MEMORY;
    }

    static jni::jint
    get_error_integrity_failure(jni::JNIEnv &,
                                jni::Class<NativeFirmwareUpdate> &) {
        return ANJAY_FW_UPDATE_ERR_INTEGRITY_FAILURE;
    }

    static jni::jint
    get_error_unsupported_package_type(jni::JNIEnv &,
                                       jni::Class<NativeFirmwareUpdate> &) {
        return ANJAY_FW_UPDATE_ERR_UNSUPPORTED_PACKAGE_TYPE;
    }
};
