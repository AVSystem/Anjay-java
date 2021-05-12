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

#include "../util_classes/accessor_base.hpp"
#include "../util_classes/construct.hpp"

#include <cstring>
#include <string>

namespace compat {

struct SocketAddress {
    static constexpr auto Name() {
        return "java/net/SocketAddress";
    }
};

class InetAddress {
    jni::JNIEnv &env_;
    jni::Global<jni::Object<InetAddress>> self_;

public:
    static constexpr auto Name() {
        return "java/net/InetAddress";
    }

    InetAddress(jni::JNIEnv &env,
                const jni::Local<jni::Object<InetAddress>> &self)
            : env_(env), self_(jni::NewGlobal(env_, self)) {}

    jni::Local<jni::Object<InetAddress>> into_java() const {
        return jni::NewLocal(env_, self_);
    }

    std::string get_host_address() {
        return jni::Make<std::string>(
                env_, utils::AccessorBase<InetAddress>{ env_, self_ }
                              .get_method<jni::String()>("getHostAddress")());
    }

    std::string get_host_name() {
        return jni::Make<std::string>(
                env_, utils::AccessorBase<InetAddress>{ env_, self_ }
                              .get_method<jni::String()>("getHostName")());
    }

    bool is_ipv4() {
        return strchr(get_host_address().c_str(), ':') != nullptr;
    }

    static std::vector<InetAddress> get_all_by_name(jni::JNIEnv &env,
                                                    const std::string &host) {
        auto resolved_addrs =
                utils::AccessorBase<InetAddress>::get_static_method<
                        jni::Array<jni::Object<InetAddress>>(jni::String)>(
                        env, "getAllByName")(jni::Make<jni::String>(env, host));

        std::vector<InetAddress> result{};
        for (jni::jsize i = 0; i < resolved_addrs.Length(env); ++i) {
            result.emplace_back(env, resolved_addrs.Get(env, i));
        }
        return result;
    }
};

struct InetSocketAddress {
    static constexpr auto Name() {
        return "java/net/InetSocketAddress";
    }

    static jni::Local<jni::Object<SocketAddress>>
    from_host_port(jni::JNIEnv &env, const std::string &host, int port) {
        return jni::Cast<SocketAddress>(
                env, jni::Class<SocketAddress>::Find(env),
                utils::construct<InetSocketAddress>(
                        env, jni::Make<jni::String>(env, host), port));
    }

    static jni::Local<jni::Object<SocketAddress>>
    from_host_port(jni::JNIEnv &env,
                   const std::string &host,
                   const std::string &port) {
        return from_host_port(env, host, std::stoi(port));
    }

    static jni::Local<jni::Object<SocketAddress>>
    from_port(jni::JNIEnv &env, const std::string &port) {
        return jni::Cast<SocketAddress>(
                env, jni::Class<SocketAddress>::Find(env),
                utils::construct<InetSocketAddress>(env, std::stoi(port)));
    }

    static jni::Local<jni::Object<SocketAddress>>
    from_resolved(jni::JNIEnv &env, const InetAddress &address, int port) {
        return jni::Cast<SocketAddress>(
                env, jni::Class<SocketAddress>::Find(env),
                utils::construct<InetSocketAddress>(env, address.into_java(),
                                                    port));
    }
};

} // namespace compat
