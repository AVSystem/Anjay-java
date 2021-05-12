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

#include <unordered_map>

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"

#include <anjay/download.h>

namespace utils {

struct DownloadResultDetails {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayDownload$ResultDetails";
    }

    static jni::Local<jni::Object<DownloadResultDetails>>
    into_java(jni::JNIEnv &env, avs_error_t details) {
        if (details.category != AVS_ERRNO_CATEGORY) {
            return from_string(env, "UNKNOWN");
        }

        static std::unordered_map<uint16_t, std::string> MAPPING{
            { AVS_EADDRNOTAVAIL, "DNS_RESOLUTION_FAILED" },
            { AVS_ECONNABORTED, "REMOTE_RESOURCE_NO_LONGER_VALID" },
            { AVS_ECONNREFUSED, "SERVER_RESPONDED_WITH_RESET" },
            { AVS_ECONNRESET, "CONNECTION_LOST" },
            { AVS_EINVAL, "FAILED_TO_PARSE_RESPONSE" },
            { AVS_EIO, "INTERNAL_ERROR" },
            { AVS_EMSGSIZE, "MESSAGE_TOO_LARGE" },
            { AVS_ENOMEM, "OUT_OF_MEMORY" },
            { AVS_ETIMEDOUT, "TIMEOUT" },
        };

        auto mapped_to = MAPPING.find(details.code);
        if (mapped_to == MAPPING.end()) {
            return from_string(env, "UNKNOWN");
        }

        return from_string(env, mapped_to->second);
    }

    static jni::Local<jni::Object<DownloadResultDetails>>
    into_java(jni::JNIEnv &env, int details) {
        static std::unordered_map<int, std::string> MAPPING{
            { 132, "NOT_FOUND" },
            { 161, "NOT_IMPLEMENTED" },
            { 404, "NOT_FOUND" },
            { 501, "NOT_IMPLEMENTED" }
        };

        auto mapped_to = MAPPING.find(details);
        if (mapped_to == MAPPING.end()) {
            return from_string(env, "UNKNOWN");
        }
        return from_string(env, mapped_to->second);
    }

private:
    static jni::Local<jni::Object<DownloadResultDetails>>
    from_string(jni::JNIEnv &env, const std::string &name) {
        auto clazz = jni::Class<DownloadResultDetails>::Find(env);
        return clazz.Get(
                env, clazz.GetStaticField<jni::Object<DownloadResultDetails>>(
                             env, name.c_str()));
    }
};

} // namespace utils
