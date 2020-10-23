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

struct CoapUdpTxParams {
    static constexpr auto Name() {
        return "com/avsystem/anjay/Anjay$CoapUdpTxParams";
    }

    static avs_coap_udp_tx_params_t
    into_native(jni::JNIEnv &env,
                const jni::Object<CoapUdpTxParams> &instance) {
        auto accessor = AccessorBase<CoapUdpTxParams>{ env, instance };
        avs_coap_udp_tx_params_t result{};
        result.ack_timeout = Duration::into_native(
                env, accessor.get_value<jni::Object<Duration>>("ackTimeout"));
        result.ack_random_factor =
                accessor.get_value<double>("ackRandomFactor");
        result.max_retransmit = accessor.get_value<int>("maxRetransmit");
        result.nstart = accessor.get_value<int>("nstart");
        return result;
    }
};

} // namespace utils
