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

#include <anjay/anjay.h>

#include <avsystem/commons/avs_log.h>

#include "./native_anjay.hpp"

#include "./util_classes/exception.hpp"

using namespace std;

NativeAnjay::NativeAnjay(jni::JNIEnv &env,
                         jni::Object<utils::Configuration> &config)
        : endpoint_name_(),
          udp_tx_params_(ANJAY_COAP_DEFAULT_UDP_TX_PARAMS),
          objects_(),
          anjay_() {
    auto config_accessor = utils::Configuration::Accessor{ env, config };
    auto endpoint_name = config_accessor.get_endpoint_name();
    if (!endpoint_name) {
        avs_throw(IllegalArgumentException(env, "endpoint name MUST be set"));
    }
    endpoint_name_ = *endpoint_name;
    anjay_configuration_t configuration{};
    configuration.endpoint_name = endpoint_name_.c_str();
    configuration.in_buffer_size = config_accessor.get_in_buffer_size();
    configuration.out_buffer_size = config_accessor.get_out_buffer_size();
    configuration.msg_cache_size = config_accessor.get_msg_cache_size();
    configuration.udp_listen_port = config_accessor.get_udp_listen_port();
    configuration.confirmable_notifications =
            config_accessor.get_confirmable_notifications();
    configuration.disable_legacy_server_initiated_bootstrap =
            config_accessor.get_disable_legacy_server_initiated_bootstrap();
    configuration.stored_notification_limit =
            config_accessor.get_stored_notification_limit();
    configuration.prefer_hierarchical_formats =
            config_accessor.get_prefer_hierarchical_formats();
    configuration.use_connection_id = config_accessor.get_use_connection_id();
    auto udp_tx_params = config_accessor.get_udp_tx_params();
    if (udp_tx_params) {
        udp_tx_params_ = *udp_tx_params;
    }
    configuration.udp_tx_params = &udp_tx_params_;

    auto dtls_hs_params = config_accessor.get_udp_dtls_hs_tx_params();
    if (dtls_hs_params) {
        configuration.udp_dtls_hs_tx_params = &*dtls_hs_params;
    }

    auto default_tls_ciphersuites =
            config_accessor.get_default_tls_ciphersuites();
    configuration.default_tls_ciphersuites = {
        default_tls_ciphersuites.data(), default_tls_ciphersuites.size()
    };

    auto dtls_version = config_accessor.get_dtls_version();
    if (dtls_version) {
        configuration.dtls_version = *dtls_version;
    }

    if (!(anjay_ = decltype(anjay_)(anjay_new(&configuration), anjay_delete))) {
        avs_throw(AnjayException(env, -1, "could not instantiate anjay"));
    }
}

jni::Local<jni::Array<jni::Object<utils::NativeSocketEntry>>>
NativeAnjay::get_socket_entries(jni::JNIEnv &env) {
    AVS_LIST(const anjay_socket_entry_t) entries =
            anjay_get_socket_entries(anjay_.get());
    const size_t size = AVS_LIST_SIZE(entries);

    auto result =
            jni::Array<jni::Object<utils::NativeSocketEntry>>::New(env, size);
    int index = 0;
    AVS_LIST(const anjay_socket_entry_t) it;
    AVS_LIST_FOREACH(it, entries) {
        result.Set(env, index++, utils::NativeSocketEntry::New(env, it));
    }
    return result;
}

void NativeAnjay::serve(jni::JNIEnv &, jni::jlong socket_ptr) {
    anjay_serve(anjay_.get(), reinterpret_cast<avs_net_socket_t *>(socket_ptr));
}

void NativeAnjay::sched_run(jni::JNIEnv &) {
    anjay_sched_run(anjay_.get());
}

jni::Local<jni::Object<utils::Duration>>
NativeAnjay::get_sched_time_to_next(jni::JNIEnv &env) {
    avs_time_duration_t duration = AVS_TIME_DURATION_INVALID;
    (void) anjay_sched_time_to_next(anjay_.get(), &duration);
    if (!avs_time_duration_valid(duration)) {
        return jni::Local<jni::Object<utils::Duration>>(env, nullptr);
    }
    return utils::Duration::into_java(env, duration);
}

jni::jint NativeAnjay::schedule_registration_update(jni::JNIEnv &,
                                                    jni::jint ssid) {
    return anjay_schedule_registration_update(anjay_.get(), ssid);
}

jni::jint NativeAnjay::schedule_transport_reconnect(
        jni::JNIEnv &env,
        jni::Object<utils::NativeTransportSet> &transport_set) {
    return anjay_transport_schedule_reconnect(
            anjay_.get(),
            utils::NativeTransportSet::into_transport_set(env, transport_set));
}

jni::jboolean NativeAnjay::transport_is_offline(
        jni::JNIEnv &env,
        jni::Object<utils::NativeTransportSet> &transport_set) {
    return anjay_transport_is_offline(
            anjay_.get(),
            utils::NativeTransportSet::into_transport_set(env, transport_set));
}

jni::jint NativeAnjay::transport_enter_offline(
        jni::JNIEnv &env,
        jni::Object<utils::NativeTransportSet> &transport_set) {
    return anjay_transport_enter_offline(
            anjay_.get(),
            utils::NativeTransportSet::into_transport_set(env, transport_set));
}

jni::jint NativeAnjay::transport_exit_offline(
        jni::JNIEnv &env,
        jni::Object<utils::NativeTransportSet> &transport_set) {
    return anjay_transport_exit_offline(
            anjay_.get(),
            utils::NativeTransportSet::into_transport_set(env, transport_set));
}

jni::jint NativeAnjay::notify_changed(jni::JNIEnv &,
                                      jni::jint oid,
                                      jni::jint iid,
                                      jni::jint rid) {
    return anjay_notify_changed(anjay_.get(), oid, iid, rid);
}

jni::jint NativeAnjay::notify_instances_changed(jni::JNIEnv &, jni::jint oid) {
    return anjay_notify_instances_changed(anjay_.get(), oid);
}

jni::jint NativeAnjay::disable_server(jni::JNIEnv &, jni::jint ssid) {
    return anjay_disable_server(anjay_.get(), ssid);
}

jni::jint NativeAnjay::disable_server_with_timeout(
        jni::JNIEnv &env,
        jni::jint ssid,
        jni::Object<utils::Optional> &duration) {
    auto maybe_duration = utils::Optional{ env, jni::NewLocal(env, duration) };
    avs_time_duration_t disable_duration = AVS_TIME_DURATION_INVALID;
    if (maybe_duration.is_present()) {
        disable_duration = utils::Duration::into_native(
                env, maybe_duration.get<utils::Duration>());
    }
    return anjay_disable_server_with_timeout(anjay_.get(), ssid,
                                             disable_duration);
}

jni::jint NativeAnjay::enable_server(jni::JNIEnv &, jni::jint ssid) {
    return anjay_enable_server(anjay_.get(), ssid);
}

avs_coap_udp_tx_params_t NativeAnjay::get_udp_tx_params() {
    return udp_tx_params_;
}

jni::jboolean NativeAnjay::has_security_config_for_uri(jni::JNIEnv &env,
                                                       jni::String &uri) {
    if (!uri) {
        return false;
    }
    anjay_security_config_t dummy;
    auto uri_as_string = jni::Make<std::string>(env, uri);
    if (anjay_security_config_from_dm(anjay_.get(), &dummy,
                                      uri_as_string.c_str())) {
        return false;
    }
    return true;
}

jni::jint
NativeAnjay::register_object(jni::JNIEnv &env,
                             jni::Object<utils::NativeAnjayObject> &object) {
    auto adapter = make_unique<NativeAnjayObjectAdapter>(anjay_, env, object);
    int result = adapter->install();
    if (!result) {
        objects_.push_back(move(adapter));
    }
    return result;
}

void NativeAnjay::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeAnjay>(
            env, jni::Class<NativeAnjay>::Find(env), "self",
            jni::MakePeer<NativeAnjay, jni::Object<utils::Configuration> &>,
            "init",
            "cleanup",
            METHOD(&NativeAnjay::get_socket_entries, "anjayGetSocketEntries"),
            METHOD(&NativeAnjay::serve, "anjayServe"),
            METHOD(&NativeAnjay::sched_run, "anjaySchedRun"),
            METHOD(&NativeAnjay::get_sched_time_to_next, "anjaySchedTimeToNext"),
            METHOD(&NativeAnjay::schedule_registration_update, "anjayScheduleRegistrationUpdate"),
            METHOD(&NativeAnjay::schedule_transport_reconnect, "anjayScheduleTransportReconnect"),
            METHOD(&NativeAnjay::enable_server, "anjayEnableServer"),
            METHOD(&NativeAnjay::disable_server, "anjayDisableServer"),
            METHOD(&NativeAnjay::disable_server_with_timeout, "anjayDisableServerWithTimeout"),
            METHOD(&NativeAnjay::transport_is_offline, "anjayTransportIsOffline"),
            METHOD(&NativeAnjay::transport_enter_offline, "anjayTransportEnterOffline"),
            METHOD(&NativeAnjay::transport_exit_offline, "anjayTransportExitOffline"),
            METHOD(&NativeAnjay::notify_changed, "anjayNotifyChanged"),
            METHOD(&NativeAnjay::notify_instances_changed, "anjayNotifyInstancesChanged"),
            METHOD(&NativeAnjay::register_object, "anjayRegisterObject"),
            METHOD(&NativeAnjay::has_security_config_for_uri, "anjayHasSecurityConfigForUri")
    );

#define STATIC_METHOD(MethodPtr, Name) \
    jni::MakeNativeMethod<decltype(MethodPtr), (MethodPtr)>(Name)

    jni::RegisterNatives(
            env, *jni::Class<NativeAnjay>::Find(env),
            STATIC_METHOD(&NativeAnjay::get_version, "getVersion"),
            STATIC_METHOD(&NativeAnjay::get_ssid_any, "getSsidAny"),
            STATIC_METHOD(&NativeAnjay::get_id_invalid, "getIdInvalid"),
            STATIC_METHOD(&NativeAnjay::get_ssid_bootstrap, "getSsidBootstrap"),
            STATIC_METHOD(&NativeAnjay::get_error_bad_request, "getErrorBadRequest"),
            STATIC_METHOD(&NativeAnjay::get_error_unauthorized, "getErrorUnauthorized"),
            STATIC_METHOD(&NativeAnjay::get_error_bad_option, "getErrorBadOption"),
            STATIC_METHOD(&NativeAnjay::get_error_not_found, "getErrorNotFound"),
            STATIC_METHOD(&NativeAnjay::get_error_method_not_allowed, "getErrorMethodNotAllowed"),
            STATIC_METHOD(&NativeAnjay::get_error_not_acceptable, "getErrorNotAcceptable"),
            STATIC_METHOD(&NativeAnjay::get_error_request_entity_incomplete,"getErrorRequestEntityIncomplete"),
            STATIC_METHOD(&NativeAnjay::get_error_internal, "getErrorInternal"),
            STATIC_METHOD(&NativeAnjay::get_error_not_implemnted, "getErrorNotImplemented"),
            STATIC_METHOD(&NativeAnjay::get_error_service_unavailable, "getErrorServiceUnavailable"));
    // clang-format on
#undef METHOD
#undef STATIC_METHOD
}
