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

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"

namespace utils {

template <typename WrapperType, typename NativeType>
struct NativePointer {
    static constexpr auto Name() {
        return WrapperType::Name();
    }

    static NativeType *into_native(jni::JNIEnv &env,
                                   jni::Object<WrapperType> &instance) {
        auto accessor = AccessorBase<WrapperType>{ env, instance };
        return reinterpret_cast<NativeType *>(
                accessor.template get_value<jni::jlong>("pointer"));
    }

    static jni::Local<jni::Object<WrapperType>>
    into_object(jni::JNIEnv &env, NativeType *pointer) {
        auto clazz = jni::Class<WrapperType>::Find(env);
        auto ctor = clazz.template GetConstructor<jni::jlong>(env);
        return clazz.New(env, ctor, reinterpret_cast<jni::jlong>(pointer));
    }
};

} // namespace utils
