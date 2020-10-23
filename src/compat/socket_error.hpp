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

#include <stdexcept>
#include <string>

#include <avsystem/commons/avs_errno.h>

namespace compat {

struct SocketError : public std::exception {
    avs_errno_t error_;
    std::string description_;

public:
    SocketError(avs_errno_t error, const std::string &description = {})
            : error_(error), description_(description) {}

    virtual ~SocketError() {}

    virtual const char *what() const noexcept {
        return description_.c_str();
    }

    avs_errno_t error() const noexcept {
        return error_;
    }
};

} // namespace compat
