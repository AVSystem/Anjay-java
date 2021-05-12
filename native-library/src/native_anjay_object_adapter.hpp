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

#include <anjay/anjay.h>

#include "./jni_wrapper.hpp"

#include "util_classes/native_anjay_object.hpp"

#include <string>

class NativeAnjayObjectAdapter {
    anjay_dm_object_def_t def_;
    const anjay_dm_object_def_t *const def_ptr_;

    std::weak_ptr<anjay_t> anjay_;
    utils::NativeAnjayObject::Accessor accessor_;
    std::string version_;

    NativeAnjayObjectAdapter(const NativeAnjayObjectAdapter &) = delete;
    NativeAnjayObjectAdapter &
    operator=(const NativeAnjayObjectAdapter &) = delete;

    NativeAnjayObjectAdapter(NativeAnjayObjectAdapter &&) = delete;
    NativeAnjayObjectAdapter &operator=(NativeAnjayObjectAdapter &&) = delete;

    static NativeAnjayObjectAdapter *
    get_obj(const anjay_dm_object_def_t *const *obj_ptr);

    static int list_instances(anjay_t *,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_dm_list_ctx_t *ctx);

    static int list_resources(anjay_t *,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_iid_t iid,
                              anjay_dm_resource_list_ctx_t *ctx);

    static int
    list_resource_instances(anjay_t *,
                            const anjay_dm_object_def_t *const *obj_ptr,
                            anjay_iid_t iid,
                            anjay_rid_t rid,
                            anjay_dm_list_ctx_t *ctx);

    static int resource_read(anjay_t *,
                             const anjay_dm_object_def_t *const *obj_ptr,
                             anjay_iid_t iid,
                             anjay_rid_t rid,
                             anjay_riid_t riid,
                             anjay_output_ctx_t *ctx);

    static int resource_write(anjay_t *,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_iid_t iid,
                              anjay_rid_t rid,
                              anjay_riid_t riid,
                              anjay_input_ctx_t *ctx);

    static int resource_execute(anjay_t *,
                                const anjay_dm_object_def_t *const *obj_ptr,
                                anjay_iid_t iid,
                                anjay_rid_t rid,
                                anjay_execute_ctx_t *ctx);

    static int resource_reset(anjay_t *anjay,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_iid_t iid,
                              anjay_rid_t rid);

    static int instance_reset(anjay_t *,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_iid_t iid);

    static int instance_create(anjay_t *,
                               const anjay_dm_object_def_t *const *obj_ptr,
                               anjay_iid_t iid);

    static int transaction_begin(anjay_t *,
                                 const anjay_dm_object_def_t *const *obj_ptr);

    static int
    transaction_validate(anjay_t *,
                         const anjay_dm_object_def_t *const *obj_ptr);

    static int transaction_commit(anjay_t *,
                                  const anjay_dm_object_def_t *const *obj_ptr);
    static int
    transaction_rollback(anjay_t *,
                         const anjay_dm_object_def_t *const *obj_ptr);

    static int
    object_read_default_attrs(anjay_t *,
                              const anjay_dm_object_def_t *const *obj_ptr,
                              anjay_ssid_t ssid,
                              anjay_dm_oi_attributes_t *out);

    static int
    object_write_default_attrs(anjay_t *,
                               const anjay_dm_object_def_t *const *obj_ptr,
                               anjay_ssid_t ssid,
                               const anjay_dm_oi_attributes_t *attrs);

    static int
    instance_read_default_attrs(anjay_t *,
                                const anjay_dm_object_def_t *const *obj_ptr,
                                anjay_iid_t iid,
                                anjay_ssid_t ssid,
                                anjay_dm_oi_attributes_t *out);

    static int
    instance_write_default_attrs(anjay_t *,
                                 const anjay_dm_object_def_t *const *obj_ptr,
                                 anjay_iid_t iid,
                                 anjay_ssid_t ssid,
                                 const anjay_dm_oi_attributes_t *attrs);

    static int resource_read_attrs(anjay_t *,
                                   const anjay_dm_object_def_t *const *obj_ptr,
                                   anjay_iid_t iid,
                                   anjay_rid_t rid,
                                   anjay_ssid_t ssid,
                                   anjay_dm_r_attributes_t *out);

    static int resource_write_attrs(anjay_t *,
                                    const anjay_dm_object_def_t *const *obj_ptr,
                                    anjay_iid_t iid,
                                    anjay_rid_t rid,
                                    anjay_ssid_t ssid,
                                    const anjay_dm_r_attributes_t *attrs);

    static int
    resource_instance_read_attrs(anjay_t *,
                                 const anjay_dm_object_def_t *const *obj_ptr,
                                 anjay_iid_t iid,
                                 anjay_rid_t rid,
                                 anjay_riid_t riid,
                                 anjay_ssid_t ssid,
                                 anjay_dm_r_attributes_t *out);

    static int
    resource_instance_write_attrs(anjay_t *,
                                  const anjay_dm_object_def_t *const *obj_ptr,
                                  anjay_iid_t iid,
                                  anjay_rid_t rid,
                                  anjay_riid_t riid,
                                  anjay_ssid_t ssid,
                                  const anjay_dm_r_attributes_t *attrs);

public:
    explicit NativeAnjayObjectAdapter(
            const std::weak_ptr<anjay_t> &anjay,
            jni::JNIEnv &env,
            const jni::Object<utils::NativeAnjayObject> &object);
    ~NativeAnjayObjectAdapter();

    int install();
};
