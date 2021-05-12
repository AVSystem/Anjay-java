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

#include "./jni_wrapper.hpp"

#include "./native_input_context.hpp"

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/byte_buffer.hpp"

NativeInputContext::NativeInputContext(
        jni::JNIEnv &env,
        jni::Object<utils::NativeInputContextPointer> &context)
        : ctx_(utils::NativeInputContextPointer::into_native(env, context)) {}

jni::jint
NativeInputContext::get_i32(jni::JNIEnv &env,
                            jni::Object<details::InputCtx<int32_t>> &ctx) {
    return get_value<int32_t>(env, ctx, &anjay_get_i32);
}

jni::jint
NativeInputContext::get_i64(jni::JNIEnv &env,
                            jni::Object<details::InputCtx<int64_t>> &ctx) {
    return get_value<int64_t>(env, ctx, &anjay_get_i64);
}

jni::jint
NativeInputContext::get_bool(jni::JNIEnv &env,
                             jni::Object<details::InputCtx<bool>> &ctx) {
    return get_value<bool>(env, ctx, &anjay_get_bool);
}

jni::jint
NativeInputContext::get_float(jni::JNIEnv &env,
                              jni::Object<details::InputCtx<float>> &ctx) {
    return get_value<float>(env, ctx, &anjay_get_float);
}

jni::jint
NativeInputContext::get_double(jni::JNIEnv &env,
                               jni::Object<details::InputCtx<double>> &ctx) {
    return get_value<double>(env, ctx, &anjay_get_double);
}

jni::jint NativeInputContext::get_string(
        jni::JNIEnv &env, jni::Object<details::InputCtx<std::string>> &ctx) {
    return get_value<std::string>(env, ctx, [&](auto *ctx, auto *out_value) {
        int result;
        do {
            char chunk[1024];
            result = anjay_get_string(ctx, chunk, sizeof(chunk));
            if (result >= 0) {
                out_value->append(chunk);
            }
        } while (result == ANJAY_BUFFER_TOO_SHORT);
        return result;
    });
}

jni::jint NativeInputContext::get_objlnk(
        jni::JNIEnv &env, jni::Object<details::InputCtx<utils::Objlnk>> &ctx) {
    return get_value<utils::Objlnk>(env, ctx, [&](auto *ctx, auto *out_value) {
        return anjay_get_objlnk(ctx, &out_value->oid, &out_value->iid);
    });
}

jni::jint
NativeInputContext::get_bytes(jni::JNIEnv &env,
                              jni::Object<details::InputCtx<uint8_t[]>> &ctx) {
    auto context_accessor =
            utils::AccessorBase<details::InputCtx<uint8_t[]>>{ env, ctx };
    auto slice = utils::ByteBuffer(
            env, context_accessor.get_value<jni::Object<utils::ByteBuffer>>(
                         "slice"));
    size_t capacity = slice.capacity();
    size_t bytes_read = 0;
    bool message_finished = false;
    std::vector<jni::jbyte> buffer(
            std::min(capacity, static_cast<size_t>(4096)));

    while (capacity > 0 && !message_finished) {
        size_t chunk_bytes_read;
        int result = anjay_get_bytes(ctx_, &chunk_bytes_read, &message_finished,
                                     buffer.data(),
                                     std::min(buffer.size(), capacity));
        if (result < 0) {
            return result;
        }
        capacity -= chunk_bytes_read;
        bytes_read += chunk_bytes_read;
        slice.put(buffer);
    }
    context_accessor.set_value<int>("bytesRead", static_cast<int>(bytes_read));
    context_accessor.set_value<bool>("messageFinished", message_finished);
    return 0;
}

void NativeInputContext::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeInputContext>(
            env, jni::Class<NativeInputContext>::Find(env), "self",
            jni::MakePeer<NativeInputContext, jni::Object<utils::NativeInputContextPointer> &>,
            "init",
            "cleanup",
            METHOD(&NativeInputContext::get_i32, "anjayGetI32"),
            METHOD(&NativeInputContext::get_i64, "anjayGetI64"),
            METHOD(&NativeInputContext::get_bool, "anjayGetBool"),
            METHOD(&NativeInputContext::get_float, "anjayGetFloat"),
            METHOD(&NativeInputContext::get_double, "anjayGetDouble"),
            METHOD(&NativeInputContext::get_string, "anjayGetString"),
            METHOD(&NativeInputContext::get_objlnk, "anjayGetObjlnk"),
            METHOD(&NativeInputContext::get_bytes, "anjayGetBytes")
    );
    // clang-format on
}
