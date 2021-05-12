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

#include <anjay/dm.h>

#include "./accessor_base.hpp"

namespace utils {

struct ObjectInstanceAttrs {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayAttributes$ObjectInstanceAttrs";
    }

    static anjay_dm_oi_attributes_t
    into_native(jni::JNIEnv &env,
                const jni::Object<ObjectInstanceAttrs> &attrs) {
        anjay_dm_oi_attributes_t native_attrs;
        auto accessor = AccessorBase<ObjectInstanceAttrs>{ env, attrs };

        native_attrs.min_period = accessor.get_value<int>("minPeriod");
        native_attrs.max_period = accessor.get_value<int>("maxPeriod");
        native_attrs.min_eval_period = accessor.get_value<int>("minEvalPeriod");
        native_attrs.max_eval_period = accessor.get_value<int>("maxEvalPeriod");

        return native_attrs;
    }

    static jni::Local<jni::Object<ObjectInstanceAttrs>>
    into_java(jni::JNIEnv &env, const anjay_dm_oi_attributes_t *attrs) {
        auto clazz = jni::Class<ObjectInstanceAttrs>::Find(env);
        auto ctor = clazz.GetConstructor<jni::jint, jni::jint, jni::jint,
                                         jni::jint>(env);
        return clazz.New(env, ctor, attrs->min_period, attrs->max_period,
                         attrs->min_eval_period, attrs->max_eval_period);
    }
};

struct ObjectInstanceAttrsByReference {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/"
               "NativeAnjayObject$ObjectInstanceAttrsByReference";
    }

    static jni::Local<jni::Object<ObjectInstanceAttrsByReference>>
    New(jni::JNIEnv &env) {
        auto clazz = jni::Class<ObjectInstanceAttrsByReference>::Find(env);
        auto ctor = clazz.GetConstructor(env);
        return clazz.New(env, ctor);
    }

    static jni::Local<jni::Object<ObjectInstanceAttrs>>
    get_value(jni::JNIEnv &env,
              const jni::Object<ObjectInstanceAttrsByReference> &instance) {
        auto accessor =
                AccessorBase<ObjectInstanceAttrsByReference>{ env, instance };
        return accessor.get_value<jni::Object<ObjectInstanceAttrs>>("value");
    }
};

struct ResourceAttrs {
    static constexpr auto Name() {
        return "com/avsystem/anjay/AnjayAttributes$ResourceAttrs";
    }

    static anjay_dm_r_attributes_t
    into_native(jni::JNIEnv &env, const jni::Object<ResourceAttrs> &attrs) {
        anjay_dm_r_attributes_t native_attrs;
        auto accessor = AccessorBase<ResourceAttrs>{ env, attrs };

        native_attrs.common = ObjectInstanceAttrs::into_native(
                env,
                accessor.get_value<jni::Object<ObjectInstanceAttrs>>("common"));
        native_attrs.greater_than = accessor.get_value<double>("greaterThan");
        native_attrs.less_than = accessor.get_value<double>("lessThan");
        native_attrs.step = accessor.get_value<double>("step");

        return native_attrs;
    }

    static jni::Local<jni::Object<ResourceAttrs>>
    into_java(jni::JNIEnv &env, const anjay_dm_r_attributes_t *attrs) {
        auto clazz = jni::Class<ResourceAttrs>::Find(env);
        auto ctor =
                clazz.GetConstructor<jni::Object<ObjectInstanceAttrs>,
                                     jni::jdouble, jni::jdouble, jni::jdouble>(
                        env);
        return clazz.New(env, ctor,
                         ObjectInstanceAttrs::into_java(env, &attrs->common),
                         attrs->greater_than, attrs->less_than, attrs->step);
    }
};

struct ResourceAttrsByReference {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/"
               "NativeAnjayObject$ResourceAttrsByReference";
    }

    static jni::Local<jni::Object<ResourceAttrsByReference>>
    New(jni::JNIEnv &env) {
        auto clazz = jni::Class<ResourceAttrsByReference>::Find(env);
        auto ctor = clazz.GetConstructor(env);
        return clazz.New(env, ctor);
    }

    static jni::Local<jni::Object<ResourceAttrs>>
    get_value(jni::JNIEnv &env,
              const jni::Object<ResourceAttrsByReference> &instance) {
        auto accessor = AccessorBase<ResourceAttrsByReference>{ env, instance };
        return accessor.get_value<jni::Object<ResourceAttrs>>("value");
    }
};

} // namespace utils
