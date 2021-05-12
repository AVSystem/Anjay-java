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

#include "./native_output_context.hpp"

NativeOutputContext::NativeOutputContext(
        jni::JNIEnv &env,
        jni::Object<utils::NativeOutputContextPointer> &context)
        : ctx_(utils::NativeOutputContextPointer::into_native(env, context)) {}

jni::jint NativeOutputContext::ret_i32(jni::JNIEnv &, jni::jint value) {
    return anjay_ret_i32(ctx_, value);
}

jni::jint NativeOutputContext::ret_i64(jni::JNIEnv &, jni::jlong value) {
    return anjay_ret_i64(ctx_, value);
}

jni::jint NativeOutputContext::ret_bool(jni::JNIEnv &, jni::jboolean value) {
    return anjay_ret_bool(ctx_, value);
}

jni::jint NativeOutputContext::ret_float(jni::JNIEnv &, jni::jfloat value) {
    return anjay_ret_float(ctx_, value);
}

jni::jint NativeOutputContext::ret_double(jni::JNIEnv &, jni::jdouble value) {
    return anjay_ret_double(ctx_, value);
}

jni::jint NativeOutputContext::ret_string(jni::JNIEnv &env,
                                          const jni::String &value) {
    auto str = jni::Make<std::string>(env, value);
    return anjay_ret_string(ctx_, str.c_str());
}

jni::jint
NativeOutputContext::ret_objlnk(jni::JNIEnv &env,
                                const jni::Object<utils::Objlnk> &value) {
    auto objlnk = utils::Objlnk::into_native(env, value);
    return anjay_ret_objlnk(ctx_, objlnk.oid, objlnk.iid);
}

jni::Local<jni::Object<utils::NativeBytesContextPointer>>
NativeOutputContext::ret_bytes_begin(jni::JNIEnv &env, jni::jint length) {
    return utils::NativeBytesContextPointer::into_object(
            env, anjay_ret_bytes_begin(ctx_, length));
}

void NativeOutputContext::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(name)

    // clang-format off
    jni::RegisterNativePeer<NativeOutputContext>(
            env, jni::Class<NativeOutputContext>::Find(env), "self",
            jni::MakePeer<NativeOutputContext,
                          jni::Object<utils::NativeOutputContextPointer> &>,
            "init",
            "cleanup",
            METHOD(&NativeOutputContext::ret_i32, "anjayRetI32"),
            METHOD(&NativeOutputContext::ret_i64, "anjayRetI64"),
            METHOD(&NativeOutputContext::ret_bool, "anjayRetBool"),
            METHOD(&NativeOutputContext::ret_float, "anjayRetFloat"),
            METHOD(&NativeOutputContext::ret_double, "anjayRetDouble"),
            METHOD(&NativeOutputContext::ret_string, "anjayRetString"),
            METHOD(&NativeOutputContext::ret_objlnk, "anjayRetObjlnk"),
            METHOD(&NativeOutputContext::ret_bytes_begin, "anjayRetBytesBegin")
    );
    // clang-format on
}
