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

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/byte_buffer.hpp"
#include "./util_classes/native_bytes_context_pointer.hpp"
#include "./util_classes/native_output_context_pointer.hpp"
#include "./util_classes/objlnk.hpp"

class NativeOutputContext {
    anjay_output_ctx_t *ctx_;

    NativeOutputContext(const NativeOutputContext &) = delete;
    NativeOutputContext &operator=(const NativeOutputContext &) = delete;

    NativeOutputContext(NativeOutputContext &&) = delete;
    NativeOutputContext &operator=(NativeOutputContext &&) = delete;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeOutputContext";
    }

    static void register_native(jni::JNIEnv &env);

    NativeOutputContext(
            jni::JNIEnv &env,
            jni::Object<utils::NativeOutputContextPointer> &context);

    jni::jint ret_i32(jni::JNIEnv &, jni::jint value);
    jni::jint ret_i64(jni::JNIEnv &, jni::jlong value);
    jni::jint ret_bool(jni::JNIEnv &, jni::jboolean value);
    jni::jint ret_float(jni::JNIEnv &, jni::jfloat value);
    jni::jint ret_double(jni::JNIEnv &, jni::jdouble value);
    jni::jint ret_string(jni::JNIEnv &env, const jni::String &value);
    jni::jint ret_objlnk(jni::JNIEnv &env,
                         const jni::Object<utils::Objlnk> &value);
    jni::Local<jni::Object<utils::NativeBytesContextPointer>>
    ret_bytes_begin(jni::JNIEnv &env, jni::jint length);
};
