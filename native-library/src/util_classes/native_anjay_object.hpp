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

#include "./accessor_base.hpp"
#include "./attributes.hpp"
#include "./exception.hpp"
#include "./hash_map.hpp"
#include "./integer_array_by_reference.hpp"
#include "./map.hpp"
#include "./native_input_context_pointer.hpp"
#include "./native_output_context_pointer.hpp"
#include "./optional.hpp"
#include "./resource_def.hpp"
#include "./resource_def_array_by_reference.hpp"

#include <map>
#include <optional>
#include <vector>

namespace utils {

struct NativeAnjayObject {
    static constexpr auto Name() {
        return "com/avsystem/anjay/impl/NativeAnjayObject";
    }

    class Accessor : public AccessorBase<NativeAnjayObject> {
    public:
        explicit Accessor(jni::JNIEnv &env,
                          const jni::Object<NativeAnjayObject> &instance)
                : AccessorBase(env, instance) {}

        anjay_oid_t get_oid() {
            return get_method<jni::jint()>("oid")();
        }

        std::string get_version() {
            return jni::Make<std::string>(env_, get_method<jni::String()>(
                                                        "version")());
        }

        template <typename Func>
        int for_each_instance(Func &&func) {
            auto array_by_ref = IntegerArrayByReference::New(env_);
            int result =
                    get_method<jni::jint(jni::Object<IntegerArrayByReference>)>(
                            "instances")(array_by_ref);
            if (result) {
                return result;
            }
            IntegerArrayByReference::for_each(env_, array_by_ref, [&](int iid) {
                if (iid < std::numeric_limits<anjay_iid_t>::min()
                        || iid > std::numeric_limits<anjay_iid_t>::max()) {
                    avs_throw(std::runtime_error("iid out of range: "
                                                 + std::to_string(iid)));
                }
                func(static_cast<anjay_iid_t>(iid));
            });
            return 0;
        }

        template <typename Func>
        int for_each_resource_instance(anjay_iid_t iid,
                                       anjay_rid_t rid,
                                       Func &&func) {
            auto array_by_ref = IntegerArrayByReference::New(env_);
            int result =
                    get_method<jni::jint(jni::jint, jni::jint,
                                         jni::Object<IntegerArrayByReference>)>(
                            "resourceInstances")(iid, rid, array_by_ref);
            if (result) {
                return result;
            }
            IntegerArrayByReference::for_each(
                    env_, array_by_ref, [&](int riid) {
                        if (riid < std::numeric_limits<anjay_riid_t>::min()
                                || riid > std::numeric_limits<
                                                  anjay_riid_t>::max()) {
                            avs_throw(
                                    std::runtime_error("riid out of range: "
                                                       + std::to_string(riid)));
                        }
                        func(static_cast<anjay_riid_t>(riid));
                    });
            return 0;
        }

        template <typename Func>
        int for_each_resource(anjay_iid_t iid, Func &&func) {
            auto array_by_ref = ResourceDefArrayByReference::New(env_);
            int result = get_method<jni::jint(
                    jni::jint, jni::Object<ResourceDefArrayByReference>)>(
                    "resources")(iid, array_by_ref);
            if (result) {
                return result;
            }
            ResourceDefArrayByReference::for_each(env_, array_by_ref, func);
            return 0;
        }

        int resource_read(anjay_iid_t iid,
                          anjay_rid_t rid,
                          anjay_riid_t riid,
                          anjay_output_ctx_t *ctx) {
            return get_method<jni::jint(
                    jni::jint, jni::jint, jni::jint,
                    jni::Object<utils::NativeOutputContextPointer>)>(
                    "resourceRead")(
                    iid, rid, riid,
                    utils::NativeOutputContextPointer::into_object(env_, ctx));
        }

        int resource_write(anjay_iid_t iid,
                           anjay_rid_t rid,
                           anjay_riid_t riid,
                           anjay_input_ctx_t *ctx) {
            return get_method<jni::jint(
                    jni::jint, jni::jint, jni::jint,
                    jni::Object<utils::NativeInputContextPointer>)>(
                    "resourceWrite")(
                    iid, rid, riid,
                    utils::NativeInputContextPointer::into_object(env_, ctx));
        }

        int resource_execute(anjay_iid_t iid,
                             anjay_rid_t rid,
                             anjay_execute_ctx_t *ctx) {
            auto clazz = jni::Class<HashMap>::Find(env_);
            auto ctor = clazz.GetConstructor<>(env_);
            auto args_map = clazz.New(env_, ctor);

            auto accessor = AccessorBase<HashMap>{ env_, args_map };
            auto args_map_inserter =
                    accessor.get_method<jni::Object<>(jni::Object<>,
                                                      jni::Object<>)>("put");

            int current_arg;
            bool has_value;
            int retval;

            while (!(retval = anjay_execute_get_next_arg(ctx, &current_arg,
                                                         &has_value))) {
                jni::Local<jni::Object<Optional>> opt;
                if (has_value) {
                    std::string value;
                    char buf[128];
                    do {
                        retval = anjay_execute_get_arg_value(ctx, NULL, buf,
                                                             sizeof(buf));
                        if (retval == 0 || retval == ANJAY_BUFFER_TOO_SHORT) {
                            value.append(buf);
                        } else {
                            return retval;
                        }
                    } while (retval == ANJAY_BUFFER_TOO_SHORT);

                    opt = utils::Optional::of(
                                  env_, jni::Make<jni::String>(env_, value))
                                  .into_java();
                } else {
                    opt = utils::Optional::empty(env_).into_java();
                }
                args_map_inserter(jni::Box(env_, current_arg), opt);
            }

            if (retval != ANJAY_EXECUTE_GET_ARG_END) {
                return retval;
            }

            return get_method<jni::jint(jni::jint, jni::jint,
                                        jni::Object<Map>)>("resourceExecute")(
                    iid, rid,
                    jni::Cast(env_, jni::Class<Map>::Find(env_), args_map));
        }

        int resource_reset(anjay_iid_t iid, anjay_rid_t rid) {
            return get_method<jni::jint(jni::jint, jni::jint)>(
                    "resourceReset")(iid, rid);
        }

        int instance_reset(anjay_iid_t iid) {
            return get_method<jni::jint(jni::jint)>("instanceReset")(iid);
        }

        int instance_create(anjay_iid_t iid) {
            return get_method<jni::jint(jni::jint)>("instanceCreate")(iid);
        }

        int transaction_begin() {
            return get_method<jni::jint()>("transactionBegin")();
        }

        int transaction_validate() {
            return get_method<jni::jint()>("transactionValidate")();
        }

        int transaction_commit() {
            return get_method<jni::jint()>("transactionCommit")();
        }

        int transaction_rollback() {
            return get_method<jni::jint()>("transactionRollback")();
        }

        bool implements_attr_handlers() {
            return get_method<jni::jboolean()>("implementsAttrHandlers")();
        }

        int object_read_default_attrs(anjay_ssid_t ssid,
                                      anjay_dm_oi_attributes_t *out) {
            auto attrs_by_reference =
                    utils::ObjectInstanceAttrsByReference::New(env_);
            int result = get_method<jni::jint(
                    jni::jint,
                    jni::Object<utils::ObjectInstanceAttrsByReference>)>(
                    "objectReadDefaultAttrs")(ssid, attrs_by_reference);
            if (!result) {
                auto attrs = utils::ObjectInstanceAttrsByReference::get_value(
                        env_, attrs_by_reference);
                *out = utils::ObjectInstanceAttrs::into_native(env_, attrs);
            }
            return result;
        }

        int object_write_default_attrs(anjay_ssid_t ssid,
                                       const anjay_dm_oi_attributes_t *attrs) {
            return get_method<jni::jint(
                    jni::jint, jni::Object<utils::ObjectInstanceAttrs>)>(
                    "objectWriteDefaultAttrs")(
                    ssid, utils::ObjectInstanceAttrs::into_java(env_, attrs));
        }

        int instance_read_default_attrs(anjay_iid_t iid,
                                        anjay_ssid_t ssid,
                                        anjay_dm_oi_attributes_t *out) {
            auto attrs_by_reference =
                    utils::ObjectInstanceAttrsByReference::New(env_);
            int result = get_method<jni::jint(
                    jni::jint, jni::jint,
                    jni::Object<utils::ObjectInstanceAttrsByReference>)>(
                    "instanceReadDefaultAttrs")(iid, ssid, attrs_by_reference);
            if (!result) {
                auto attrs = utils::ObjectInstanceAttrsByReference::get_value(
                        env_, attrs_by_reference);
                *out = utils::ObjectInstanceAttrs::into_native(env_, attrs);
            }
            return result;
        }

        int
        instance_write_default_attrs(anjay_iid_t iid,
                                     anjay_ssid_t ssid,
                                     const anjay_dm_oi_attributes_t *attrs) {
            return get_method<jni::jint(
                    jni::jint, jni::jint,
                    jni::Object<utils::ObjectInstanceAttrs>)>(
                    "instanceWriteDefaultAttrs")(
                    iid, ssid,
                    utils::ObjectInstanceAttrs::into_java(env_, attrs));
        }

        int resource_read_attrs(anjay_iid_t iid,
                                anjay_rid_t rid,
                                anjay_ssid_t ssid,
                                anjay_dm_r_attributes_t *out) {
            auto attrs_by_reference =
                    utils::ResourceAttrsByReference::New(env_);
            int result = get_method<jni::jint(
                    jni::jint, jni::jint, jni::jint,
                    jni::Object<utils::ResourceAttrsByReference>)>(
                    "resourceReadAttrs")(iid, rid, ssid, attrs_by_reference);
            if (!result) {
                auto attrs = utils::ResourceAttrsByReference::get_value(
                        env_, attrs_by_reference);
                *out = utils::ResourceAttrs::into_native(env_, attrs);
            }
            return result;
        }

        int resource_write_attrs(anjay_iid_t iid,
                                 anjay_rid_t rid,
                                 anjay_ssid_t ssid,
                                 const anjay_dm_r_attributes_t *attrs) {
            return get_method<jni::jint(jni::jint, jni::jint, jni::jint,
                                        jni::Object<utils::ResourceAttrs>)>(
                    "resourceWriteAttrs")(
                    iid, rid, ssid,
                    utils::ResourceAttrs::into_java(env_, attrs));
        }

        int resource_instance_read_attrs(anjay_iid_t iid,
                                         anjay_rid_t rid,
                                         anjay_riid_t riid,
                                         anjay_ssid_t ssid,
                                         anjay_dm_r_attributes_t *out) {
            auto attrs_by_reference =
                    utils::ResourceAttrsByReference::New(env_);
            int result = get_method<jni::jint(
                    jni::jint, jni::jint, jni::jint, jni::jint,
                    jni::Object<utils::ResourceAttrsByReference>)>(
                    "resourceInstanceReadAttrs")(iid, rid, riid, ssid,
                                                 attrs_by_reference);
            if (!result) {
                auto attrs = utils::ResourceAttrsByReference::get_value(
                        env_, attrs_by_reference);
                *out = utils::ResourceAttrs::into_native(env_, attrs);
            }
            return result;
        }

        int
        resource_instance_write_attrs(anjay_iid_t iid,
                                      anjay_rid_t rid,
                                      anjay_riid_t riid,
                                      anjay_ssid_t ssid,
                                      const anjay_dm_r_attributes_t *attrs) {
            return get_method<jni::jint(jni::jint, jni::jint, jni::jint,
                                        jni::jint,
                                        jni::Object<utils::ResourceAttrs>)>(
                    "resourceInstanceWriteAttrs")(
                    iid, rid, riid, ssid,
                    utils::ResourceAttrs::into_java(env_, attrs));
        }
    };
};

} // namespace utils
