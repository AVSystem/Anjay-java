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

#include "./native_security_object.hpp"

#include "./util_classes/exception.hpp"

NativeSecurityObject::NativeSecurityObject(
        jni::JNIEnv &env, const jni::Object<NativeAnjay> &instance)
        : anjay_() {
    auto native_anjay = NativeAnjay::into_native(env, instance);
    anjay_ = native_anjay->get_anjay();
    if (auto locked = anjay_.lock()) {
        int result = anjay_security_object_install(locked.get());
        if (result) {
            avs_throw(AnjayException(env, result,
                                     "Could not install security object"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeSecurityObject::purge(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        anjay_security_object_purge(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint NativeSecurityObject::add_instance(jni::JNIEnv &env,
                                             jni::Object<Instance> &instance,
                                             jni::jint preferred_iid) {
    auto accessor = Instance::Accessor{ env, instance };
    anjay_security_instance_t sec{};
    sec.ssid = accessor.get_ssid();
    auto uri = accessor.get_server_uri();
    if (uri) {
        sec.server_uri = uri->c_str();
    }
    sec.bootstrap_server = accessor.get_bootstrap_server();
    sec.security_mode = accessor.get_security_mode();
    sec.client_holdoff_s = accessor.get_client_holdoff_s().value_or(-1);
    sec.bootstrap_timeout_s = accessor.get_bootstrap_timeout_s().value_or(-1);
    auto public_cert_or_psk_identity =
            accessor.get_public_cert_or_psk_identity();
    if (public_cert_or_psk_identity) {
        sec.public_cert_or_psk_identity = reinterpret_cast<const uint8_t *>(
                public_cert_or_psk_identity->data());
        sec.public_cert_or_psk_identity_size =
                public_cert_or_psk_identity->size();
    }
    auto private_cert_or_psk_key = accessor.get_private_cert_or_psk_key();
    if (private_cert_or_psk_key) {
        sec.private_cert_or_psk_key = reinterpret_cast<const uint8_t *>(
                private_cert_or_psk_key->data());
        sec.private_cert_or_psk_key_size = private_cert_or_psk_key->size();
    }
    auto server_public_key = accessor.get_server_public_key();
    if (server_public_key) {
        sec.server_public_key =
                reinterpret_cast<const uint8_t *>(server_public_key->data());
        sec.server_public_key_size = server_public_key->size();
    }
    sec.sms_security_mode = accessor.get_sms_security_mode();
    auto sms_key_parameters = accessor.get_sms_key_parameters();
    if (sms_key_parameters) {
        sec.sms_key_parameters =
                reinterpret_cast<const uint8_t *>(sms_key_parameters->data());
        sec.sms_key_parameters_size = sms_key_parameters->size();
    }
    auto sms_secret_key = accessor.get_sms_secret_key();
    if (sms_secret_key) {
        sec.sms_secret_key =
                reinterpret_cast<const uint8_t *>(sms_secret_key->data());
        sec.sms_secret_key_size = sms_secret_key->size();
    }
    auto server_sms_number = accessor.get_server_sms_number();
    if (server_sms_number) {
        sec.server_sms_number = server_sms_number->c_str();
    }

    if (preferred_iid < 0 || preferred_iid > ANJAY_ID_INVALID) {
        avs_throw(IllegalArgumentException(
                env, "preferred iid out of anjay_iid_t range"));
    }
    anjay_iid_t iid = static_cast<anjay_iid_t>(preferred_iid);
    if (auto locked = anjay_.lock()) {
        if (anjay_security_object_add_instance(locked.get(), &sec, &iid)) {
            return -1;
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
    return iid;
}

jni::jboolean NativeSecurityObject::is_modified(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        return anjay_security_object_is_modified(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeSecurityObject::persist(jni::JNIEnv &env,
                              jni::Object<utils::OutputStream> &output_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::OutputStream::get_avs_stream(&output_stream);

        if (!stream
                || avs_is_err(anjay_security_object_persist(locked.get(),
                                                            stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeSecurityObject::restore(jni::JNIEnv &env,
                              jni::Object<utils::InputStream> &input_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::InputStream::get_avs_stream(&input_stream);

        if (!stream
                || avs_is_err(anjay_security_object_restore(locked.get(),
                                                            stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeSecurityObject::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeSecurityObject>(
            env, jni::Class<NativeSecurityObject>::Find(env), "self",
            jni::MakePeer<NativeSecurityObject, jni::Object<NativeAnjay> &>,
            "init",
            "cleanup",
            METHOD(&NativeSecurityObject::purge, "anjaySecurityObjectPurge"),
            METHOD(&NativeSecurityObject::add_instance, "anjaySecurityObjectAddInstance"),
            METHOD(&NativeSecurityObject::is_modified, "anjaySecurityObjectIsModified"),
            METHOD(&NativeSecurityObject::persist, "anjaySecurityObjectPersist"),
            METHOD(&NativeSecurityObject::restore, "anjaySecurityObjectRestore")
    );
    // clang-format on
}
