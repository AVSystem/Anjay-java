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

#include "../jni_wrapper.hpp"

#include <avsystem/commons/avs_time.h>

#include "./accessor_base.hpp"
#include "./exception.hpp"

namespace utils {

struct Duration {
    static constexpr auto Name() {
        return "java/time/Duration";
    }

    static avs_time_duration_t
    into_native(jni::JNIEnv &env, const jni::Object<Duration> &instance) {
        auto accessor = AccessorBase<Duration>{ env, instance };
        avs_time_duration_t result{};
        result.seconds = accessor.get_method<jni::jlong()>("getSeconds")();
        result.nanoseconds = accessor.get_method<jni::jint()>("getNano")();
        return result;
    }

    static jni::Local<jni::Object<Duration>>
    into_java(jni::JNIEnv &env, avs_time_duration_t duration) {
        if (!avs_time_duration_valid(duration)) {
            avs_throw(std::runtime_error("duration is invalid"));
        }
        return AccessorBase<Duration>::get_static_method<jni::Object<Duration>(
                jni::jlong, jni::jlong)>(env, "ofSeconds")(
                duration.seconds, duration.nanoseconds);
    }
};

} // namespace utils
