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

#include "./util_classes/byte_buffer.hpp"
#include "./util_classes/native_bytes_context_pointer.hpp"

class NativeBytesContext {
    anjay_ret_bytes_ctx_t *ctx_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeBytesContext";
    }

    static void register_native(jni::JNIEnv &env);

    NativeBytesContext(jni::JNIEnv &env,
                       jni::Object<utils::NativeBytesContextPointer> &context);

    jni::jint append(jni::JNIEnv &env,
                     const jni::Array<jni::jbyte> &chunk,
                     jni::jint length);
};
