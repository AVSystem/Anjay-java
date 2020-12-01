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
#include <anjay/server.h>

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/exception.hpp"
#include "./util_classes/input_stream.hpp"
#include "./util_classes/output_stream.hpp"

#include <optional>
#include <stdexcept>

class NativeServerObject {
    struct Instance {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjayServerObject$Instance";
        }

        class Accessor : public utils::AccessorBase<Instance> {
        public:
            explicit Accessor(jni::JNIEnv &env,
                              const jni::Object<Instance> &instance)
                    : AccessorBase(env, instance) {}

            anjay_ssid_t get_ssid() {
                return get_value<uint16_t>("ssid");
            }

            int32_t get_lifetime() {
                return get_value<int32_t>("lifetime");
            }

            std::optional<int32_t> get_default_min_period() {
                return get_optional_integer<int32_t>("defaultMinPeriod");
            }

            std::optional<int32_t> get_default_max_period() {
                return get_optional_integer<int32_t>("defaultMaxPeriod");
            }

            std::optional<int32_t> get_disable_timeout() {
                return get_optional_integer<int32_t>("disableTimeout");
            }

            std::optional<std::string> get_binding() {
                return get_nullable_value<std::string>("binding");
            }

            bool get_notification_storing() {
                return get_value<bool>("notificationStoring");
            }

        };
    };

    std::weak_ptr<anjay_t> anjay_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeServerObject";
    }

    static void register_native(jni::JNIEnv &env);

    NativeServerObject(jni::JNIEnv &env,
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
