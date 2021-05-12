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

#include <anjay/anjay.h>

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"
#include "./exception.hpp"

#include <string>
#include <unordered_map>

namespace utils {

struct ResourceKind {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayObject$ResourceKind";
    }

    static anjay_dm_resource_kind_t
    into_native(jni::JNIEnv &env, const jni::Object<ResourceKind> &instance) {
        static std::unordered_map<std::string, anjay_dm_resource_kind_t>
                MAPPING{
                    { "R", ANJAY_DM_RES_R },   { "W", ANJAY_DM_RES_W },
                    { "RW", ANJAY_DM_RES_RW }, { "RM", ANJAY_DM_RES_RM },
                    { "WM", ANJAY_DM_RES_WM }, { "RWM", ANJAY_DM_RES_RWM },
                    { "E", ANJAY_DM_RES_E },   { "BS_RW", ANJAY_DM_RES_BS_RW }
                };
        auto clazz = jni::Class<ResourceKind>::Find(env);
        auto value = jni::Make<std::string>(
                env, instance.Call(
                             env, clazz.GetMethod<jni::String()>(env, "name")));
        auto mapped_to = MAPPING.find(value);
        if (mapped_to == MAPPING.end()) {
            avs_throw(IllegalArgumentException(env, "Unsupported enum value: "
                                                            + value));
        }
        return mapped_to->second;
    }
};

} // namespace utils
