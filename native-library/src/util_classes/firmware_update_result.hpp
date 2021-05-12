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

#include <optional>
#include <unordered_map>

#include "../jni_wrapper.hpp"

#include <anjay/fw_update.h>

#include "./accessor_base.hpp"
#include "./etag.hpp"

namespace utils {

struct FirmwareUpdateResult {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayFirmwareUpdate$Result";
    }

    static anjay_fw_update_result_t
    into_native(jni::JNIEnv &env,
                const jni::Object<FirmwareUpdateResult> &result) {
        static std::unordered_map<std::string, anjay_fw_update_result_t>
                MAPPING{ { "INITIAL", ANJAY_FW_UPDATE_RESULT_INITIAL },
                         { "SUCCESS", ANJAY_FW_UPDATE_RESULT_SUCCESS },
                         { "NOT_ENOUGH_SPACE",
                           ANJAY_FW_UPDATE_RESULT_NOT_ENOUGH_SPACE },
                         { "OUT_OF_MEMORY",
                           ANJAY_FW_UPDATE_RESULT_OUT_OF_MEMORY },
                         { "CONNECTION_LOST",
                           ANJAY_FW_UPDATE_RESULT_CONNECTION_LOST },
                         { "INTEGRITY_FAILURE",
                           ANJAY_FW_UPDATE_RESULT_INTEGRITY_FAILURE },
                         { "UNSUPPORTED_PACKAGE_TYPE",
                           ANJAY_FW_UPDATE_RESULT_UNSUPPORTED_PACKAGE_TYPE },
                         { "INVALID_URI", ANJAY_FW_UPDATE_RESULT_INVALID_URI },
                         { "FAILED", ANJAY_FW_UPDATE_RESULT_FAILED },
                         { "UNSUPPORTED_PROTOCOL",
                           ANJAY_FW_UPDATE_RESULT_UNSUPPORTED_PROTOCOL } };
        auto clazz = jni::Class<FirmwareUpdateResult>::Find(env);
        auto value = jni::Make<std::string>(
                env,
                result.Call(env, clazz.GetMethod<jni::String()>(env, "name")));
        auto mapped_to = MAPPING.find(value);
        if (mapped_to == MAPPING.end()) {
            avs_throw(IllegalArgumentException(
                    env, "Unsupported enum value: " + value));
        }
        return mapped_to->second;
    }
};

} // namespace utils
