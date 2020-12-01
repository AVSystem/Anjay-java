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

#include <avsystem/commons/avs_log.h>

#include "./global_context.hpp"
#include "./native_log.hpp"

#include <iostream>

void NativeLog::log_handler(avs_log_level_t level,
                            const char *module,
                            const char *message) try {
    GlobalContext::call_with_env([=](jni::UniqueEnv &&env) {
        try {
            const std::string java_module = "Anjay." + std::string(module);
            auto logger = utils::AccessorBase<utils::Logger>::get_static_method<
                    jni::Object<utils::Logger>(jni::String)>(*env, "getLogger")(
                    jni::Make<jni::String>(*env, java_module));

            auto accessor = utils::AccessorBase<utils::Logger>{ *env, logger };

            accessor.get_method<void(jni::Object<utils::Level>, jni::String)>(
                    "log")(utils::Level::from_native(*env, level),
                           jni::Make<jni::String>(*env, message));
        } catch (jni::PendingJavaException &) {
            jni::ExceptionClear(*env);
            throw;
        }
    });
} catch (...) {
    std::cerr << "Exception occurred in log_handler(). The log to print was: "
              << std::endl
              << message << std::endl;
}

void NativeLog::initialize(jni::JNIEnv &, jni::Class<NativeLog> &) {
    avs_log_set_default_level(AVS_LOG_TRACE);
    avs_log_set_handler(log_handler);
}

void NativeLog::register_native(jni::JNIEnv &env) {
#define STATIC_METHOD(MethodPtr, Name) \
    jni::MakeNativeMethod<decltype(MethodPtr), (MethodPtr)>(Name)

    jni::RegisterNatives(env, *jni::Class<NativeLog>::Find(env),
                         STATIC_METHOD(&NativeLog::initialize, "initialize"));
#undef STATIC_METHOD
}
