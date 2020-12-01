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

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"

#include "./util_classes/attributes.hpp"
#include "./util_classes/exception.hpp"
#include "./util_classes/input_stream.hpp"
#include "./util_classes/output_stream.hpp"

#include <optional>
#include <stdexcept>

class NativeAttrStorage {
    std::weak_ptr<anjay_t> anjay_;

    template <typename T>
    T cast_id(jni::jint id) {
        T result = static_cast<T>(id);
        if (result != id) {
            avs_throw(std::runtime_error("id out of range"));
        }
        return result;
    }

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeAttrStorage";
    }

    NativeAttrStorage(jni::JNIEnv &env, const jni::Object<NativeAnjay> &anjay);

    static void register_native(jni::JNIEnv &env);

    void purge(jni::JNIEnv &);

    void set_object_attrs(jni::JNIEnv &env,
                          jni::jint ssid,
                          jni::jint oid,
                          jni::Object<utils::ObjectInstanceAttrs> &attrs);

    void set_instance_attrs(jni::JNIEnv &env,
                            jni::jint ssid,
                            jni::jint oid,
                            jni::jint iid,
                            jni::Object<utils::ObjectInstanceAttrs> &attrs);

    void set_resource_attrs(jni::JNIEnv &env,
                            jni::jint ssid,
                            jni::jint oid,
                            jni::jint iid,
                            jni::jint rid,
                            jni::Object<utils::ResourceAttrs> &attrs);

    void set_resource_instance_attrs(jni::JNIEnv &env,
                                     jni::jint ssid,
                                     jni::jint oid,
                                     jni::jint iid,
                                     jni::jint rid,
                                     jni::jint riid,
                                     jni::Object<utils::ResourceAttrs> &attrs);

    jni::jboolean is_modified(jni::JNIEnv &);

    jni::jint persist(jni::JNIEnv &,
                      jni::Object<utils::OutputStream> &output_stream);

    jni::jint restore(jni::JNIEnv &,
                      jni::Object<utils::InputStream> &input_stream);

    static jni::jint get_attr_period_none(jni::JNIEnv &,
                                          jni::Class<NativeAttrStorage> &);

    static jni::jdouble get_attr_value_none(jni::JNIEnv &,
                                            jni::Class<NativeAttrStorage> &);
};
