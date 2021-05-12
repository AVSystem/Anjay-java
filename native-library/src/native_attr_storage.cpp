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

#include "./native_attr_storage.hpp"

#include "./util_classes/cast_id.hpp"
#include "./util_classes/exception.hpp"

#include <anjay/attr_storage.h>

NativeAttrStorage::NativeAttrStorage(jni::JNIEnv &env,
                                     const jni::Object<NativeAnjay> &anjay)
        : anjay_() {
    auto native_anjay = NativeAnjay::into_native(env, anjay);
    anjay_ = native_anjay->get_anjay();
    if (auto locked = anjay_.lock()) {
        int result = anjay_attr_storage_install(locked.get());
        if (result) {
            avs_throw(AnjayException(env, result,
                                     "Could not install Attribute storage"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAttrStorage::set_object_attrs(
        jni::JNIEnv &env,
        jni::jint ssid,
        jni::jint oid,
        jni::Object<utils::ObjectInstanceAttrs> &attrs) {
    anjay_ssid_t anjay_ssid = utils::cast_id<anjay_ssid_t>(env, ssid);
    anjay_oid_t anjay_oid = utils::cast_id<anjay_oid_t>(env, oid);
    anjay_dm_oi_attributes_t anjay_attrs =
            utils::ObjectInstanceAttrs::into_native(env, attrs);
    if (auto locked = anjay_.lock()) {
        int result =
                anjay_attr_storage_set_object_attrs(locked.get(), anjay_ssid,
                                                    anjay_oid, &anjay_attrs);
        if (result) {
            avs_throw(AnjayException(env, result, "could not set attributes"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAttrStorage::set_instance_attrs(
        jni::JNIEnv &env,
        jni::jint ssid,
        jni::jint oid,
        jni::jint iid,
        jni::Object<utils::ObjectInstanceAttrs> &attrs) {
    anjay_ssid_t anjay_ssid = utils::cast_id<anjay_ssid_t>(env, ssid);
    anjay_oid_t anjay_oid = utils::cast_id<anjay_oid_t>(env, oid);
    anjay_iid_t anjay_iid = utils::cast_id<anjay_iid_t>(env, iid);
    anjay_dm_oi_attributes_t anjay_attrs =
            utils::ObjectInstanceAttrs::into_native(env, attrs);
    if (auto locked = anjay_.lock()) {
        int result = anjay_attr_storage_set_instance_attrs(
                locked.get(), anjay_ssid, anjay_oid, anjay_iid, &anjay_attrs);
        if (result) {
            avs_throw(AnjayException(env, result, "could not set attributes"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAttrStorage::set_resource_attrs(
        jni::JNIEnv &env,
        jni::jint ssid,
        jni::jint oid,
        jni::jint iid,
        jni::jint rid,
        jni::Object<utils::ResourceAttrs> &attrs) {
    anjay_ssid_t anjay_ssid = utils::cast_id<anjay_ssid_t>(env, ssid);
    anjay_oid_t anjay_oid = utils::cast_id<anjay_oid_t>(env, oid);
    anjay_iid_t anjay_iid = utils::cast_id<anjay_iid_t>(env, iid);
    anjay_rid_t anjay_rid = utils::cast_id<anjay_rid_t>(env, rid);
    anjay_dm_r_attributes_t anjay_attrs =
            utils::ResourceAttrs::into_native(env, attrs);
    if (auto locked = anjay_.lock()) {
        int result =
                anjay_attr_storage_set_resource_attrs(locked.get(), anjay_ssid,
                                                      anjay_oid, anjay_iid,
                                                      anjay_rid, &anjay_attrs);
        if (result) {
            avs_throw(AnjayException(env, result, "could not set attributes"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeAttrStorage::get_attr_period_none(jni::JNIEnv &,
                                        jni::Class<NativeAttrStorage> &) {
    return ANJAY_ATTRIB_PERIOD_NONE;
}

jni::jdouble
NativeAttrStorage::get_attr_value_none(jni::JNIEnv &,
                                       jni::Class<NativeAttrStorage> &) {
    return ANJAY_ATTRIB_VALUE_NONE;
}

void NativeAttrStorage::purge(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        anjay_attr_storage_purge(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jboolean NativeAttrStorage::is_modified(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        return anjay_attr_storage_is_modified(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeAttrStorage::persist(jni::JNIEnv &env,
                           jni::Object<utils::OutputStream> &output_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::OutputStream::get_avs_stream(&output_stream);

        if (!stream
                || avs_is_err(anjay_attr_storage_persist(locked.get(),
                                                         stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeAttrStorage::restore(jni::JNIEnv &env,
                           jni::Object<utils::InputStream> &input_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::InputStream::get_avs_stream(&input_stream);

        if (!stream
                || avs_is_err(anjay_attr_storage_restore(locked.get(),
                                                         stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAttrStorage::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, Name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(Name)

#define STATIC_METHOD(MethodPtr, Name) \
    jni::MakeNativeMethod<decltype(MethodPtr), (MethodPtr)>(Name)

    // clang-format off
    jni::RegisterNativePeer<NativeAttrStorage>(
            env, jni::Class<NativeAttrStorage>::Find(env), "self",
            jni::MakePeer<NativeAttrStorage, jni::Object<NativeAnjay> &>,
            "init",
            "cleanup",
            METHOD(&NativeAttrStorage::purge, "attrStoragePurge"),
            METHOD(&NativeAttrStorage::set_object_attrs, "attrStorageSetObjectAttrs"),
            METHOD(&NativeAttrStorage::set_instance_attrs, "attrStorageSetInstanceAttrs"),
            METHOD(&NativeAttrStorage::set_resource_attrs, "attrStorageSetResourceAttrs"),
            METHOD(&NativeAttrStorage::is_modified, "attrStorageIsModified"),
            METHOD(&NativeAttrStorage::persist, "attrStoragePersist"),
            METHOD(&NativeAttrStorage::restore, "attrStorageRestore"));

    jni::RegisterNatives(
            env, *jni::Class<NativeAttrStorage>::Find(env),
            STATIC_METHOD(&NativeAttrStorage::get_attr_period_none, "getAttrPeriodNone"),
            STATIC_METHOD(&NativeAttrStorage::get_attr_value_none, "getAttrValueNone"));
    // clang-format on

#undef METHOD
#undef STATIC_METHOD
}
