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

#include <avsystem/coap/udp.h>

#include "./accessor_base.hpp"
#include "./duration.hpp"

namespace utils {

struct DtlsHandshakeTimeouts {
    static constexpr auto Name() {
        return "com/avsystem/anjay/Anjay$DtlsHandshakeTimeouts";
    }

    static avs_net_dtls_handshake_timeouts_t
    into_native(jni::JNIEnv &env,
                const jni::Object<DtlsHandshakeTimeouts> &instance) {
        auto accessor = AccessorBase<DtlsHandshakeTimeouts>{ env, instance };
        avs_net_dtls_handshake_timeouts_t result{};
        result.min = Duration::into_native(
                env, accessor.get_value<jni::Object<Duration>>("min"));
        result.max = Duration::into_native(
                env, accessor.get_value<jni::Object<Duration>>("max"));
        return result;
    }
};

} // namespace utils
