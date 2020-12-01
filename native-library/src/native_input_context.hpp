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

#include "./util_classes/accessor_base.hpp"
#include "./util_classes/native_input_context_pointer.hpp"
#include "./util_classes/objlnk.hpp"

namespace details {
template <typename T>
struct InputCtx;

template <>
struct InputCtx<int32_t> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$IntegerContext";
    }
};

template <>
struct InputCtx<int64_t> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$LongContext";
    }
};

template <>
struct InputCtx<bool> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$BooleanContext";
    }
};

template <>
struct InputCtx<float> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$FloatContext";
    }
};

template <>
struct InputCtx<double> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$DoubleContext";
    }
};

template <>
struct InputCtx<std::string> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$StringContext";
    }
};

template <>
struct InputCtx<utils::Objlnk> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$ObjlnkContext";
    }
};

template <>
struct InputCtx<uint8_t[]> {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext$BytesContext";
    }
};

} // namespace details

class NativeInputContext {
    anjay_input_ctx_t *ctx_;

    template <typename T, typename Getter>
    int get_value(jni::JNIEnv &env,
                  jni::Object<details::InputCtx<T>> &ctx,
                  Getter &&getter) {
        T value;
        int result = getter(ctx_, &value);
        if (result) {
            return result;
        }
        auto accessor = utils::AccessorBase<details::InputCtx<T>>{ env, ctx };
        accessor.template set_value<T>("value", value);
        return 0;
    }

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeInputContext";
    }

    static void register_native(jni::JNIEnv &env);

    NativeInputContext(jni::JNIEnv &env,
                       jni::Object<utils::NativeInputContextPointer> &context);

    jni::jint get_i32(jni::JNIEnv &env,
                      jni::Object<details::InputCtx<int32_t>> &ctx);
    jni::jint get_i64(jni::JNIEnv &env,
                      jni::Object<details::InputCtx<int64_t>> &ctx);
    jni::jint get_bool(jni::JNIEnv &env,
                       jni::Object<details::InputCtx<bool>> &ctx);
    jni::jint get_float(jni::JNIEnv &env,
                        jni::Object<details::InputCtx<float>> &ctx);
    jni::jint get_double(jni::JNIEnv &env,
                         jni::Object<details::InputCtx<double>> &ctx);
    jni::jint get_string(jni::JNIEnv &env,
                         jni::Object<details::InputCtx<std::string>> &ctx);
    jni::jint get_objlnk(jni::JNIEnv &env,
                         jni::Object<details::InputCtx<utils::Objlnk>> &ctx);
    jni::jint get_bytes(jni::JNIEnv &env,
                        jni::Object<details::InputCtx<uint8_t[]>> &ctx);
};
