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

#include <anjay/anjay.h>

#include "./jni_wrapper.hpp"

#include "./native_anjay.hpp"

#include "./util_classes/exception.hpp"
#include "./util_classes/input_stream.hpp"
#include "./util_classes/output_stream.hpp"

#include <optional>
#include <stdexcept>

class NativeAccessControl {

    std::weak_ptr<anjay_t> anjay_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeAccessControl";
    }

    NativeAccessControl(jni::JNIEnv &env,
                        const jni::Object<NativeAnjay> &anjay);

    static void register_native(jni::JNIEnv &env);

    void purge(jni::JNIEnv &);

    jni::jboolean is_modified(jni::JNIEnv &);

    jni::jint persist(jni::JNIEnv &,
                      jni::Object<utils::OutputStream> &output_stream);

    jni::jint restore(jni::JNIEnv &,
                      jni::Object<utils::InputStream> &input_stream);

    void set_acl(jni::JNIEnv &env,
                 jni::jint oid,
                 jni::jint iid,
                 jni::jint ssid,
                 jni::jint access_mask);
};
