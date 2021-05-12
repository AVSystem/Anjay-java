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

#include "./jni_wrapper.hpp"

#include <optional>

class GlobalContext {
    // According to this
    // https://latkin.org/blog/2016/02/01/jni-object-lifetimes-quick-reference/
    // the pointer to JavaVM is valid across threads (even though we're not
    // running more than one). We're making use of this to extract JNIEnv *
    // (which may be different between threads), that are unfortunately
    // necessary in some portions of code that can't easily obtain it in another
    // way.
    JavaVM *const vm_;

    static std::optional<GlobalContext> SELF;
    struct ConstructorAccess {};

    GlobalContext(const GlobalContext &) = delete;
    GlobalContext &operator=(const GlobalContext &) = delete;

    GlobalContext(GlobalContext &&) = delete;
    GlobalContext &operator=(GlobalContext &&) = delete;

public:
    GlobalContext(ConstructorAccess, JavaVM *vm);

    static GlobalContext &init(JavaVM *vm);
    static GlobalContext &instance();

    /**
     * Calls the specified @p functor with UniqueEnv&& passed that's valid
     * within this thread.
     *
     * NOTE: this function may throw std::runtime_error on its own, if getting
     * the JNIEnv instance out of JavaVM fails.
     */
    template <typename Functor>
    static auto call_with_env(Functor &&functor) {
        auto &instance = GlobalContext::instance();
        return functor(std::move(jni::GetAttachedEnv(*instance.vm_)));
    }
};
