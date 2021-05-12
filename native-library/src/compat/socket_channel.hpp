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

#include <avsystem/commons/avs_socket.h>
#include <avsystem/commons/avs_time.h>

#include "../global_context.hpp"
#include "../util_classes/accessor_base.hpp"
#include "../util_classes/byte_buffer.hpp"
#include "../util_classes/exception.hpp"
#include "../util_classes/native_utils.hpp"
#include "../util_classes/selectable_channel.hpp"

#include "./socket.hpp"
#include "./socket_address.hpp"
#include "./socket_error.hpp"

#include <optional>

namespace compat {

struct TcpChannelTag {
    typedef TcpSocketTag SocketTag;

    static constexpr auto Name() {
        return "java/nio/channels/SocketChannel";
    }
};

struct UdpChannelTag {
    typedef UdpSocketTag SocketTag;

    static constexpr auto Name() {
        return "java/nio/channels/DatagramChannel";
    }
};

template <typename ChannelTag>
class SocketChannel {
    jni::JNIEnv &env_;
    jni::Global<jni::Object<ChannelTag>> self_;
    avs_time_duration_t timeout_;
    bool is_shutdown_;

    auto accessor() {
        return utils::AccessorBase<ChannelTag>{ env_, self_ };
    }

    auto socket() {
        return Socket<typename ChannelTag::SocketTag>(
                env_,
                accessor()
                        .template get_method<
                                jni::Object<typename ChannelTag::SocketTag>()>(
                                "socket")());
    }

    void configure_blocking(bool on) {
        accessor()
                .template get_method<jni::Object<utils::SelectableChannel>(
                        jni::jboolean)>("configureBlocking")(on);
    }

    auto try_connect(const InetAddress &address, int port) {
        auto resolved_address =
                InetSocketAddress::from_resolved(env_, address, port);
        if constexpr (std::is_same<ChannelTag, UdpChannelTag>::value) {
            accessor()
                    .template get_method<jni::Object<ChannelTag>(
                            jni::Object<SocketAddress>)>("connect")(
                            resolved_address);

            return AVS_NO_ERROR;
        }

        accessor()
                .template get_method<jni::jboolean(jni::Object<SocketAddress>)>(
                        "connect")(resolved_address);
        utils::NativeUtils::ReadyState wait_state{};
        wait_state.connect = true;
        if (!utils::NativeUtils::wait_until_ready(env_, as_selectable_channel(),
                                                  NET_CONNECT_TIMEOUT,
                                                  wait_state)
                     .connect) {
            return AVS_ETIMEDOUT;
        }
        // NOTE: finishConnect() may throw an exception on Java side.
        try {
            if (accessor().template get_method<jni::jboolean()>(
                        "finishConnect")()) {
                return AVS_NO_ERROR;
            } else {
                // This probably shouldn't even happen, because
                // wait_until_ready() should have timed-out, but let's keep
                // things complete.
                return AVS_ETIMEDOUT;
            }
        } catch (jni::PendingJavaException &) {
            avs_log_and_clear_exception(DEBUG);
            // Probably connection refused.
            return AVS_ECONNREFUSED;
        }
    }

    void create() {
        self_ = jni::NewGlobal(
                env_,
                utils::AccessorBase<ChannelTag>::template get_static_method<
                        jni::Object<ChannelTag>()>(env_, "open")());
        is_shutdown_ = false;
        configure_blocking(false);
    }

    void recreate_if_required() {
        if (socket().is_closed()) {
            create();
        }
    }

    static constexpr avs_time_duration_t NET_CONNECT_TIMEOUT{ 10, 0 };
    static constexpr avs_time_duration_t NET_SEND_TIMEOUT{ 30, 0 };

public:
    SocketChannel()
            : env_(*GlobalContext::call_with_env([](auto &&env) {
                  // This looks insane, however, we can be relatively sure that
                  // the socket is created from the same thread Anjay operates.
                  // Otherwise the behavior is undefined anyway, so we may as
                  // well get this global and store it through the entire
                  // datagram lifetime.
                  return env.get();
              })),
              self_(),
              timeout_(AVS_NET_SOCKET_DEFAULT_RECV_TIMEOUT),
              is_shutdown_() {
        create();
    }

    jni::Local<jni::Object<utils::SelectableChannel>>
    as_selectable_channel() const {
        return jni::Cast<utils::SelectableChannel>(
                env_, jni::Class<utils::SelectableChannel>::Find(env_), self_);
    }

    void close() {
        accessor().template get_method<void()>("close")();
    }

    void connect(const char *host, const char *port) {
        if (!host || !port) {
            avs_throw(SocketError(AVS_EINVAL, "host & port MUST NOT be NULL"));
        }

        recreate_if_required();
        avs_errno_t error = AVS_EHOSTUNREACH;
        for (const InetAddress &addr :
             InetAddress::get_all_by_name(env_, host)) {
            if (!(error = try_connect(addr, std::stoi(port)))) {
                return;
            }
        }
        avs_throw(SocketError(error, "could not connect()"));
    }

    void send(const void *buffer, size_t buffer_length) {
        if (!socket().is_connected()) {
            avs_throw(SocketError(AVS_ENOTCONN,
                                  "Cannot send() on unconnected socket"));
        }
        const avs_time_monotonic_t deadline =
                avs_time_monotonic_add(avs_time_monotonic_now(),
                                       NET_SEND_TIMEOUT);
        utils::BufferView byte_buffer{ env_, const_cast<void *>(buffer),
                                       buffer_length };
        size_t sent_so_far = 0;

        auto try_send_next_chunk = [&]() {
            utils::NativeUtils::ReadyState wait_state{};
            wait_state.write = true;

            avs_time_duration_t timeout =
                    avs_time_monotonic_diff(deadline, avs_time_monotonic_now());
            if (avs_time_duration_less(timeout, AVS_TIME_DURATION_ZERO)) {
                timeout = AVS_TIME_DURATION_ZERO;
            }
            if (utils::NativeUtils::wait_until_ready(
                        env_, as_selectable_channel(), timeout, wait_state)
                        .write) {
                return accessor()
                        .template get_method<jni::jint(
                                jni::Object<utils::ByteBuffer>)>("write")(
                                byte_buffer.into_java());
            }
            return 0;
        };

        do {
            try {
                sent_so_far += try_send_next_chunk();
            } catch (jni::PendingJavaException &) {
                // Really hard to tell what went wrong, but since the timeout is
                // handled and did not yet occurr, then the underlying problem
                // must be serious.
                avs_log_and_clear_exception(DEBUG);
                avs_throw(SocketError(AVS_ECONNABORTED));
            }
            if constexpr (std::is_same<ChannelTag, UdpChannelTag>::value) {
                if (sent_so_far < buffer_length) {
                    avs_throw(SocketError(AVS_EIO, "send() fail"));
                }
            }
        } while (sent_so_far < buffer_length
                 && avs_time_monotonic_before(avs_time_monotonic_now(),
                                              deadline));

        if (sent_so_far < buffer_length) {
            avs_throw(SocketError(AVS_ETIMEDOUT, "timeout while send()"));
        }
    }

    void timeout_respecting_receive(size_t *out_size,
                                    void *buffer,
                                    size_t buffer_length) {
        if (!socket().is_connected()) {
            avs_throw(SocketError(AVS_ENOTCONN,
                                  "Cannot receive() from unconnected socket"));
        }
        utils::NativeUtils::ReadyState wait_state{};
        wait_state.read = true;

        if (!utils::NativeUtils::wait_until_ready(env_, as_selectable_channel(),
                                                  timeout_, wait_state)
                     .read) {
            avs_throw(SocketError(AVS_ETIMEDOUT));
        }
        utils::BufferView byte_buffer{ env_, buffer, buffer_length };
        try {
            int read = accessor()
                               .template get_method<jni::jint(
                                       jni::Object<utils::ByteBuffer>)>("read")(
                                       byte_buffer.into_java());
            // -1 is EOF
            *out_size = std::max(0, read);
        } catch (jni::PendingJavaException &) {
            // Probably the connection is lost.
            avs_log_and_clear_exception(DEBUG);
            avs_throw(SocketError(AVS_ECONNREFUSED));
        }
    }

    void bind(const char *localaddr, const char *port) {
        recreate_if_required();

        if (localaddr && *localaddr) {
            accessor()
                    .template get_method<jni::Object<ChannelTag>(
                            jni::Object<SocketAddress>)>("bind")(
                            InetSocketAddress::from_host_port(env_, localaddr,
                                                              port));
        } else {
            accessor()
                    .template get_method<jni::Object<ChannelTag>(
                            jni::Object<SocketAddress>)>("bind")(
                            InetSocketAddress::from_port(env_, port));
        }
    }

    void set_timeout(avs_time_duration_t duration) {
        if (!avs_time_duration_valid(timeout_)) {
            avs_throw(SocketError(
                    AVS_EINVAL,
                    "Timeout cannot be set to an invalid duration"));
        }
        timeout_ = duration;
    }

    avs_time_duration_t get_timeout() {
        return timeout_;
    }

    void set_reuse_address(bool on) {
        return socket().set_reuse_address(on);
    }

    avs_net_socket_state_t get_state() {
        auto s = socket();
        if (s.is_closed()) {
            return AVS_NET_SOCKET_STATE_CLOSED;
        }
        if (is_shutdown_) {
            return AVS_NET_SOCKET_STATE_SHUTDOWN;
        }
        if (s.is_connected()) {
            return AVS_NET_SOCKET_STATE_CONNECTED;
        }
        if (s.is_bound()) {
            return AVS_NET_SOCKET_STATE_BOUND;
        }
        // The socket is not closed, not shutdown, not connected, and not bound,
        // which means it is in initial state.
        return AVS_NET_SOCKET_STATE_CLOSED;
    }

    std::optional<InetAddress> get_remote_address() {
        return socket().get_remote_address();
    }

    std::optional<InetAddress> get_local_address() {
        return socket().get_local_address();
    }

    int get_local_port() {
        return socket().get_local_port();
    }

    int get_remote_port() {
        return socket().get_remote_port();
    }

    int get_inner_mtu() {
        if constexpr (std::is_same<ChannelTag, TcpChannelTag>::value) {
            avs_throw(SocketError(
                    AVS_ENOTSUP,
                    "Getting inner MTU for TCP sockets is not supported"));
        }

        auto remote = get_remote_address();
        if (!remote) {
            avs_throw(SocketError(AVS_EIO, "could not get remote address"));
        }
        if (remote->is_ipv4()) {
            return 548; /* 576 - (20 for IP + 8 for UDP) */
        } else {
            return 1232; /* 1280 - (40 for IPv6 + 8 for UDP) */
        }
    }

    void shutdown() {
        if constexpr (std::is_same<ChannelTag, TcpChannelTag>::value) {
            is_shutdown_ = true;
            accessor().template get_method<jni::Object<ChannelTag>()>(
                    "shutdownInput")();
            accessor().template get_method<jni::Object<ChannelTag>()>(
                    "shutdownOutput")();
        } else {
            // UDP sockets in Java do not have shutdown()... so close()
            close();
        }
    }
};

} // namespace compat
