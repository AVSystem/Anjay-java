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

#include <avsystem/commons/avs_time.h>

#include <optional>

#include "../util_classes/accessor_base.hpp"
#include "./socket_address.hpp"

namespace compat {

struct TcpSocketTag {
    static constexpr auto Name() {
        return "java/net/Socket";
    }
};

struct UdpSocketTag {
    static constexpr auto Name() {
        return "java/net/DatagramSocket";
    }
};

template <typename SocketTag>
class Socket {
    jni::JNIEnv &env_;
    jni::Global<jni::Object<SocketTag>> self_;

    auto accessor() {
        return utils::AccessorBase<SocketTag>{ env_, self_ };
    }

public:
    Socket(jni::JNIEnv &env, const jni::Local<jni::Object<SocketTag>> &socket)
            : env_(env), self_(jni::NewGlobal(env_, socket)) {}

    void set_reuse_address(bool on) {
        accessor().template get_method<void(jni::jboolean)>("setReuseAddress")(
                on);
    }

    bool is_bound() {
        return accessor().template get_method<jni::jboolean()>("isBound")();
    }

    bool is_closed() {
        return accessor().template get_method<jni::jboolean()>("isClosed")();
    }

    bool is_connected() {
        return accessor().template get_method<jni::jboolean()>("isConnected")();
    }

    jni::Local<jni::Object<SocketAddress>> get_remote_socket_address() {
        return accessor().template get_method<jni::Object<SocketAddress>()>(
                "getRemoteSocketAddress")();
    }

    std::optional<InetAddress> get_remote_address() {
        auto value = accessor().template get_method<jni::Object<InetAddress>()>(
                "getInetAddress")();
        if (!value) {
            return {};
        }
        return { InetAddress{ env_, value } };
    }

    std::optional<InetAddress> get_local_address() {
        auto value = accessor().template get_method<jni::Object<InetAddress>()>(
                "getLocalAddress")();
        if (!value) {
            return {};
        }
        return { InetAddress{ env_, value } };
    }

    int get_local_port() {
        return accessor().template get_method<jni::jint()>("getLocalPort")();
    }

    int get_remote_port() {
        return accessor().template get_method<jni::jint()>("getPort")();
    }
};

} // namespace compat
