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

#include <anjay/core.h>

#include "./accessor_base.hpp"
#include "./security_info_cert.hpp"
#include "./security_info_psk.hpp"

#include <optional>
#include <variant>

namespace utils {

class SecurityConfig {
    std::weak_ptr<anjay_t> anjay_;
    jni::JNIEnv &env_;
    jni::Global<jni::Object<SecurityConfig>> self_;
    std::optional<std::variant<SecurityInfoPsk, SecurityInfoCert>> psk_or_cert_;
    anjay_security_config_t config_;
    bool config_from_dm_;

    struct SecurityInfo {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjaySecurityInfo";
        }
    };

    struct SecurityConfigFromUser {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjaySecurityConfig";
        }
    };

    struct SecurityConfigFromDm {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjaySecurityConfigFromDm";
        }
    };

    std::optional<SecurityInfoPsk> get_psk_security(
            const jni::Local<jni::Object<SecurityConfigFromUser>> &config) {
        auto accessor = AccessorBase<SecurityConfigFromUser>{ env_, config };
        auto info =
                accessor.get_value<jni::Object<SecurityInfo>>("securityInfo");
        auto clazz = jni::Class<SecurityInfoPsk>::Find(env_);
        if (!jni::IsInstanceOf(env_, info.get(), *clazz)) {
            return {};
        }
        return { SecurityInfoPsk{ env_, jni::Cast(env_, clazz, info) } };
    }

    std::optional<SecurityInfoCert> get_cert_security(
            const jni::Local<jni::Object<SecurityConfigFromUser>> &config) {
        auto accessor = AccessorBase<SecurityConfigFromUser>{ env_, config };
        auto info =
                accessor.get_value<jni::Object<SecurityInfo>>("securityInfo");
        auto clazz = jni::Class<SecurityInfoCert>::Find(env_);
        if (!jni::IsInstanceOf(env_, info.get(), *clazz)) {
            return {};
        }
        return { SecurityInfoCert{ env_, jni::Cast(env_, clazz, info) } };
    }

    std::variant<SecurityInfoPsk, SecurityInfoCert> get_security(
            const jni::Local<jni::Object<SecurityConfigFromUser>> &config) {
        if (auto psk = get_psk_security(config)) {
            return { std::move(*psk) };
        } else if (auto cert = get_cert_security(config)) {
            return { std::move(*cert) };
        } else {
            avs_throw(std::runtime_error("unsupported security info type"));
        }
    }

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayAbstractSecurityConfig";
    }

    SecurityConfig(std::weak_ptr<anjay_t> anjay,
                   jni::JNIEnv &env,
                   const jni::Local<jni::Object<SecurityConfig>> &instance)
            : anjay_(anjay),
              env_(env),
              self_(jni::NewGlobal(env, instance)),
              psk_or_cert_(),
              config_(),
              config_from_dm_(false) {
        if (jni::IsInstanceOf(env, instance.get(),
                              *jni::Class<SecurityConfigFromUser>::Find(env))) {
            psk_or_cert_.emplace(get_security(jni::Cast<SecurityConfigFromUser>(
                    env, jni::Class<SecurityConfigFromUser>::Find(env),
                    self_)));

            std::visit(
                    [&](auto &&security) {
                        using T = std::decay_t<decltype(security)>;
                        if constexpr (std::is_same<T, SecurityInfoPsk>::value) {
                            config_.security_info =
                                    avs_net_security_info_from_psk(
                                            security.get_info());
                        } else {
                            config_.security_info =
                                    avs_net_security_info_from_certificates(
                                            security.get_info());
                        }
                    },
                    *psk_or_cert_);
        } else {
            config_from_dm_ = true;
        }
    }

    anjay_security_config_t get_config() {
        if (config_from_dm_) {
            auto as_config_from_dm = jni::Cast<SecurityConfigFromDm>(
                    env_, jni::Class<SecurityConfigFromDm>::Find(env_), self_);
            auto accessor =
                    AccessorBase<SecurityConfigFromDm>{ env_,
                                                        as_config_from_dm };
            auto uri = *accessor.get_nullable_value<std::string>("uri");
            anjay_security_config_t from_dm;
            if (auto locked = anjay_.lock()) {
                if (anjay_security_config_from_dm(locked.get(), &from_dm,
                                                  uri.c_str())) {
                    struct ConcurrentModificationException {
                        static constexpr auto Name() {
                            return "java/util/ConcurrentModificationException";
                        }
                    };
                    jni::ThrowNew(
                            env_,
                            *jni::Class<ConcurrentModificationException>::Find(
                                    env_),
                            "Security configuration got invalidated since it "
                            "was returned from Anjay.securityConfigFromDm().");
                }
                return from_dm;
            } else {
                avs_throw(std::runtime_error("anjay object expired"));
            }
        } else {
            return config_;
        }
    }
};

} // namespace utils
