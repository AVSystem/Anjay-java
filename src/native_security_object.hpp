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

#include <anjay/anjay.h>
#include <anjay/security.h>

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/exception.hpp"
#include "./util_classes/input_stream.hpp"
#include "./util_classes/output_stream.hpp"

#include <optional>
#include <stdexcept>

class NativeSecurityObject {
    struct Instance {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjaySecurityObject$Instance";
        }

        class Accessor : public utils::AccessorBase<Instance> {
            auto to_optional_std_string(
                    const std::optional<jni::Local<jni::String>> &value) {
                if (!value) {
                    return std::optional<std::string>{};
                }
                return std::make_optional(
                        jni::Make<std::string>(get_env(), *value));
            }

        public:
            explicit Accessor(jni::JNIEnv &env,
                              const jni::Object<Instance> &instance)
                    : AccessorBase(env, instance) {}

            anjay_ssid_t get_ssid() {
                return get_value<uint16_t>("ssid");
            }

            std::optional<std::string> get_server_uri() {
                return to_optional_std_string(
                        get_optional_value<jni::StringTag>("serverUri"));
            }

            bool get_bootstrap_server() {
                return get_value<bool>("bootstrapServer");
            }

            anjay_security_mode_t get_security_mode() {
                static std::unordered_map<std::string, anjay_security_mode_t>
                        MAPPING{
                            { "PSK", ANJAY_SECURITY_PSK },
                            { "RPK", ANJAY_SECURITY_RPK },
                            { "CERTIFICATE", ANJAY_SECURITY_CERTIFICATE },
                            { "NOSEC", ANJAY_SECURITY_NOSEC },
                            { "EST", ANJAY_SECURITY_EST },
                        };
                struct SecurityMode {
                    static constexpr auto Name() {
                        return "com/avsystem/anjay/"
                               "AnjaySecurityObject$SecurityMode";
                    }
                };
                return get_enum_value<SecurityMode, anjay_security_mode_t>(
                        "securityMode", MAPPING);
            }

            std::optional<int32_t> get_client_holdoff_s() {
                return get_optional_integer<int32_t>("clientHoldoffS");
            }

            std::optional<int32_t> get_bootstrap_timeout_s() {
                return get_optional_integer<int32_t>("bootstrapTimeoutS");
            }

            std::optional<std::vector<jni::jbyte>>
            get_public_cert_or_psk_identity() {
                return get_optional_array<jni::jbyte>(
                        "publicCertOrPskIdentity");
            }

            std::optional<std::vector<jni::jbyte>>
            get_private_cert_or_psk_key() {
                return get_optional_array<jni::jbyte>("privateCertOrPskKey");
            }

            std::optional<std::vector<jni::jbyte>> get_server_public_key() {
                return get_optional_array<jni::jbyte>("serverPublicKey");
            }

            anjay_sms_security_mode_t get_sms_security_mode() {
                static std::unordered_map<std::string,
                                          anjay_sms_security_mode_t>
                        MAPPING{
                            { "DTLS_PSK", ANJAY_SMS_SECURITY_DTLS_PSK },
                            { "SECURE_PACKET",
                              ANJAY_SMS_SECURITY_SECURE_PACKET },
                            { "NOSEC", ANJAY_SMS_SECURITY_NOSEC },
                        };
                struct SmsSecurityMode {
                    static constexpr auto Name() {
                        return "com/avsystem/anjay/"
                               "AnjaySecurityObject$SmsSecurityMode";
                    }
                };
                return get_enum_value<SmsSecurityMode,
                                      anjay_sms_security_mode_t>(
                        "smsSecurityMode", MAPPING);
            }

            std::optional<std::vector<jni::jbyte>> get_sms_key_parameters() {
                return get_optional_array<jni::jbyte>("smsKeyParameters");
            }

            std::optional<std::vector<jni::jbyte>> get_sms_secret_key() {
                return get_optional_array<jni::jbyte>("smsSecretKey");
            }

            std::optional<std::string> get_server_sms_number() {
                return to_optional_std_string(
                        get_optional_value<jni::StringTag>("serverSmsNumber"));
            }
        };
    };

    std::weak_ptr<anjay_t> anjay_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeSecurityObject";
    }

    static void register_native(jni::JNIEnv &env);

    NativeSecurityObject(jni::JNIEnv &env,
                         const jni::Object<NativeAnjay> &instance);

    void purge(jni::JNIEnv &);

    jni::jboolean is_modified(jni::JNIEnv &);

    jni::jint persist(jni::JNIEnv &,
                      jni::Object<utils::OutputStream> &output_stream);

    jni::jint restore(jni::JNIEnv &,
                      jni::Object<utils::InputStream> &input_stream);

    jni::jint add_instance(jni::JNIEnv &env,
                           jni::Object<Instance> &instance,
                           jni::jint preferred_iid);
};
