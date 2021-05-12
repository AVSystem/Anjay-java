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

#include "./native_access_control.hpp"

#include "./util_classes/cast_id.hpp"
#include "./util_classes/exception.hpp"

#include <anjay/access_control.h>

NativeAccessControl::NativeAccessControl(jni::JNIEnv &env,
                                         const jni::Object<NativeAnjay> &anjay)
        : anjay_() {
    auto native_anjay = NativeAnjay::into_native(env, anjay);
    auto raw_anjay = native_anjay->get_anjay();
    anjay_ = raw_anjay;
    int result = anjay_access_control_install(raw_anjay.get());
    if (result) {
        avs_throw(AnjayException(env, result,
                                 "Could not install Access Control"));
    }
}

void NativeAccessControl::set_acl(jni::JNIEnv &env,
                                  jni::jint oid,
                                  jint iid,
                                  jni::jint ssid,
                                  jni::jint access_mask) {
    anjay_oid_t anjay_oid = utils::cast_id<anjay_oid_t>(env, oid);
    anjay_iid_t anjay_iid = utils::cast_id<anjay_iid_t>(env, iid);
    anjay_ssid_t anjay_ssid = utils::cast_id<anjay_ssid_t>(env, ssid);
    anjay_access_mask_t anjay_access_mask =
            utils::cast_id<anjay_access_mask_t>(env, access_mask);
    if (auto locked = anjay_.lock()) {
        int result =
                anjay_access_control_set_acl(locked.get(), anjay_oid, anjay_iid,
                                             anjay_ssid, anjay_access_mask);
        if (result) {
            avs_throw(AnjayException(env, result, "could not set acl"));
        }
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAccessControl::purge(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        anjay_access_control_purge(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jboolean NativeAccessControl::is_modified(jni::JNIEnv &env) {
    if (auto locked = anjay_.lock()) {
        return anjay_access_control_is_modified(locked.get());
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeAccessControl::persist(jni::JNIEnv &env,
                             jni::Object<utils::OutputStream> &output_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::OutputStream::get_avs_stream(&output_stream);

        if (!stream
                || avs_is_err(anjay_access_control_persist(locked.get(),
                                                           stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

jni::jint
NativeAccessControl::restore(jni::JNIEnv &env,
                             jni::Object<utils::InputStream> &input_stream) {
    if (auto locked = anjay_.lock()) {
        auto stream = utils::InputStream::get_avs_stream(&input_stream);

        if (!stream
                || avs_is_err(anjay_access_control_restore(locked.get(),
                                                           stream.get()))) {
            return -1;
        }
        return 0;
    } else {
        avs_throw(IllegalStateException(env, "anjay object expired"));
    }
}

void NativeAccessControl::register_native(jni::JNIEnv &env) {
#define METHOD(MethodPtr, Name) \
    jni::MakeNativePeerMethod<decltype(MethodPtr), (MethodPtr)>(Name)

    // clang-format off
    jni::RegisterNativePeer<NativeAccessControl>(
            env, jni::Class<NativeAccessControl>::Find(env), "self",
            jni::MakePeer<NativeAccessControl, jni::Object<NativeAnjay> &>,
            "init",
            "cleanup",
            METHOD(&NativeAccessControl::purge, "accessControlPurge"),
            METHOD(&NativeAccessControl::is_modified, "accessControlIsModified"),
            METHOD(&NativeAccessControl::persist, "accessControlPersist"),
            METHOD(&NativeAccessControl::restore, "accessControlRestore"),
            METHOD(&NativeAccessControl::set_acl, "accessControlSetAcl"));
    // clang-format on

#undef METHOD
}
