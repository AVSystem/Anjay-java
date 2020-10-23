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

#include <unordered_map>

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"

#include <anjay/download.h>

namespace utils {

struct DownloadResult {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayDownload$Result";
    }

    static jni::Local<jni::Object<DownloadResult>>
    into_java(jni::JNIEnv &env, anjay_download_result_t result) {
        auto clazz = jni::Class<DownloadResult>::Find(env);
        auto get_enum_instance = [&](const std::string &name) {
            return clazz.Get(env,
                             clazz.GetStaticField<jni::Object<DownloadResult>>(
                                     env, name.c_str()));
        };
        static std::unordered_map<anjay_download_result_t, std::string> MAPPING{
            { ANJAY_DOWNLOAD_FINISHED, "FINISHED" },
            { ANJAY_DOWNLOAD_ERR_FAILED, "FAILED" },
            { ANJAY_DOWNLOAD_ERR_INVALID_RESPONSE, "INVALID_RESPONSE" },
            { ANJAY_DOWNLOAD_ERR_EXPIRED, "EXPIRED" },
            { ANJAY_DOWNLOAD_ERR_ABORTED, "ABORTED" }
        };
        return get_enum_instance(MAPPING[result]);
    }
};

} // namespace utils
