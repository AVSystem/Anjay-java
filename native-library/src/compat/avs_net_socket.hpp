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

#include <avsystem/commons/avs_errno.h>
#include <avsystem/commons/avs_socket.h>

#include "../jni_wrapper.hpp"

#include "../util_classes/selectable_channel.hpp"
#include "./socket_channel.hpp"
#include "./socket_error.hpp"

namespace compat {

class AvsSocketBase {
public:
    virtual ~AvsSocketBase() {}

    virtual jni::Local<jni::Object<utils::SelectableChannel>>
    selectable_channel() const = 0;

    virtual void connect(const char *host, const char *port) = 0;

    virtual void send(const void *buffer, size_t buffer_length) = 0;

    virtual void
    receive(size_t *out_size, void *buffer, size_t buffer_length) = 0;

    virtual void bind(const char *localaddr, const char *port) = 0;

    virtual void close() = 0;

    virtual void shutdown() = 0;

    virtual void remote_host(char *out_buffer, size_t out_buffer_size) = 0;

    virtual void remote_hostname(char *out_buffer, size_t out_buffer_size) = 0;

    virtual void remote_port(char *out_buffer, size_t out_buffer_size) = 0;

    virtual void local_host(char *out_buffer, size_t out_buffer_size) = 0;

    virtual void local_port(char *out_buffer, size_t out_buffer_size) = 0;

    virtual void get_opt(avs_net_socket_opt_key_t option_key,
                         avs_net_socket_opt_value_t *out_option_value) = 0;

    virtual void set_opt(avs_net_socket_opt_key_t option_key,
                         avs_net_socket_opt_value_t option_value) = 0;
};

template <typename ChannelType>
class AvsSocket final : public AvsSocketBase {
    SocketChannel<ChannelType> channel_;

public:
    AvsSocket(const avs_net_socket_configuration_t *config) : channel_() {
        if (config && config->reuse_addr) {
            channel_.set_reuse_address(true);
        }
    }

    virtual ~AvsSocket() {
        close();
    }

    virtual jni::Local<jni::Object<utils::SelectableChannel>>
    selectable_channel() const {
        return channel_.as_selectable_channel();
    }

    virtual void connect(const char *host, const char *port) {
        channel_.connect(host, port);
    }

    virtual void send(const void *buffer, size_t buffer_length) {
        channel_.send(buffer, buffer_length);
    }

    virtual void receive(size_t *out_size, void *buffer, size_t buffer_length) {
        channel_.timeout_respecting_receive(out_size, buffer, buffer_length);
    }

    virtual void bind(const char *localaddr, const char *port) {
        channel_.bind(localaddr, port);
    }

    virtual void close() {
        channel_.close();
    }

    virtual void shutdown() {
        channel_.shutdown();
    }

    virtual void remote_host(char *out_buffer, size_t out_buffer_size) {
        auto remote_host = channel_.get_remote_address();
        if (!remote_host) {
            avs_throw(SocketError(AVS_EBADF));
        }
        if (avs_simple_snprintf(out_buffer, out_buffer_size, "%s",
                                remote_host->get_host_address().c_str())
                < 0) {
            avs_throw(SocketError(AVS_ERANGE));
        }
    }

    virtual void remote_hostname(char *out_buffer, size_t out_buffer_size) {
        auto remote_host = channel_.get_remote_address();
        if (!remote_host) {
            avs_throw(SocketError(AVS_EBADF));
        }
        if (avs_simple_snprintf(out_buffer, out_buffer_size, "%s",
                                remote_host->get_host_name().c_str())
                < 0) {
            avs_throw(SocketError(AVS_ERANGE));
        }
    }

    virtual void remote_port(char *out_buffer, size_t out_buffer_size) {
        auto remote_port = channel_.get_remote_port();
        if (remote_port < 0) {
            avs_throw(SocketError(AVS_EBADF));
        }
        if (avs_simple_snprintf(out_buffer, out_buffer_size, "%d", remote_port)
                < 0) {
            avs_throw(SocketError(AVS_ERANGE));
        }
    }

    virtual void local_host(char *out_buffer, size_t out_buffer_size) {
        auto local_host = channel_.get_local_address();
        if (!local_host) {
            avs_throw(SocketError(AVS_EBADF));
        }
        if (avs_simple_snprintf(out_buffer, out_buffer_size, "%s",
                                local_host->get_host_address().c_str())
                < 0) {
            avs_throw(SocketError(AVS_ERANGE));
        }
    }

    virtual void local_port(char *out_buffer, size_t out_buffer_size) {
        auto local_port = channel_.get_local_port();
        if (local_port < 0) {
            avs_throw(SocketError(AVS_EBADF));
        }
        if (avs_simple_snprintf(out_buffer, out_buffer_size, "%d", local_port)
                < 0) {
            avs_throw(SocketError(AVS_ERANGE));
        }
    }

    virtual void get_opt(avs_net_socket_opt_key_t option_key,
                         avs_net_socket_opt_value_t *out_option_value) {
        switch (option_key) {
        case AVS_NET_SOCKET_OPT_STATE:
            out_option_value->state = channel_.get_state();
            break;
        case AVS_NET_SOCKET_OPT_INNER_MTU:
            out_option_value->mtu = channel_.get_inner_mtu();
            break;
        case AVS_NET_SOCKET_OPT_RECV_TIMEOUT:
            out_option_value->recv_timeout = channel_.get_timeout();
            break;
        default:
            avs_throw(SocketError(
                    AVS_EINVAL,
                    "get_opt_net: unknown or unsupported option key"));
        }
    }

    virtual void set_opt(avs_net_socket_opt_key_t option_key,
                         avs_net_socket_opt_value_t option_value) {
        switch (option_key) {
        case AVS_NET_SOCKET_OPT_RECV_TIMEOUT:
            channel_.set_timeout(option_value.recv_timeout);
            break;
        default:
            avs_throw(SocketError(
                    AVS_EINVAL,
                    "set_opt_net: unknown or unsupported option key"));
        }
    }
};

} // namespace compat
