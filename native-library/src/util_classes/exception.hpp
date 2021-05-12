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

#include <avsystem/commons/avs_log.h>

#include "../global_context.hpp"

#include "./construct.hpp"

#include <cassert>
#include <sstream>
#include <string>

namespace utils {

namespace detail {
struct Throwable {
    static constexpr auto Name() {
        return "java/lang/Throwable";
    }
};

struct StackTraceElement {
    static constexpr auto Name() {
        return "java/lang/StackTraceElement";
    }
};

template <typename>
class ExceptionWrapper;

class OriginAwareException {
public:
    virtual ~OriginAwareException() noexcept {}

    OriginAwareException(const OriginAwareException &) = default;
    OriginAwareException &operator=(const OriginAwareException &) = default;

    OriginAwareException(OriginAwareException &&) = default;
    OriginAwareException &operator=(OriginAwareException &&) = default;

    std::string get_throw_location() const {
        std::stringstream ss;
        ss << file_ << ":" << line_;
        if (function_) {
            ss << " (" << function_ << ")";
        }
        return ss.str();
    }

private:
    const char *file_;
    unsigned line_;
    const char *function_;

    OriginAwareException(const char *file, unsigned line, const char *function)
            : file_(file), line_(line), function_(function) {}

    template <typename>
    friend class ::utils::detail::ExceptionWrapper;
};

template <typename T>
class ExceptionWrapper : public T, public OriginAwareException {
public:
    ExceptionWrapper(const char *file,
                     unsigned line,
                     const char *function,
                     T &&e)
            : T(std::move(e)), OriginAwareException(file, line, function) {}
};

template <typename T>
ExceptionWrapper<T>
wrap_exception(const char *file, unsigned line, const char *function, T &&e) {
    return ExceptionWrapper<T>(file, line, function, std::forward<T>(e));
}

void avs_log_and_clear_exception_impl(avs_log_level_t level,
                                      const char *file,
                                      unsigned line);

} // namespace detail

} // namespace utils

struct AnjayException : public jni::PendingJavaException {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayException";
    }

    AnjayException(JNIEnv &env, jni::jint error_code, const std::string &str) {
        auto java_exception = utils::construct<AnjayException>(
                env, error_code, jni::Make<jni::String>(env, str));
        env.Throw(reinterpret_cast<::jthrowable>(java_exception.get()));
    }
};

struct ClassCastException : public jni::PendingJavaException {
    ClassCastException(JNIEnv &env, const char *str)
            : jni::PendingJavaException() {
        env.ThrowNew(env.FindClass("java/lang/ClassCastException"), str);
    }

    ClassCastException(JNIEnv &env, const std::string &str)
            : ClassCastException(env, str.c_str()) {}
};

struct IllegalArgumentException : public jni::PendingJavaException {
    IllegalArgumentException(JNIEnv &env, const char *str)
            : jni::PendingJavaException() {
        env.ThrowNew(env.FindClass("java/lang/IllegalArgumentException"), str);
    }

    IllegalArgumentException(JNIEnv &env, const std::string &str)
            : IllegalArgumentException(env, str.c_str()) {}
};

struct IllegalStateException : public jni::PendingJavaException {
    IllegalStateException(JNIEnv &env, const char *str)
            : jni::PendingJavaException() {
        env.ThrowNew(env.FindClass("java/lang/IllegalStateException"), str);
    }

    IllegalStateException(JNIEnv &env, const std::string &str)
            : IllegalStateException(env, str.c_str()) {}
};

struct UnsupportedOperationException : public jni::PendingJavaException {
    UnsupportedOperationException(JNIEnv &env, const char *str)
            : jni::PendingJavaException() {
        env.ThrowNew(env.FindClass("java/lang/UnsupportedOperationException"),
                     str);
    }

    UnsupportedOperationException(JNIEnv &env, const std::string &str)
            : UnsupportedOperationException(env, str.c_str()) {}
};

#define avs_throw(e)                       \
    throw(::utils::detail::wrap_exception( \
            __FILE__, __LINE__, __PRETTY_FUNCTION__, (e)))

#define avs_log_and_clear_exception(level)             \
    ::utils::detail::avs_log_and_clear_exception_impl( \
            AVS_LOG_##level, __FILE__, __LINE__)
