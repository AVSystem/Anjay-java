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

#include <anjay/core.h>

#include "../jni_wrapper.hpp"

#include "./construct.hpp"
#include "./selectable_channel.hpp"
#include "./transport.hpp"

#include "../compat/avs_net_socket.hpp"

namespace utils {

struct NativeSocketEntry {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeSocketEntry";
    }

    static jni::Local<jni::Object<NativeSocketEntry>>
    New(jni::JNIEnv &env, const anjay_socket_entry_t *entry) {
        const compat::AvsSocketBase &backend =
                *reinterpret_cast<const compat::AvsSocketBase *>(
                        avs_net_socket_get_system(entry->socket));

        return construct<NativeSocketEntry>(
                env, utils::Transport::New(env, entry->transport),
                backend.selectable_channel(),
                reinterpret_cast<jni::jlong>(entry->socket),
                static_cast<jni::jint>(entry->ssid),
                static_cast<jni::jboolean>(entry->queue_mode));
    }
};

} // namespace utils
