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

#include <avsystem/commons/avs_log.h>

#include "../jni_wrapper.hpp"

#include <unordered_map>

#include "./accessor_base.hpp"

namespace utils {

struct Level {
    static constexpr auto Name() {
        return "java/util/logging/Level";
    }

    static jni::Local<jni::Object<Level>> from_native(jni::JNIEnv &env,
                                                      avs_log_level_t level) {
        auto clazz = jni::Class<Level>::Find(env);
        auto get_enum_instance = [&](const std::string &name) {
            return clazz.Get(env,
                             clazz.GetStaticField<jni::Object<Level>>(
                                     env, name.c_str()));
        };
        static std::unordered_map<avs_log_level_t, std::string> MAPPING{
            { AVS_LOG_TRACE, "FINEST" }, { AVS_LOG_DEBUG, "FINE" },
            { AVS_LOG_INFO, "INFO" },    { AVS_LOG_WARNING, "WARNING" },
            { AVS_LOG_ERROR, "SEVERE" }, { AVS_LOG_QUIET, "OFF" }
        };
        return get_enum_instance(MAPPING[level]);
    }
};

} // namespace utils
