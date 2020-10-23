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

#include <anjay/anjay.h>

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"
#include "./resource_kind.hpp"

#include <vector>

namespace utils {

struct ResourceDef {
    const anjay_rid_t rid;
    const anjay_dm_resource_kind_t kind;
    const bool present;

    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayObject$ResourceDef";
    }

    static ResourceDef into_native(jni::JNIEnv &env,
                                   const jni::Object<ResourceDef> &instance) {
        auto accessor = AccessorBase<ResourceDef>{ env, instance };
        return ResourceDef{
            static_cast<anjay_rid_t>(accessor.get_value<int>("rid")),
            ResourceKind::into_native(
                    env,
                    accessor.get_value<jni::Object<utils::ResourceKind>>(
                            "kind")),
            accessor.get_value<bool>("present")
        };
    }

private:
    ResourceDef(anjay_rid_t rid, anjay_dm_resource_kind_t kind, bool present)
            : rid(rid), kind(kind), present(present) {}
};

} // namespace utils
