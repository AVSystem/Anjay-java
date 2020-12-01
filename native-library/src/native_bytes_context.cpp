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

#include "./native_bytes_context.hpp"

NativeBytesContext::NativeBytesContext(
        jni::JNIEnv &env,
        jni::Object<utils::NativeBytesContextPointer> &context)
        : ctx_(utils::NativeBytesContextPointer::into_native(env, context)) {}

jni::jint NativeBytesContext::append(jni::JNIEnv &env,
                                     const jni::Array<jni::jbyte> &chunk,
                                     jni::jint length) {
    std::vector<jni::jbyte> buffer(std::min(length, 4096));
    size_t start = 0;
    while (length > 0) {
        const size_t to_read =
                std::min(static_cast<size_t>(length), buffer.size());
        jni::GetArrayRegion(env, *chunk, start, to_read, buffer.data());
        int result = anjay_ret_bytes_append(ctx_, buffer.data(), to_read);
        if (result) {
            return result;
        }
        length -= to_read;
        start += to_read;
    }
    return 0;
}

void NativeBytesContext::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeBytesContext>(
            env, jni::Class<NativeBytesContext>::Find(env), "self",
            jni::MakePeer<NativeBytesContext,
                          jni::Object<utils::NativeBytesContextPointer> &>,
            "init",
            "cleanup",
            METHOD(&NativeBytesContext::append, "anjayRetBytesAppend")
    );
    // clang-format on
}
