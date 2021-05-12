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

#include "./accessor_base.hpp"

#include <vector>

namespace utils {

class SecurityInfoPsk {
    std::vector<jni::jbyte> identity_;
    std::vector<jni::jbyte> key_;

public:
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjaySecurityInfoPsk";
    }

    SecurityInfoPsk(jni::JNIEnv &env,
                    const jni::Local<jni::Object<SecurityInfoPsk>> &instance)
            : identity_(), key_() {
        auto accessor = AccessorBase<SecurityInfoPsk>{ env, instance };
        auto identity = accessor.get_value<jni::Array<jni::jbyte>>("identity");
        identity_.resize(identity.Length(env));
        identity.GetRegion(env, 0, identity_);

        auto key = accessor.get_value<jni::Array<jni::jbyte>>("key");
        key_.resize(key.Length(env));
        key.GetRegion(env, 0, key_);
    }

    SecurityInfoPsk(const SecurityInfoPsk &) = delete;
    SecurityInfoPsk &operator=(const SecurityInfoPsk &) = delete;

    SecurityInfoPsk(SecurityInfoPsk &&) = default;
    SecurityInfoPsk &operator=(SecurityInfoPsk &&) = default;

    avs_net_psk_info_t get_info() const {
        avs_net_psk_info_t info{};
        info.identity = identity_.data();
        info.identity_size = identity_.size();
        info.psk = key_.data();
        info.psk_size = key_.size();
        return info;
    }
};

} // namespace utils
