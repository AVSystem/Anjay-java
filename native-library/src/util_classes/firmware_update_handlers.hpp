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

#include "../jni_wrapper.hpp"

#include "./byte_buffer.hpp"
#include "./coap_udp_tx_params.hpp"
#include "./security_config.hpp"

namespace utils {

struct FirmwareUpdateHandlers {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeFirmwareUpdateHandlers";
    }

    class Accessor : public AccessorBase<FirmwareUpdateHandlers> {
    public:
        explicit Accessor(jni::JNIEnv &env,
                          const jni::Object<FirmwareUpdateHandlers> &handlers)
                : AccessorBase(env, handlers) {}

        int stream_open(const char *package_uri,
                        const anjay_etag_t *package_etag) {
            auto make_etag = [&](const anjay_etag_t *etag) {
                if (etag) {
                    std::vector<jni::jbyte> etag_vec(etag->value,
                                                     etag->value + etag->size);
                    return utils::Optional::of(
                            env_,
                            jni::Make<jni::Array<jni::jbyte>>(env_, etag_vec));
                } else {
                    return utils::Optional::empty(env_);
                }
            };

            auto make_uri = [&](const char *uri) {
                if (uri) {
                    return utils::Optional::of(
                            env_, jni::Make<jni::String>(env_, package_uri));
                } else {
                    return utils::Optional::empty(env_);
                }
            };

            return get_method<jni::jint(jni::Object<utils::Optional>,
                                        jni::Object<utils::Optional>)>(
                    "streamOpen")(make_uri(package_uri).into_java(),
                                  make_etag(package_etag).into_java());
        }

        int stream_write(const void *data, size_t length) {
            std::vector<jni::jbyte> data_vec((const uint8_t *) data,
                                             (const uint8_t *) data + length);
            return get_method<jni::jint(jni::Array<jni::jbyte>)>("streamWrite")(
                    jni::Make<jni::Array<jni::jbyte>>(env_, data_vec));
        }

        int stream_finish() {
            return get_method<jni::jint()>("streamFinish")();
        }

        void reset() {
            get_method<void()>("reset")();
        }

        std::optional<std::string> get_name() {
            auto result = get_method<jni::String()>("getName")();
            if (result) {
                return std::make_optional<std::string>(
                        jni::Make<std::string>(env_, result));
            } else {
                return {};
            }
        }

        std::optional<std::string> get_version() {
            auto result = get_method<jni::String()>("getVersion")();
            if (result) {
                return std::make_optional<std::string>(
                        jni::Make<std::string>(env_, result));
            } else {
                return {};
            }
        }

        int perform_upgrade() {
            return get_method<jni::jint()>("performUpgrade")();
        }

        std::optional<avs_coap_udp_tx_params_t>
        get_coap_tx_params(const char *download_uri) {
            auto tx_params = get_method<jni::Object<utils::CoapUdpTxParams>(
                    jni::String)>("getCoapTxParams")(
                    jni::Make<jni::String>(env_, download_uri));
            if (tx_params) {
                return { utils::CoapUdpTxParams::into_native(env_, tx_params) };
            } else {
                return {};
            }
        }

        std::unique_ptr<SecurityConfig>
        get_security_config(std::weak_ptr<anjay_t> anjay,
                            const char *download_uri) {
            auto security_config =
                    get_method<jni::Object<utils::SecurityConfig>(jni::String)>(
                            "getSecurityConfig")(
                            jni::Make<jni::String>(env_, download_uri));
            if (!security_config) {
                return {};
            } else {
                return std::make_unique<utils::SecurityConfig>(anjay, env_,
                                                               security_config);
            }
        }
    };
};

} // namespace utils
