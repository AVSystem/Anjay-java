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

#include "./native_anjay_object_adapter.hpp"

NativeAnjayObjectAdapter::NativeAnjayObjectAdapter(
        const std::weak_ptr<anjay_t> &anjay,
        jni::JNIEnv &env,
        const jni::Object<utils::NativeAnjayObject> &object)
        : def_(),
          def_ptr_(&def_),
          anjay_(anjay),
          accessor_(env, std::move(object)),
          version_() {
    def_.oid = accessor_.get_oid();
    version_ = accessor_.get_version();
    def_.version = version_.c_str();
    def_.handlers.list_instances = &NativeAnjayObjectAdapter::list_instances;
    def_.handlers.list_resources = &NativeAnjayObjectAdapter::list_resources;
    def_.handlers.list_resource_instances =
            &NativeAnjayObjectAdapter::list_resource_instances;
    def_.handlers.resource_read = &NativeAnjayObjectAdapter::resource_read;
    def_.handlers.resource_write = &NativeAnjayObjectAdapter::resource_write;
    def_.handlers.resource_execute =
            &NativeAnjayObjectAdapter::resource_execute;
    def_.handlers.resource_reset = &NativeAnjayObjectAdapter::resource_reset;
    def_.handlers.instance_reset = &NativeAnjayObjectAdapter::instance_reset;
    def_.handlers.instance_create = &NativeAnjayObjectAdapter::instance_create;
    def_.handlers.transaction_begin =
            &NativeAnjayObjectAdapter::transaction_begin;
    def_.handlers.transaction_validate =
            &NativeAnjayObjectAdapter::transaction_validate;
    def_.handlers.transaction_commit =
            &NativeAnjayObjectAdapter::transaction_commit;
    def_.handlers.transaction_rollback =
            &NativeAnjayObjectAdapter::transaction_rollback;

    if (accessor_.implements_attr_handlers()) {
        def_.handlers.object_read_default_attrs =
                &NativeAnjayObjectAdapter::object_read_default_attrs;
        def_.handlers.object_write_default_attrs =
                &NativeAnjayObjectAdapter::object_write_default_attrs;
        def_.handlers.instance_read_default_attrs =
                &NativeAnjayObjectAdapter::instance_read_default_attrs;
        def_.handlers.instance_write_default_attrs =
                &NativeAnjayObjectAdapter::instance_write_default_attrs;
        def_.handlers.resource_read_attrs =
                &NativeAnjayObjectAdapter::resource_read_attrs;
        def_.handlers.resource_write_attrs =
                &NativeAnjayObjectAdapter::resource_write_attrs;
    }
}

NativeAnjayObjectAdapter::~NativeAnjayObjectAdapter() {
    if (auto anjay = anjay_.lock()) {
        // NOTE: this may fail if install() failed, but we don't really care.
        (void) anjay_unregister_object(anjay.get(), &def_ptr_);
    }
}

int NativeAnjayObjectAdapter::install() {
    if (auto anjay = anjay_.lock()) {
        return anjay_register_object(anjay.get(), &def_ptr_);
    } else {
        avs_throw(IllegalStateException(accessor_.get_env(),
                                        "anjay object expired"));
    }
}

NativeAnjayObjectAdapter *
NativeAnjayObjectAdapter::get_obj(const anjay_dm_object_def_t *const *obj_ptr) {
    static const NativeAnjayObjectAdapter *const FAKE_ADAPTER_PTR = nullptr;
    static const auto DEF_PTR_OFFSET = intptr_t(
            reinterpret_cast<const char *>(&FAKE_ADAPTER_PTR[1].def_ptr_)
            - reinterpret_cast<const char *>(&FAKE_ADAPTER_PTR[1]));
    return reinterpret_cast<NativeAnjayObjectAdapter *>(intptr_t(obj_ptr)
                                                        - DEF_PTR_OFFSET);
}

int NativeAnjayObjectAdapter::list_instances(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_dm_list_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.for_each_instance(
            [&](anjay_iid_t iid) { anjay_dm_emit(ctx, iid); });
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::list_resources(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_dm_resource_list_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.for_each_resource(iid, [&](utils::ResourceDef def) {
        anjay_dm_emit_res(ctx, def.rid, def.kind,
                          def.present ? ANJAY_DM_RES_PRESENT
                                      : ANJAY_DM_RES_ABSENT);
    });
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::list_resource_instances(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_dm_list_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.for_each_resource_instance(
            iid, rid, [&](anjay_riid_t riid) { anjay_dm_emit(ctx, riid); });
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_read(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_riid_t riid,
        anjay_output_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_read(iid, rid, riid, ctx);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_write(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_riid_t riid,
        anjay_input_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_write(iid, rid, riid, ctx);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_execute(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_execute_ctx_t *ctx) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_execute(iid, rid, ctx);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_reset(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_reset(iid, rid);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::instance_reset(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.instance_reset(iid);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::instance_create(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.instance_create(iid);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::transaction_begin(
        anjay_t *, const anjay_dm_object_def_t *const *obj_ptr) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.transaction_begin();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::transaction_validate(
        anjay_t *, const anjay_dm_object_def_t *const *obj_ptr) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.transaction_validate();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::transaction_commit(
        anjay_t *, const anjay_dm_object_def_t *const *obj_ptr) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.transaction_commit();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::transaction_rollback(
        anjay_t *, const anjay_dm_object_def_t *const *obj_ptr) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.transaction_rollback();
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::object_read_default_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_ssid_t ssid,
        anjay_dm_oi_attributes_t *out) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.object_read_default_attrs(ssid, out);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::object_write_default_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_ssid_t ssid,
        const anjay_dm_oi_attributes_t *attrs) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.object_write_default_attrs(ssid, attrs);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::instance_read_default_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_ssid_t ssid,
        anjay_dm_oi_attributes_t *out) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.instance_read_default_attrs(iid, ssid, out);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::instance_write_default_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_ssid_t ssid,
        const anjay_dm_oi_attributes_t *attrs) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.instance_write_default_attrs(iid, ssid, attrs);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_read_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_ssid_t ssid,
        anjay_dm_r_attributes_t *out) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_read_attrs(iid, rid, ssid, out);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}

int NativeAnjayObjectAdapter::resource_write_attrs(
        anjay_t *,
        const anjay_dm_object_def_t *const *obj_ptr,
        anjay_iid_t iid,
        anjay_rid_t rid,
        anjay_ssid_t ssid,
        const anjay_dm_r_attributes_t *attrs) try {
    auto &self = *get_obj(obj_ptr);
    return self.accessor_.resource_write_attrs(iid, rid, ssid, attrs);
} catch (...) {
    avs_log_and_clear_exception(DEBUG);
    return -1;
}
