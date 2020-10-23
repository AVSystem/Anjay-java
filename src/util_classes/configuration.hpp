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

#include <optional>
#include <stdexcept>
#include <string>

#include "./accessor_base.hpp"
#include "./coap_udp_tx_params.hpp"
#include "./dtls_handshake_timeouts.hpp"
#include "./dtls_version.hpp"
#include "./duration.hpp"

namespace utils {

struct Configuration {
    static constexpr auto Name() {
        return "com/avsystem/anjay/Anjay$Configuration";
    }

    class Accessor : public AccessorBase<Configuration> {
    public:
        explicit Accessor(jni::JNIEnv &env,
                          const jni::Object<utils::Configuration> &instance)
                : AccessorBase(env, instance) {}

        std::optional<std::string> get_endpoint_name() {
            return get_nullable_value<std::string>("endpointName");
        }

        uint16_t get_udp_listen_port() {
            return get_value<uint16_t>("udpListenPort");
        }

        size_t get_in_buffer_size() {
            return get_value<size_t>("inBufferSize");
        }

        size_t get_out_buffer_size() {
            return get_value<size_t>("outBufferSize");
        }

        size_t get_msg_cache_size() {
            return get_value<size_t>("msgCacheSize");
        }

        bool get_confirmable_notifications() {
            return get_value<bool>("confirmableNotifications");
        }

        bool get_disable_legacy_server_initiated_bootstrap() {
            return get_value<bool>("disableLegacyServerInitiatedBootstrap");
        }

        size_t get_stored_notification_limit() {
            return get_value<size_t>("storedNotificationLimit");
        }

        bool get_prefer_hierarchical_formats() {
            return get_value<bool>("preferHierarchicalFormats");
        }

        bool get_use_connection_id() {
            return get_value<bool>("useConnectionId");
        }

        std::optional<avs_coap_udp_tx_params_t> get_udp_tx_params() {
            auto value = get_optional_value<CoapUdpTxParams>("udpTxParams");
            if (value) {
                return std::make_optional(
                        CoapUdpTxParams::into_native(get_env(), *value));
            }
            return {};
        }

        std::optional<avs_net_dtls_handshake_timeouts_t>
        get_udp_dtls_hs_tx_params() {
            auto value = get_optional_value<DtlsHandshakeTimeouts>(
                    "udpDtlsHsTxParams");
            if (value) {
                return std::make_optional(
                        DtlsHandshakeTimeouts::into_native(get_env(), *value));
            }
            return {};
        }

        std::vector<uint32_t> get_default_tls_ciphersuites() {
            auto value =
                    get_optional_array<jni::jint>("defaultTlsCiphersuites");
            if (value) {
                std::vector<uint32_t> result(value->size());
                for (size_t i = 0; i < value->size(); i++) {
                    if ((*value)[i] < 0) {
                        avs_throw(std::runtime_error(
                                "ciphersuite ID must not be negative"));
                    }
                    result[i] = static_cast<uint32_t>((*value)[i]);
                }
                return result;
            }
            return {};
        }

        std::optional<avs_net_ssl_version_t> get_dtls_version() {
            auto value = get_optional_value<DtlsVersion>("dtlsVersion");
            if (value) {
                return std::make_optional(
                        DtlsVersion::into_native(get_env(), *value));
            }
            return {};
        }
    };
};

} // namespace utils
