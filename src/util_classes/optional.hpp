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

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"
#include "./optional_tag.hpp"

namespace utils {

class Optional {
    jni::JNIEnv &env_;
    jni::Global<jni::Object<Optional>> self_;

public:
    static constexpr auto Name() {
        return OptionalTag::Name();
    }

    operator bool() const {
        return is_present();
    }

    Optional(jni::JNIEnv &env, const jni::Local<jni::Object<Optional>> &value)
            : env_(env), self_(jni::NewGlobal(env, value)) {}

    jni::Local<jni::Object<Optional>> into_java() {
        return jni::NewLocal(env_, self_);
    }

    bool is_present() const {
        return AccessorBase<Optional>{ env_, self_ }
                .get_method<jni::jboolean()>("isPresent")();
    }

    template <typename T>
    jni::Local<jni::Object<T>> get() {
        return jni::Cast(env_, jni::Class<T>::Find(env_),
                         AccessorBase<Optional>{ env_, self_ }
                                 .get_method<jni::Object<>()>("get")());
    }

    template <typename T>
    static Optional of(jni::JNIEnv &env, const T &value) {
        return Optional{ env, AccessorBase<Optional>::get_static_method<
                                      jni::Object<Optional>(jni::Object<>)>(
                                      env, "of")(value) };
    }

    static Optional empty(jni::JNIEnv &env) {
        return Optional{
            env,
            AccessorBase<Optional>::get_static_method<jni::Object<Optional>()>(
                    env, "empty")()
        };
    }
};

} // namespace utils
