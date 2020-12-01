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

#include <avsystem/commons/avs_commons_config.h>

#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>

#include <avsystem/commons/avs_errno_map.h>
#include <avsystem/commons/avs_log.h>
#include <avsystem/commons/avs_memory.h>
#include <avsystem/commons/avs_socket_v_table.h>
#include <avsystem/commons/avs_time.h>
#include <avsystem/commons/avs_utils.h>

#include "../jni_wrapper.hpp"

#include "./avs_net_socket.hpp"
#include "./socket_error.hpp"

#include "../util_classes/exception.hpp"

#include <stdexcept>

#define LOG(...) avs_log(net_impl, __VA_ARGS__)

struct avs_net_socket_struct {
    const avs_net_socket_v_table_t *const operations;
    avs_max_align_t impl_placeholder;
};

namespace compat {

namespace {
template <typename F>
auto call_exception_safe(const std::string &name, F &&callback) noexcept try {
    if constexpr (std::is_same<decltype(callback()), void>::value) {
        callback();
        return AVS_OK;
    } else {
        return callback();
    }
} catch (SocketError &e) {
    LOG(DEBUG, "could not perform %s: %s", name.c_str(), e.what());
    return avs_errno(e.error());
} catch (...) {
    LOG(ERROR, "could not perform %s", name.c_str());
    avs_log_and_clear_exception(ERROR);
    return avs_errno(AVS_EIO);
}
} // namespace

AvsSocketBase *get_impl(avs_net_socket_t *socket) {
    return reinterpret_cast<AvsSocketBase *>(
            &reinterpret_cast<avs_net_socket_t *>(socket)->impl_placeholder);
}

avs_error_t
connect_net(avs_net_socket_t *net_socket, const char *host, const char *port) {
    return call_exception_safe("connect()", [=]() {
        get_impl(net_socket)->connect(host, port);
    });
}

avs_error_t send_net(avs_net_socket_t *net_socket,
                     const void *buffer,
                     size_t buffer_length) {
    return call_exception_safe("send()", [=]() {
        get_impl(net_socket)->send(buffer, buffer_length);
    });
}

avs_error_t send_to_net(
        avs_net_socket_t *, const void *, size_t, const char *, const char *) {
    return avs_errno(AVS_ENOTSUP);
}

avs_error_t receive_net(avs_net_socket_t *net_socket,
                        size_t *out_size,
                        void *buffer,
                        size_t buffer_length) {
    return call_exception_safe("receive()", [=]() {
        get_impl(net_socket)->receive(out_size, buffer, buffer_length);
    });
}

avs_error_t receive_from_net(avs_net_socket_t *,
                             size_t *,
                             void *,
                             size_t,
                             char *,
                             size_t,
                             char *,
                             size_t) {
    return avs_errno(AVS_ENOTSUP);
}

avs_error_t bind_net(avs_net_socket_t *net_socket,
                     const char *localaddr,
                     const char *port) {
    return call_exception_safe("bind()", [=]() {
        get_impl(net_socket)->bind(localaddr, port);
    });
}

avs_error_t accept_net(avs_net_socket_t *, avs_net_socket_t *) {
    return avs_errno(AVS_ENOTSUP);
}

avs_error_t close_net(avs_net_socket_t *net_socket) {
    return call_exception_safe("close()",
                               [=]() { get_impl(net_socket)->close(); });
}

avs_error_t shutdown_net(avs_net_socket_t *net_socket) {
    return call_exception_safe("shutdown()", [=]() {
        avs_net_socket_opt_value_t value{};
        get_impl(net_socket)->get_opt(AVS_NET_SOCKET_OPT_STATE, &value);

        if (value.state != AVS_NET_SOCKET_STATE_CLOSED
                && value.state != AVS_NET_SOCKET_STATE_SHUTDOWN) {
            get_impl(net_socket)->shutdown();
        }
    });
}

avs_error_t cleanup_net(avs_net_socket_t **net_socket) {
    return call_exception_safe("cleanup_net()", [=]() {
        get_impl(*net_socket)->~AvsSocketBase();
        avs_free(*net_socket);
        *net_socket = NULL;
    });
}

const void *system_socket_net(avs_net_socket_t *net_socket) {
    return get_impl(net_socket);
}

avs_error_t remote_host_net(avs_net_socket_t *net_socket,
                            char *out_buffer,
                            size_t out_buffer_size) {
    return call_exception_safe("remote_host()", [=]() {
        get_impl(net_socket)->remote_host(out_buffer, out_buffer_size);
    });
}

avs_error_t remote_hostname_net(avs_net_socket_t *net_socket,
                                char *out_buffer,
                                size_t out_buffer_size) {
    return call_exception_safe("remote_hostname()", [=]() {
        get_impl(net_socket)->remote_hostname(out_buffer, out_buffer_size);
    });
}

avs_error_t remote_port_net(avs_net_socket_t *net_socket,
                            char *out_buffer,
                            size_t out_buffer_size) {
    return call_exception_safe("remote_port()", [=]() {
        get_impl(net_socket)->remote_port(out_buffer, out_buffer_size);
    });
}

avs_error_t local_host_net(avs_net_socket_t *net_socket,
                           char *out_buffer,
                           size_t out_buffer_size) {
    return call_exception_safe("local_host()", [=]() {
        get_impl(net_socket)->local_host(out_buffer, out_buffer_size);
    });
}

avs_error_t local_port_net(avs_net_socket_t *net_socket,
                           char *out_buffer,
                           size_t out_buffer_size) {
    return call_exception_safe("local_port()", [=]() {
        get_impl(net_socket)->local_port(out_buffer, out_buffer_size);
    });
}

avs_error_t get_opt_net(avs_net_socket_t *net_socket,
                        avs_net_socket_opt_key_t option_key,
                        avs_net_socket_opt_value_t *out_option_value) {
    return call_exception_safe("get_opt()", [=]() {
        get_impl(net_socket)->get_opt(option_key, out_option_value);
    });
}

avs_error_t set_opt_net(avs_net_socket_t *net_socket,
                        avs_net_socket_opt_key_t option_key,
                        avs_net_socket_opt_value_t option_value) {
    return call_exception_safe("set_opt()", [=]() {
        get_impl(net_socket)->set_opt(option_key, option_value);
    });
}

const avs_net_socket_v_table_t NET_VTABLE = ([]() {
    avs_net_socket_v_table_t res{};
    res.connect = connect_net;
    res.send = send_net;
    res.send_to = send_to_net;
    res.receive = receive_net;
    res.receive_from = receive_from_net;
    res.bind = bind_net;
    res.accept = accept_net;
    res.close = close_net;
    res.shutdown = shutdown_net;
    res.cleanup = cleanup_net;
    res.get_system_socket = system_socket_net;
    res.get_remote_host = remote_host_net;
    res.get_remote_hostname = remote_hostname_net;
    res.get_remote_port = remote_port_net;
    res.get_local_host = local_host_net;
    res.get_local_port = local_port_net;
    res.get_opt = get_opt_net;
    res.set_opt = set_opt_net;
    return res;
})();

namespace {
int ensure_supported_configuration(
        const avs_net_socket_configuration_t *config) {
    if (config->dscp) {
        LOG(ERROR, "Setting DSCP is not supported yet");
        return -1;
    }
    if (config->priority) {
        LOG(ERROR, "Setting priority is not supported yet");
        return -1;
    }
    if (config->transparent) {
        LOG(ERROR, "Setting IP_TRANSPARENT is not supported yet");
        return -1;
    }
    if (*config->interface_name) {
        LOG(ERROR, "Setting interface name is not supported yet");
        return -1;
    }
    if (config->preferred_endpoint) {
        LOG(WARNING, "Setting preferred endpoint is not supported yet");
    }
    if (config->address_family) {
        LOG(ERROR, "Setting address family is not supported yet");
        return -1;
    }
    if (config->forced_mtu) {
        LOG(ERROR, "Setting forced MTU is not supported yet");
        return -1;
    }
    if (config->preferred_family) {
        LOG(ERROR, "Setting preferred family is not supported yet");
        return -1;
    }
    return 0;
}
} // namespace

template <typename SocketType>
avs_error_t create_net_socket(avs_net_socket_t **socket,
                              const void *socket_configuration) {
    const avs_net_socket_configuration_t *configuration =
            reinterpret_cast<const avs_net_socket_configuration_t *>(
                    socket_configuration);
    if (ensure_supported_configuration(configuration)) {
        return avs_errno(AVS_EINVAL);
    }
    const size_t size =
            offsetof(avs_net_socket_t, impl_placeholder) + sizeof(SocketType);

    std::unique_ptr<avs_net_socket_t, decltype(&avs_free)> net_socket(
            reinterpret_cast<avs_net_socket_t *>(avs_calloc(1, size)),
            avs_free);
    if (!net_socket) {
        return avs_errno(AVS_ENOMEM);
    }
    const_cast<const avs_net_socket_v_table_t *&>(net_socket->operations) =
            &NET_VTABLE;

    SocketType *ptr =
            reinterpret_cast<SocketType *>(&net_socket->impl_placeholder);
    assert(static_cast<AvsSocketBase *>(ptr) == get_impl(net_socket.get()));
    new (ptr) SocketType(configuration);

    *socket = net_socket.release();
    return AVS_OK;
}

} // namespace compat

extern "C" {

avs_error_t _avs_net_create_tcp_socket(avs_net_socket_t **socket,
                                       const void *socket_configuration) {
    return compat::call_exception_safe("_avs_net_create_tcp_socket()", [=]() {
        return compat::create_net_socket<
                compat::AvsSocket<compat::TcpChannelTag>>(socket,
                                                          socket_configuration);
    });
}

avs_error_t _avs_net_create_udp_socket(avs_net_socket_t **socket,
                                       const void *socket_configuration) {
    return compat::call_exception_safe("_avs_net_create_udp_socket()", [=]() {
        return compat::create_net_socket<
                compat::AvsSocket<compat::UdpChannelTag>>(socket,
                                                          socket_configuration);
    });
}

avs_error_t _avs_net_initialize_global_compat_state(void) {
    return AVS_OK;
}

void _avs_net_cleanup_global_compat_state(void) {}

avs_net_addrinfo_t *
avs_net_addrinfo_resolve_ex(avs_net_socket_type_t,
                            avs_net_af_t,
                            const char *,
                            const char *,
                            int,
                            const avs_net_resolved_endpoint_t *) {
    AVS_UNREACHABLE("This should not be called");
    return NULL;
}

int avs_net_addrinfo_next(avs_net_addrinfo_t *, avs_net_resolved_endpoint_t *) {
    AVS_UNREACHABLE("This should not be called");
    return -1;
}

void avs_net_addrinfo_rewind(avs_net_addrinfo_t *) {
    AVS_UNREACHABLE("This should not be called");
}

void avs_net_addrinfo_delete(avs_net_addrinfo_t **) {
    AVS_UNREACHABLE("This should not be called");
}

avs_error_t avs_net_resolved_endpoint_get_host_port(
        const avs_net_resolved_endpoint_t *, char *, size_t, char *, size_t) {
    AVS_UNREACHABLE("This should not be called");
    return avs_errno(AVS_ENOTSUP);
}

avs_error_t avs_net_local_address_for_target_host(const char *,
                                                  avs_net_af_t,
                                                  char *,
                                                  size_t) {
    AVS_UNREACHABLE("This should not be called");
    return avs_errno(AVS_ENOTSUP);
}

#ifdef AVS_COMMONS_NET_WITH_IPV4
#    define IPV4_AVAILABLE 1
#else
#    define IPV4_AVAILABLE 0
#endif

#ifdef AVS_COMMONS_NET_WITH_IPV6
#    define IPV6_AVAILABLE 1
#else
#    define IPV6_AVAILABLE 0
#endif

} // extern "C"
