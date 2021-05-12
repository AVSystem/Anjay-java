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

#include <anjay/anjay.h>

#include "./accessor_base.hpp"
#include "./exception.hpp"

namespace utils {

struct Objlnk {
    anjay_oid_t oid;
    anjay_iid_t iid;

    static constexpr auto Name() {
        return "com/avsystem/anjay/Anjay$Objlnk";
    }

    jni::Local<jni::Object<Objlnk>> into_object(jni::JNIEnv &env) const {
        auto clazz = jni::Class<Objlnk>::Find(env);
        auto ctor = clazz.GetConstructor<jni::jint, jni::jint>(env);
        return clazz.New(env, ctor, oid, iid);
    }

    static Objlnk into_native(jni::JNIEnv &env,
                              const jni::Object<Objlnk> &instance) {
        auto accessor = AccessorBase<Objlnk>{ env, instance };
        auto oid = accessor.get_value<int>("oid");
        if (oid < 0 || oid > std::numeric_limits<anjay_oid_t>::max()) {
            avs_throw(IllegalArgumentException(env, "oid out of range"));
        }
        auto iid = accessor.get_value<int>("iid");
        if (iid < 0 || iid > std::numeric_limits<anjay_oid_t>::max()) {
            avs_throw(IllegalArgumentException(env, "iid out of range"));
        }
        return Objlnk{ static_cast<anjay_oid_t>(oid),
                       static_cast<anjay_iid_t>(iid) };
    }
};

} // namespace utils
