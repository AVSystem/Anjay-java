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

#include "./exception.hpp"
#include "./accessor_base.hpp"

namespace utils {

namespace detail {

namespace {

std::string get_file_msg(const char *file) {
    try {
        throw;
    } catch (OriginAwareException &e) {
        // avs_log takes file and line number to indicate where does the log
        // come from. In case of handline exceptions, we want to display throw
        // location too - to make it fit into avs_log API, we cheat a little by
        // using the whole "thrown at $THROW_FILE:$THROW_LINE, caught at
        // $CATCH_FILE" string as if it was a filename. That way it will look
        // fine after avs_log appends ":$CATCH_LINE".
        return std::string("thrown at ") + e.get_throw_location()
               + ", caught at " + file;
    } catch (...) {
        return std::string("caught at ") + file;
    }
}

void log_and_clear_java_exception(JNIEnv &env,
                                  avs_log_level_t level,
                                  const char *file,
                                  unsigned line) try {
    assert(jni::ExceptionCheck(env));
    auto exception = jni::Object<Throwable>(jni::ExceptionOccurred(env));
    // Need to do it here, otherwise any later call will throw,
    // because there's a pending exception.
    jni::ExceptionClear(env);

    auto accessor = AccessorBase<Throwable>{ env, exception };
    auto stack_trace =
            accessor.get_method<jni::Array<jni::Object<StackTraceElement>>()>(
                    "getStackTrace")();
    auto exception_name = jni::Make<std::string>(
            env, accessor.get_method<jni::String()>("toString")());

    avs_log_internal_l__(level, "anjay_jni", file, line,
                         "Java exception: %s occurred. Stack trace:",
                         exception_name.c_str());
    for (jni::jsize i = 0; i < stack_trace.Length(env); ++i) {
        avs_log_internal_l__(
                level, "anjay_jni", file, line, "%s",
                jni::Make<std::string>(env,
                                       AccessorBase<StackTraceElement>{
                                               env, stack_trace.Get(env, i) }
                                               .get_method<jni::String()>(
                                                       "toString")())
                        .c_str());
    }
} catch (...) {
    if (jni::ExceptionCheck(env)) {
        jni::ExceptionClear(env);
    }
    avs_log_internal_l__(level, "anjay_jni", file, line,
                         "Unknown exception while trying to obtain the details "
                         "of Java exception...");
}

} // namespace

void avs_log_and_clear_exception_impl(avs_log_level_t level,
                                      const char *file,
                                      unsigned line) {
    // Java exception case
    try {
        if (GlobalContext::call_with_env([=](auto &&env) {
                if (jni::ExceptionCheck(*env)) {
                    log_and_clear_java_exception(
                            *env, level, get_file_msg(file).c_str(), line);
                    assert(!jni::ExceptionCheck(*env));
                    return true;
                }
                return false;
            })) {
            return;
        }
    } catch (...) {
        avs_log_internal_l__(level, "anjay_jni", get_file_msg(file).c_str(),
                             line,
                             "Exception during GlobalContext::call_with_env()");
        return;
    }
    // C++ exception case
    try {
        throw;
    } catch (std::exception &e) {
        avs_log_internal_l__(level, "anjay_jni", get_file_msg(file).c_str(),
                             line, "%s", e.what());
    } catch (...) {
        avs_log_internal_l__(level, "anjay_jni", get_file_msg(file).c_str(),
                             line, "Unknown exception");
    }
}

} // namespace detail

} // namespace utils
