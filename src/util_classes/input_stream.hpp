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

#include <avsystem/commons/avs_stream_simple_io.h>

#include <functional>

namespace utils {

class InputStream {
    static int reader(void *arg, void *buffer, size_t *inout_size) try {
        return GlobalContext::call_with_env([=](jni::UniqueEnv &&env) {
            jni::Object<InputStream> *input_stream =
                    static_cast<jni::Object<InputStream> *>(arg);
            auto arr = jni::Array<jni::jbyte>::New(*env, *inout_size);
            int bytes_read =
                    utils::AccessorBase<utils::InputStream>{ *env,
                                                             *input_stream }
                            .get_method<jni::jint(jni::Array<jni::jbyte>,
                                                  jni::jint, jni::jint)>(
                                    "read")(arr, 0, *inout_size);
            if (bytes_read < 0) {
                *inout_size = 0;
            } else {
                assert(*inout_size >= static_cast<size_t>(bytes_read));
                *inout_size = bytes_read;
                auto vec = jni::Make<std::vector<jni::jbyte>>(*env, arr);
                memcpy(buffer, vec.data(), *inout_size);
            }
            return 0;
        });
    } catch (...) {
        avs_log_and_clear_exception(DEBUG);
        return -1;
    }

public:
    static constexpr auto Name() {
        return "java/io/InputStream";
    }

    static std::unique_ptr<avs_stream_t, std::function<void(avs_stream_t *)>>
    get_avs_stream(jni::Object<InputStream> *input_stream) {
        return std::unique_ptr<avs_stream_t,
                               std::function<void(avs_stream_t *)>>(
                avs_stream_simple_input_create(reader, input_stream),
                [](avs_stream_t *stream) { avs_stream_cleanup(&stream); });
    }
};

} // namespace utils
