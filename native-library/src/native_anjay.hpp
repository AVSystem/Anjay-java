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

#include "./jni_wrapper.hpp"

#include <memory>
#include <string>
#include <vector>

#include <anjay/anjay.h>

#include "./native_anjay_object_adapter.hpp"

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/configuration.hpp"
#include "./util_classes/duration.hpp"
#include "./util_classes/native_anjay_object.hpp"
#include "./util_classes/native_socket_entry.hpp"
#include "./util_classes/native_transport_set.hpp"
#include "./util_classes/transport.hpp"

class NativeAnjay {
    std::string endpoint_name_;
    avs_coap_udp_tx_params_t udp_tx_params_;
    std::vector<std::unique_ptr<NativeAnjayObjectAdapter>> objects_;
    std::shared_ptr<anjay_t> anjay_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeAnjay";
    }

    static NativeAnjay *
    into_native(jni::JNIEnv &env,
                const jni::Object<NativeAnjay> &native_anjay) {
        auto accessor = utils::AccessorBase<NativeAnjay>{ env, native_anjay };
        return reinterpret_cast<NativeAnjay *>(
                accessor.get_value<jni::jlong>("self"));
    }

    static void register_native(jni::JNIEnv &env);

    NativeAnjay(jni::JNIEnv &env, jni::Object<utils::Configuration> &config);

    std::shared_ptr<anjay_t> get_anjay() {
        return anjay_;
    }

    jni::Local<jni::Array<jni::Object<utils::NativeSocketEntry>>>
    get_socket_entries(jni::JNIEnv &env);

    void serve(jni::JNIEnv &, jni::jlong socket_ptr);

    void sched_run(jni::JNIEnv &);

    jni::Local<jni::Object<utils::Duration>>
    get_sched_time_to_next(jni::JNIEnv &env);

    jni::jint schedule_registration_update(jni::JNIEnv &env, jni::jint ssid);

    jni::jint schedule_transport_reconnect(
            jni::JNIEnv &env,
            jni::Object<utils::NativeTransportSet> &transport_set);

    jni::jboolean
    transport_is_offline(jni::JNIEnv &env,
                         jni::Object<utils::NativeTransportSet> &transport_set);

    jni::jint transport_enter_offline(
            jni::JNIEnv &env,
            jni::Object<utils::NativeTransportSet> &transport_set);

    jni::jint transport_exit_offline(
            jni::JNIEnv &env,
            jni::Object<utils::NativeTransportSet> &transport_set);

    jni::jint
    notify_changed(jni::JNIEnv &, jni::jint oid, jni::jint iid, jni::jint rid);

    jni::jint notify_instances_changed(jni::JNIEnv &, jni::jint oid);

    jni::jint disable_server(jni::JNIEnv &env, jni::jint ssid);

    jni::jint
    disable_server_with_timeout(jni::JNIEnv &env,
                                jni::jint ssid,
                                jni::Object<utils::Optional> &duration);

    jni::jint enable_server(jni::JNIEnv &env, jni::jint ssid);

    jni::jint register_object(jni::JNIEnv &env,
                              jni::Object<utils::NativeAnjayObject> &object);

    avs_coap_udp_tx_params_t get_udp_tx_params();

    jni::jboolean has_security_config_for_uri(jni::JNIEnv &env,
                                              jni::String &uri);

    static jni::Local<jni::String> get_version(jni::JNIEnv &env,
                                               jni::Class<NativeAnjay> &) {
        return jni::Make<jni::String>(env, anjay_get_version());
    }

    static jni::jint get_ssid_any(jni::JNIEnv &, jni::Class<NativeAnjay> &) {
        return ANJAY_SSID_ANY;
    }

    static jni::jint get_ssid_bootstrap(jni::JNIEnv &,
                                        jni::Class<NativeAnjay> &) {
        return ANJAY_SSID_BOOTSTRAP;
    }

    static jni::jint get_id_invalid(jni::JNIEnv &, jni::Class<NativeAnjay> &) {
        return ANJAY_ID_INVALID;
    }

    static jni::jint get_error_bad_request(jni::JNIEnv &,
                                           jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_BAD_REQUEST;
    }

    static jni::jint get_error_unauthorized(jni::JNIEnv &,
                                            jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_UNAUTHORIZED;
    }

    static jni::jint get_error_bad_option(jni::JNIEnv &,
                                          jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_BAD_OPTION;
    }

    static jni::jint get_error_not_found(jni::JNIEnv &,
                                         jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_NOT_FOUND;
    }

    static jni::jint get_error_method_not_allowed(jni::JNIEnv &,
                                                  jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_METHOD_NOT_ALLOWED;
    }

    static jni::jint get_error_not_acceptable(jni::JNIEnv &,
                                              jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_NOT_ACCEPTABLE;
    }

    static jni::jint
    get_error_request_entity_incomplete(jni::JNIEnv &,
                                        jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_REQUEST_ENTITY_INCOMPLETE;
    }

    static jni::jint get_error_internal(jni::JNIEnv &,
                                        jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_INTERNAL;
    }

    static jni::jint get_error_not_implemnted(jni::JNIEnv &,
                                              jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_NOT_IMPLEMENTED;
    }

    static jni::jint get_error_service_unavailable(jni::JNIEnv &,
                                                   jni::Class<NativeAnjay> &) {
        return ANJAY_ERR_SERVICE_UNAVAILABLE;
    }
};
