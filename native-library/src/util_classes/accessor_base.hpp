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

#include <optional>
#include <string>
#include <type_traits>
#include <unordered_map>

#include "./exception.hpp"
#include "./optional_tag.hpp"

namespace utils {

namespace detail {
// Workaround for inability to use static_assert(false, ...) inside if constexpr
template <class T>
struct dependent_false : std::false_type {};

template <typename T>
struct ArrayTypeExtractor : std::false_type {};

template <typename T>
struct ArrayTypeExtractor<T[]> {
    typedef T type;
};

} // namespace detail

template <typename Peer>
class AccessorBase {
protected:
    jni::JNIEnv &env_;
    jni::Global<jni::Object<Peer>> instance_;
    jni::Global<jni::Class<Peer>> class_;

    AccessorBase(AccessorBase &) = delete;
    AccessorBase &operator=(AccessorBase &) = delete;

    template <typename JType>
    auto get_impl(const char *field_name) {
        return instance_.Get(env_,
                             class_.template GetField<JType>(env_, field_name));
    }

    template <typename JType, typename T>
    void set_impl(const char *field_name, const T &value) {
        instance_.Set(env_, class_.template GetField<JType>(env_, field_name),
                      value);
    }

    // Workaround for https://github.com/mapbox/jni.hpp/issues/60
    template <typename R, typename... Args>
    auto get_method_impl(jni::Method<Peer, R(Args...)> &&method) {
        return [&, method = std::move(method)](const Args &... args) {
            return instance_.Call(env_, method, args...);
        };
    }

    template <typename R, typename... Args>
    static auto
    get_static_method_impl(jni::JNIEnv &env,
                           jni::Local<jni::Class<Peer>> &&clazz,
                           jni::StaticMethod<Peer, R(Args...)> &&method) {
        return [&, clazz = std::move(clazz),
                method = std::move(method)](const Args &... args) {
            return clazz.Call(env, method, args...);
        };
    }

public:
    // NOTE: We promote instance as well as the class found through the
    // environment to global-references because local references are only
    // valid in the frame of execution of a native JNI method, and sometimes
    // we need to extend accessor's lifetime.
    explicit AccessorBase(jni::JNIEnv &env, const jni::Object<Peer> &instance)
            : env_(env),
              instance_(jni::NewGlobal(env, instance)),
              class_(jni::NewGlobal(env, jni::Class<Peer>::Find(env))) {}

    jni::JNIEnv &get_env() {
        return env_;
    }

    template <typename Signature>
    auto get_method(const char *name) {
        return get_method_impl(
                class_.template GetMethod<Signature>(env_, name));
    }

    template <typename Signature>
    static auto get_static_method(jni::JNIEnv &env, const char *name) {
        auto clazz = jni::Class<Peer>::Find(env);
        auto method = clazz.template GetStaticMethod<Signature>(env, name);
        return get_static_method_impl(env, std::move(clazz), std::move(method));
    }

    template <typename T>
    auto get_nullable_value(const char *field_name) {
        if constexpr (std::is_same<T, std::string>::value) {
            auto str = get_impl<jni::String>(field_name);
            if (!str.get()) {
                return std::optional<std::string>{};
            }
            return std::optional<std::string>{ jni::Make<std::string>(env_,
                                                                      str) };
        } else if constexpr (std::is_array<T>::value
                             && std::is_arithmetic<
                                        typename detail::ArrayTypeExtractor<
                                                T>::type>::value) {
            using Item = typename detail::ArrayTypeExtractor<T>::type;
            using RetT = typename std::vector<Item>;

            auto arr = get_impl<jni::Array<Item>>(field_name);
            auto vec = jni::Make<RetT>(env_, arr);

            return std::optional<RetT>{ vec };
        } else {
            static_assert(detail::dependent_false<T>::value, "Not implemented");
        }
    }

    template <typename T>
    std::optional<T> get_optional_integer(const char *field_name) {
        auto value = get_optional_value<jni::IntegerTag>(field_name);

        if (!value) {
            return {};
        }
        auto unboxed = jni::Unbox(get_env(), *value);
        T casted = static_cast<T>(unboxed);
        if (static_cast<int64_t>(casted) != static_cast<int64_t>(unboxed)) {
            avs_throw(IllegalArgumentException(
                    env_,
                    std::string{ field_name }
                            + " field has value that is out of range "
                            + std::to_string(std::numeric_limits<T>::min())
                            + " - "
                            + std::to_string(std::numeric_limits<T>::max())));
        }
        return std::make_optional(casted);
    }

    template <typename T>
    std::optional<std::vector<T>> get_optional_array(const char *field_name) {
        if constexpr (std::is_arithmetic<T>::value) {
            auto optional_value =
                    get_impl<jni::Object<OptionalTag>>(field_name);
            auto accessor = AccessorBase<OptionalTag>{ env_, optional_value };

            if (!accessor.template get_method<jni::jboolean()>("isPresent")()) {
                return {};
            }

            auto object =
                    accessor.template get_method<jni::Object<>()>("get")();

            jni::Local<jni::Array<T>> casted{
                env_, jni::Cast(env_, jni::Class<jni::ArrayTag<T>>::Find(env_),
                                object)
                              .release()
            };

            std::vector<T> result = jni::Make<std::vector<T>>(env_, casted);

            return { result };
        } else {
            static_assert(detail::dependent_false<T>::value, "Not implemented");
        }
    }

    template <typename T>
    auto get_value(const char *field_name) {
        if constexpr (std::is_same<T, size_t>::value) {
            auto value = get_impl<jni::jlong>(field_name);
            if (value < 0) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "size_t field " + std::string{ field_name }
                                + " has value that is negative"));
            } else if (static_cast<uint64_t>(value)
                       > std::numeric_limits<size_t>::max()) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "size_t field " + std::string{ field_name }
                                + " has value that is too large"));
            }
            return static_cast<size_t>(value);
        } else if constexpr (std::is_same<T, uint16_t>::value) {
            auto value = get_impl<jni::jint>(field_name);
            if (value < 0) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "uint16_t field " + std::string{ field_name }
                                + " has value that is negative"));
            } else if (static_cast<uint32_t>(value)
                       > std::numeric_limits<uint16_t>::max()) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "uint16_t field " + std::string{ field_name }
                                + " has value that is too large"));
            }
            return static_cast<uint16_t>(value);
        } else if constexpr (std::is_same<T, bool>::value) {
            // Workaround for "warning: narrowing conversion from unsigned char
            // to bool"
            return static_cast<bool>(get_impl<jni::jboolean>(field_name));
        } else if constexpr (std::is_same<T, char>::value) {
            auto value = get_impl<jni::jchar>(field_name);
            if (value > std::numeric_limits<char>::max()) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "char field " + std::string{ field_name }
                                + " has value that is too large"));
            } else if (value < std::numeric_limits<char>::min()) {
                avs_throw(IllegalArgumentException(
                        env_,
                        "char field " + std::string{ field_name }
                                + " has value that is negative"));
            }
            return static_cast<char>(value);
        } else {
            // Let's hope for the best.
            return get_impl<T>(field_name);
        }
    }

    template <typename T>
    auto get_optional_value(const char *field_name) {
        auto optional_value = get_impl<jni::Object<OptionalTag>>(field_name);
        auto accessor = AccessorBase<OptionalTag>{ env_, optional_value };

        if (!accessor.template get_method<jni::jboolean()>("isPresent")()) {
            return std::optional<jni::Local<jni::Object<T>>>{};
        }
        auto value = accessor.template get_method<jni::Object<>()>("get")();
        auto casted = jni::Cast(env_, jni::Class<T>::Find(env_), value);
        return std::make_optional(std::move(casted));
    }

    template <typename JavaT, typename NativeT>
    auto
    get_enum_value(const char *field_name,
                   const std::unordered_map<std::string, NativeT> &mapping) {
        struct Enum {
            static constexpr auto Name() {
                return "java/lang/Enum";
            }
        };
        auto field_value = get_value<jni::Object<JavaT>>(field_name);
        if (!jni::IsInstanceOf(env_, field_value.get(),
                               *jni::Class<Enum>::Find(env_))) {
            avs_throw(ClassCastException(env_,
                                         "Field " + std::string{ field_name }
                                                 + " is not a Java Enum"));
        }
        auto accessor = AccessorBase<JavaT>{ env_, field_value };

        auto value = jni::Make<std::string>(
                env_, accessor.template get_method<jni::String()>("name")());

        auto mapped_to = mapping.find(value);
        if (mapped_to == mapping.end()) {
            avs_throw(IllegalArgumentException(env_, "Unsupported enum value: "
                                                             + value));
        }
        return mapped_to->second;
    }

    template <typename T>
    void set_value(const char *field_name, const T &value) {
        if constexpr (std::is_same<T, int32_t>::value) {
            set_impl<jni::jint>(field_name, static_cast<jni::jint>(value));
        } else if constexpr (std::is_same<T, int64_t>::value) {
            set_impl<jni::jlong>(field_name, static_cast<jni::jlong>(value));
        } else if constexpr (std::is_same<T, bool>::value) {
            set_impl<jni::jboolean>(field_name,
                                    static_cast<jni::jboolean>(value));
        } else if constexpr (std::is_same<T, float>::value) {
            set_impl<jni::jfloat>(field_name, value);
        } else if constexpr (std::is_same<T, double>::value) {
            set_impl<jni::jdouble>(field_name, value);
        } else if constexpr (std::is_same<T, std::string>::value) {
            set_impl<jni::String>(field_name,
                                  jni::Make<jni::String>(env_, value));
        } else {
            set_impl<jni::Object<T>>(field_name, value.into_object(env_));
        }
    }
};

} // namespace utils
