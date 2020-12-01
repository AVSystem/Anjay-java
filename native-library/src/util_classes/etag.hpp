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

#include <vector>

#include "../jni_wrapper.hpp"

#include <anjay/download.h>

namespace utils {

struct Etag {
    anjay_etag_t *data;

    Etag(const std::vector<jni::jbyte> &etag) {
        uint8_t size = static_cast<uint8_t>(etag.size());
        if (size != etag.size()) {
            avs_throw(std::runtime_error("etag too long"));
        }
        data = anjay_etag_new(size);
        if (!data) {
            avs_throw(std::runtime_error("out of memory"));
        }
        memcpy(data->value, etag.data(), size);
    }

    ~Etag() {
        avs_free(data);
    }

    Etag(const Etag &) = delete;
    Etag &operator=(const Etag &) = delete;
};

} // namespace utils
