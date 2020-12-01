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

#include "./native_server_object.hpp"

#include "./util_classes/exception.hpp"

#include <avsystem/commons/avs_stream_simple_io.h>

NativeServerObject::NativeServerObject(jni::JNIEnv &env,
                                       const jni::Object<NativeAnjay> &instance)
        : anjay_() {
    auto native_anjay = NativeAnjay::into_native(env, instance);
    anjay_ = native_anjay->get_anjay();
    if (auto locked = anjay_.lock()) {
        if (anjay_server_object_install(locked.get())) {
            avs_throw(std::runtime_error("Could not install server object"));
        }
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

void NativeServerObject::purge(jni::JNIEnv &) {
    if (auto locked = anjay_.lock()) {
        anjay_server_object_purge(locked.get());
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

jni::jint NativeServerObject::add_instance(jni::JNIEnv &env,
                                           jni::Object<Instance> &instance,
                                           jni::jint preferred_iid) {
    auto accessor = Instance::Accessor{ env, instance };
    anjay_server_instance_t serv{};
    serv.ssid = accessor.get_ssid();
    serv.lifetime = accessor.get_lifetime();
    serv.default_min_period = accessor.get_default_min_period().value_or(-1);
    serv.default_max_period = accessor.get_default_max_period().value_or(-1);
    serv.disable_timeout = accessor.get_disable_timeout().value_or(-1);
    auto binding = accessor.get_binding();
    if (binding) {
        serv.binding = binding->c_str();
    }

    serv.notification_storing = accessor.get_notification_storing();


    if (preferred_iid < 0 || preferred_iid > ANJAY_ID_INVALID) {
        avs_throw(std::runtime_error("preferred iid out of anjay_iid_t range"));
    }
    anjay_iid_t iid = static_cast<anjay_iid_t>(preferred_iid);
    if (auto locked = anjay_.lock()) {
        if (anjay_server_object_add_instance(locked.get(), &serv, &iid)) {
            return -1;
        }
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
    return iid;
}

jni::jboolean NativeServerObject::is_modified(jni::JNIEnv &) {
    if (auto locked = anjay_.lock()) {
        return anjay_server_object_is_modified(locked.get());
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

jni::jint
NativeServerObject::persist(jni::JNIEnv &,
                            jni::Object<utils::OutputStream> &output_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::OutputStream::get_avs_stream(&output_stream);

        if (!stream
                || avs_is_err(anjay_server_object_persist(locked.get(),
                                                          stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

jni::jint
NativeServerObject::restore(jni::JNIEnv &,
                            jni::Object<utils::InputStream> &input_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::InputStream::get_avs_stream(&input_stream);

        if (!stream
                || avs_is_err(anjay_server_object_restore(locked.get(),
                                                          stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(std::runtime_error("anjay object expired"));
    }
}

void NativeServerObject::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeServerObject>(
            env, jni::Class<NativeServerObject>::Find(env), "self",
            jni::MakePeer<NativeServerObject, jni::Object<NativeAnjay> &>,
            "init",
            "cleanup",
            METHOD(&NativeServerObject::purge, "anjayServerObjectPurge"),
            METHOD(&NativeServerObject::add_instance, "anjayServerObjectAddInstance"),
            METHOD(&NativeServerObject::is_modified, "anjayServerObjectIsModified"),
            METHOD(&NativeServerObject::persist, "anjayServerObjectPersist"),
            METHOD(&NativeServerObject::restore, "anjayServerObjectRestore")
    );
    // clang-format on
}
