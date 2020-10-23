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

#include "./accessor_base.hpp"
#include "./etag.hpp"

#include <anjay/download.h>

namespace utils {

struct DownloadConfiguration {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayDownload$Configuration";
    }

    class Accessor : public AccessorBase<DownloadConfiguration> {
    public:
        explicit Accessor(
                jni::JNIEnv &env,
                const jni::Object<utils::DownloadConfiguration> &config)
                : AccessorBase(env, config) {}

        std::optional<std::string> get_url() {
            return get_nullable_value<std::string>("url");
        }

        int get_start_offset() {
            return get_value<int>("startOffset");
        }

        std::optional<Etag> get_etag() {
            auto value = get_optional_array<jni::jbyte>("etag");
            if (value) {
                return std::make_optional<Etag>(*value);
            } else {
                return {};
            }
        }

        std::optional<avs_coap_udp_tx_params_t> get_coap_tx_params() {
            auto value = get_optional_value<CoapUdpTxParams>("coapTxParams");
            if (value) {
                return std::make_optional(
                        CoapUdpTxParams::into_native(get_env(), *value));
            }
            return {};
        }
    };
};

} // namespace utils
