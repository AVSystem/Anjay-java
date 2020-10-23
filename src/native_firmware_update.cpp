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

#include "./native_firmware_update.hpp"
#include "./util_classes/firmware_update_initial_state.hpp"
#include "./util_classes/firmware_update_result.hpp"

#include <anjay/fw_update.h>

int NativeFirmwareUpdate::stream_open(
        void *user_ptr,
        const char *package_uri,
        const struct anjay_etag *package_etag) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    return obj->accessor_.stream_open(package_uri, package_etag);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeFirmwareUpdate::stream_write(void *user_ptr,
                                       const void *data,
                                       size_t length) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    return obj->accessor_.stream_write(data, length);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeFirmwareUpdate::stream_finish(void *user_ptr) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    return obj->accessor_.stream_finish();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

void NativeFirmwareUpdate::reset(void *user_ptr) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    obj->accessor_.reset();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
}

const char *NativeFirmwareUpdate::get_name(void *user_ptr) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    obj->name_ = obj->accessor_.get_name();
    if (obj->name_) {
        return obj->name_->c_str();
    } else {
        return nullptr;
    }
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return nullptr;
}

const char *NativeFirmwareUpdate::get_version(void *user_ptr) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    obj->version_ = obj->accessor_.get_version();
    if (obj->version_) {
        return obj->version_->c_str();
    } else {
        return nullptr;
    }
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return nullptr;
}

int NativeFirmwareUpdate::perform_upgrade(void *user_ptr) try {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    return obj->accessor_.perform_upgrade();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

avs_coap_udp_tx_params_t
NativeFirmwareUpdate::get_coap_tx_params(void *user_ptr,
                                         const char *download_uri) {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    try {
        auto tx_params = obj->accessor_.get_coap_tx_params(download_uri);
        if (tx_params) {
            return *tx_params;
        }
    } catch (...) {
        avs_log_and_clear_exception(DEBUG);
    }
    return obj->anjay_tx_params_;
}

int NativeFirmwareUpdate::get_security_config(
        void *user_ptr,
        anjay_security_config_t *out_security_info,
        const char *download_uri) {
    NativeFirmwareUpdate *obj = static_cast<NativeFirmwareUpdate *>(user_ptr);
    try {
        obj->security_config_ =
                obj->accessor_.get_security_config(obj->anjay_, download_uri);

        if (obj->security_config_) {
            *out_security_info = obj->security_config_->get_config();
            return 0;
        } else if (auto anjay = obj->anjay_.lock()) {
            return anjay_security_config_from_dm(anjay.get(), out_security_info,
                                                 download_uri);
        } else {
            avs_throw(std::runtime_error("anjay object expired"));
        }
    } catch (...) {
        avs_log_and_clear_exception(DEBUG);
        return -1;
    }
}

NativeFirmwareUpdate::NativeFirmwareUpdate(
        jni::JNIEnv &env,
        const jni::Object<NativeAnjay> &anjay,
        const jni::Object<utils::FirmwareUpdateHandlers> &handlers,
        const jni::Object<utils::FirmwareUpdateInitialState> &initial_state)
        : anjay_(),
          handlers_{ stream_open,       stream_write,
                     stream_finish,     reset,
                     get_name,          get_version,
                     perform_upgrade,   get_security_config,
                     get_coap_tx_params },
          accessor_(env, handlers) {
    auto native_anjay = NativeAnjay::into_native(env, anjay);
    anjay_ = native_anjay->get_anjay();

    anjay_tx_params_ = native_anjay->get_udp_tx_params();

    auto state_accessor =
            utils::FirmwareUpdateInitialState::Accessor{ env, initial_state };
    anjay_fw_update_initial_state_t state{};
    state.result = state_accessor.get_result();

    auto persisted_uri = state_accessor.get_persisted_uri();
    if (persisted_uri) {
        state.persisted_uri = persisted_uri->c_str();
    }

    state.resume_offset = state_accessor.get_resume_offset();

    auto resume_etag = state_accessor.get_resume_etag();
    if (resume_etag) {
        state.resume_etag = resume_etag->data;
    }

    if (auto locked = anjay_.lock()) {
        if (anjay_fw_update_install(locked.get(), &handlers_, this, &state)) {
            avs_throw(std::runtime_error(
                    "Could not install firmware update object"));
        }
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

void NativeFirmwareUpdate::set_result(
        jni::JNIEnv &env,
        const jni::Object<utils::FirmwareUpdateResult> &result) {
    if (auto locked = anjay_.lock()) {
        anjay_fw_update_set_result(
                locked.get(),
                utils::FirmwareUpdateResult::into_native(env, result));
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

void NativeFirmwareUpdate::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, Name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(Name)
#define STATIC_METHOD(MethodPtr, Name) \
    jni::MakeNativeMethod<decltype(MethodPtr), (MethodPtr)>(Name)

    // clang-format off
    jni::RegisterNativePeer<NativeFirmwareUpdate>(
            env, jni::Class<NativeFirmwareUpdate>::Find(env), "self",
            jni::MakePeer<NativeFirmwareUpdate, jni::Object<NativeAnjay> &,
                          jni::Object<utils::FirmwareUpdateHandlers> &,
                          jni::Object<utils::FirmwareUpdateInitialState> &>,
            "init",
            "cleanup",
            METHOD(&NativeFirmwareUpdate::set_result, "nativeSetResult"));

    jni::RegisterNatives(
            env, *jni::Class<NativeFirmwareUpdate>::Find(env),
            STATIC_METHOD(&NativeFirmwareUpdate::get_error_not_enough_space, "getErrorNotEnoughSpace"),
            STATIC_METHOD(&NativeFirmwareUpdate::get_error_out_of_memory, "getErrorOutOfMemory"),
            STATIC_METHOD(&NativeFirmwareUpdate::get_error_integrity_failure, "getErrorIntegrityFailure"),
            STATIC_METHOD(&NativeFirmwareUpdate::get_error_unsupported_package_type, "getErrorUnsupportedPackageType"));
    // clang-format on

#undef METHOD
#undef STATIC_METHOD
}
