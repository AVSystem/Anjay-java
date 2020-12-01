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

#include "../jni_wrapper.hpp"

#include <anjay/fw_update.h>

#include "./accessor_base.hpp"
#include "./etag.hpp"
#include "./firmware_update_initial_result.hpp"

namespace utils {

struct FirmwareUpdateInitialState {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayFirmwareUpdate$InitialState";
    }

    class Accessor : public AccessorBase<FirmwareUpdateInitialState> {
    public:
        explicit Accessor(
                jni::JNIEnv &env,
                const jni::Object<FirmwareUpdateInitialState> &initial_state)
                : AccessorBase(env, initial_state) {}

        std::optional<std::string> get_persisted_uri() {
            return get_nullable_value<std::string>("persistedUri");
        }

        int get_resume_offset() {
            return get_value<int>("resumeOffset");
        }

        std::optional<Etag> get_resume_etag() {
            auto value = get_optional_array<jni::jbyte>("resumeEtag");
            if (value) {
                return std::make_optional<Etag>(*value);
            } else {
                return {};
            }
        }

        anjay_fw_update_initial_result_t get_result() {
            auto value = get_value<jni::Object<FirmwareUpdateInitialResult>>(
                    "result");
            if (value) {
                return FirmwareUpdateInitialResult::into_native(env_, value);
            } else {
                return ANJAY_FW_UPDATE_INITIAL_NEUTRAL;
            }
        }

    };
};

} // namespace utils
