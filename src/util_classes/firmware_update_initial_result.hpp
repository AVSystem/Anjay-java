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

#include <optional>
#include <unordered_map>

#include "../jni_wrapper.hpp"

#include <anjay/fw_update.h>

#include "./accessor_base.hpp"
#include "./etag.hpp"

namespace utils {

struct FirmwareUpdateInitialResult {
    static constexpr auto Name() {
        return "com/avsystem/anjay/"
               "AnjayFirmwareUpdate$InitialState$InitialResult";
    }

    static anjay_fw_update_initial_result_t
    into_native(jni::JNIEnv &env,
                const jni::Object<FirmwareUpdateInitialResult> &instance) {
        static std::unordered_map<std::string, anjay_fw_update_initial_result_t>
                MAPPING{ { "UPDATING", ANJAY_FW_UPDATE_INITIAL_UPDATING },
                         { "DOWNLOADED", ANJAY_FW_UPDATE_INITIAL_DOWNLOADED },
                         { "DOWNLOADING", ANJAY_FW_UPDATE_INITIAL_DOWNLOADING },
                         { "NEUTRAL", ANJAY_FW_UPDATE_INITIAL_NEUTRAL },
                         { "SUCCESS", ANJAY_FW_UPDATE_INITIAL_SUCCESS },
                         { "INTEGRITY_FAILURE",
                           ANJAY_FW_UPDATE_INITIAL_INTEGRITY_FAILURE },
                         { "FAILED", ANJAY_FW_UPDATE_INITIAL_FAILED } };
        auto clazz = jni::Class<FirmwareUpdateInitialResult>::Find(env);
        auto value = jni::Make<std::string>(
                env,
                instance.Call(env,
                              clazz.GetMethod<jni::String()>(env, "name")));
        auto mapped_to = MAPPING.find(value);
        if (mapped_to == MAPPING.end()) {
            avs_throw(std::runtime_error("Unsupported enum value: " + value));
        }
        return mapped_to->second;
    }
};

} // namespace utils
