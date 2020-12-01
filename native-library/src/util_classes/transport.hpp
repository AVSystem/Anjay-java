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

#include <anjay/core.h>

#include "../jni_wrapper.hpp"

#include "./exception.hpp"

namespace utils {

struct Transport {
    static constexpr auto Name() {
        return "com/avsystem/anjay/Anjay$Transport";
    }

    static jni::Local<jni::Object<Transport>>
    New(jni::JNIEnv &env, anjay_socket_transport_t transport) {
        auto native_transport_class = jni::Class<Transport>::Find(env);
        auto get_enum_instance = [&](const char *name) {
            return native_transport_class.Get(
                    env,
                    native_transport_class
                            .GetStaticField<jni::Object<Transport>>(env, name));
        };
        switch (transport) {
        case ANJAY_SOCKET_TRANSPORT_UDP:
            return get_enum_instance("UDP");
        case ANJAY_SOCKET_TRANSPORT_TCP:
            return get_enum_instance("TCP");
        default:
            avs_throw(std::runtime_error("Unsupported transport"));
        }
    }
};

} // namespace utils
