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

#include "./accessor_base.hpp"

namespace utils {

struct NativeTransportSet {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeTransportSet";
    }

    class Accessor : public AccessorBase<NativeTransportSet> {
    public:
        explicit Accessor(jni::JNIEnv &env,
                          const jni::Object<NativeTransportSet> &instance)
                : AccessorBase(env, instance) {}

        bool get_udp() {
            return get_value<bool>("udp");
        }

        bool get_tcp() {
            return get_value<bool>("tcp");
        }

    };

    static anjay_transport_set_t
    into_transport_set(jni::JNIEnv &env,
                       jni::Object<NativeTransportSet> &instance) {
        anjay_transport_set_t transports{};
        auto accessor = Accessor{ env, instance };
        transports.udp = accessor.get_udp();
        transports.tcp = accessor.get_tcp();
        return transports;
    }
};

} // namespace utils
