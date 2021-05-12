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
#include "./resource_def.hpp"

namespace utils {

struct ResourceDefArrayByReference {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/"
               "NativeAnjayObject$ResourceDefArrayByReference";
    }

    static jni::Local<jni::Object<ResourceDefArrayByReference>>
    New(jni::JNIEnv &env) {
        auto clazz = jni::Class<ResourceDefArrayByReference>::Find(env);
        auto ctor = clazz.GetConstructor(env);
        return clazz.New(env, ctor);
    }

    template <typename Func>
    static void
    for_each(jni::JNIEnv &env,
             const jni::Object<ResourceDefArrayByReference> &instance,
             Func &&func) {
        auto accessor =
                AccessorBase<ResourceDefArrayByReference>{ env, instance };
        auto value =
                accessor.get_value<jni::Array<jni::Object<utils::ResourceDef>>>(
                        "value");
        for (jni::jsize i = 0; i < value.Length(env); ++i) {
            func(ResourceDef::into_native(env, value.Get(env, i)));
        }
    }
};

} // namespace utils
