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

#include "./accessor_base.hpp"
#include "./optional.hpp"

#include <avsystem/commons/avs_crypto_pki.h>
#include <avsystem/commons/avs_socket.h>

#include <list>

namespace utils {
namespace detail {

struct Certificate {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjaySecurityInfoCert$Certificate";
    }
};

struct CertificateRevocationList {
    static constexpr auto Name() {
        return "com/avsystem/anjay/"
               "AnjaySecurityInfoCert$CertificateRevocationList";
    }
};

template <typename>
struct CertificateType;

template <>
struct CertificateType<avs_crypto_certificate_chain_info_t> {
    typedef Certificate Type;

    static auto
    array_to_info(const avs_crypto_certificate_chain_info_t entries[],
                  size_t size) {
        return avs_crypto_certificate_chain_info_from_array(entries, size);
    }

    static auto from_buffer(jni::jbyte data[], size_t size) {
        return avs_crypto_certificate_chain_info_from_buffer(data, size);
    }
};

template <>
struct CertificateType<avs_crypto_cert_revocation_list_info_t> {
    typedef CertificateRevocationList Type;

    static auto
    array_to_info(const avs_crypto_cert_revocation_list_info_t entries[],
                  size_t size) {
        return avs_crypto_cert_revocation_list_info_from_array(entries, size);
    }

    static auto from_buffer(jni::jbyte data[], size_t size) {
        return avs_crypto_cert_revocation_list_info_from_buffer(data, size);
    }
};

} // namespace detail

class SecurityInfoCert {
    struct PrivateKey {
        static constexpr auto Name() {
            return "com/avsystem/anjay/AnjaySecurityInfoCert$PrivateKey";
        }
    };

    struct List {
        static constexpr auto Name() {
            return "java/util/List";
        }
    };

    template <typename T>
    class CertList {
        using CertType = typename detail::CertificateType<T>::Type;
        std::list<std::vector<jni::jbyte>> storage_;
        std::vector<T> chains_;

    public:
        CertList() : storage_(), chains_() {}

        CertList(jni::JNIEnv &env, const jni::Local<jni::Object<List>> &certs)
                : CertList() {
            if (!certs.get()) {
                return;
            }
            auto list_accessor = AccessorBase<List>{ env, certs };
            const int size = list_accessor.get_method<jni::jint()>("size")();
            if (size == 0) {
                return;
            }

            class Iterator {
                jni::JNIEnv &env_;
                jni::Local<jni::Object<Iterator>> self_;

            public:
                static constexpr auto Name() {
                    return "java/util/Iterator";
                }

                Iterator(jni::JNIEnv &env, const jni::Object<Iterator> &self)
                        : env_(env), self_(jni::NewLocal(env, self)) {}

                jni::Local<jni::Object<>> next() {
                    return AccessorBase<Iterator>{ env_, self_ }
                            .template get_method<jni::Object<>()>("next")();
                }

                bool has_next() {
                    return AccessorBase<Iterator>{ env_, self_ }
                            .template get_method<jni::jboolean()>("hasNext")();
                }
            };

            auto certificate_class = jni::Class<CertType>::Find(env);

            for (auto it = Iterator{ env, list_accessor.template get_method<
                                                  jni::Object<Iterator>()>(
                                                  "iterator")() };
                 it.has_next();) {
                auto accessor = AccessorBase<CertType>{
                    env, jni::Cast(env, certificate_class, it.next())
                };

                if (auto raw_cert =
                            accessor.template get_nullable_value<jni::jbyte[]>(
                                    "rawCertificate")) {
                    storage_.emplace_back(std::move(*raw_cert));
                    chains_.emplace_back(
                            detail::CertificateType<T>::from_buffer(
                                    storage_.back().data(),
                                    storage_.back().size()));
                }
            }
        }

        CertList(const CertList &) = delete;
        CertList &operator=(const CertList &) = delete;

        CertList(CertList &&) = default;
        CertList &operator=(CertList &&) = default;

        T get_info() const {
            T result = T();
            if (chains_.size() > 0) {
                result = detail::CertificateType<T>::array_to_info(
                        chains_.data(), chains_.size());
            }
            return result;
        }
    };

    bool server_cert_validation_;
    CertList<avs_crypto_certificate_chain_info_t> trusted_certs_;
    CertList<avs_crypto_certificate_chain_info_t> client_certs_;
    CertList<avs_crypto_cert_revocation_list_info_t> cert_revocation_list_;
    std::optional<std::vector<jni::jbyte>> client_key_;
    std::optional<std::string> client_key_password_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjaySecurityInfoCert";
    }

    SecurityInfoCert(jni::JNIEnv &env,
                     const jni::Local<jni::Object<SecurityInfoCert>> &instance)
            : trusted_certs_(),
              client_certs_(),
              cert_revocation_list_(),
              client_key_() {
        auto accessor = AccessorBase<SecurityInfoCert>{ env, instance };
        trusted_certs_ = CertList<avs_crypto_certificate_chain_info_t>{
            env, accessor.get_value<jni::Object<List>>("trustedCerts")
        };
        client_certs_ = CertList<avs_crypto_certificate_chain_info_t>{
            env, accessor.get_value<jni::Object<List>>("clientCert")
        };
        cert_revocation_list_ =
                CertList<avs_crypto_cert_revocation_list_info_t>{
                    env, accessor.get_value<jni::Object<List>>(
                                 "certRevocationLists")
                };
        server_cert_validation_ =
                accessor.get_value<bool>("serverCertValidation");
        if (auto client_key =
                    accessor.get_value<jni::Object<PrivateKey>>("clientKey")) {
            auto key_accessor = AccessorBase<PrivateKey>{ env, client_key };
            client_key_ =
                    *key_accessor.get_nullable_value<jni::jbyte[]>("rawKey");

            auto password =
                    Optional{ env,
                              key_accessor.get_value<jni::Object<Optional>>(
                                      "password") };
            if (password) {
                client_key_password_ =
                        jni::Make<std::string>(env,
                                               password.get<jni::StringTag>());
            }
        }
    }

    avs_net_certificate_info_t get_info() const {
        avs_net_certificate_info_t result;
        memset(&result, 0, sizeof(result));
        result.server_cert_validation = server_cert_validation_;
        result.trusted_certs = trusted_certs_.get_info();
        result.client_cert = client_certs_.get_info();
        result.cert_revocation_lists = cert_revocation_list_.get_info();
        if (client_key_) {
            result.client_key = avs_crypto_private_key_info_from_buffer(
                    client_key_->data(), client_key_->size(),
                    client_key_password_ ? client_key_password_->c_str()
                                         : nullptr);
        }
        return result;
    }
};

} // namespace utils
