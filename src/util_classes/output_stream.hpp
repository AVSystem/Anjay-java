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

class OutputStream {
    static int writer(void *arg, const void *buffer, size_t *inout_size) try {
        return GlobalContext::call_with_env([=](jni::UniqueEnv &&env) {
            jni::Object<OutputStream> *output_stream =
                    static_cast<jni::Object<OutputStream> *>(arg);
            std::vector<jni::jbyte> data_vec((const uint8_t *) buffer,
                                             (const uint8_t *) buffer
                                                     + *inout_size);
            utils::AccessorBase<OutputStream>{ *env, *output_stream }
                    .get_method<void(jni::Array<jni::jbyte>)>("write")(
                            jni::Make<jni::Array<jni::jbyte>>(*env, data_vec));
            return 0;
        });
    } catch (...) {
        avs_log_and_clear_exception(DEBUG);
        return -1;
    }

public:
    static constexpr auto Name() {
        return "java/io/OutputStream";
    }

    static std::unique_ptr<avs_stream_t, std::function<void(avs_stream_t *)>>
    get_avs_stream(jni::Object<OutputStream> *output_stream) {
        return std::unique_ptr<avs_stream_t,
                               std::function<void(avs_stream_t *)>>(
                avs_stream_simple_output_create(writer, output_stream),
                [](avs_stream_t *stream) { avs_stream_cleanup(&stream); });
    }
};

} // namespace utils
