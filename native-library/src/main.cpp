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

#include "./jni_wrapper.hpp"

#include "./native_access_control.hpp"
#include "./native_anjay.hpp"
#include "./native_anjay_download.hpp"
#include "./native_attr_storage.hpp"
#include "./native_bytes_context.hpp"
#include "./native_firmware_update.hpp"
#include "./native_input_context.hpp"
#include "./native_log.hpp"
#include "./native_output_context.hpp"
#include "./native_security_object.hpp"
#include "./native_server_object.hpp"

#include "./global_context.hpp"

#include <clocale>
#include <iostream>

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) try {
    setlocale(LC_ALL, "C");

    jni::JNIEnv &env{ jni::GetEnv(*vm) };
    GlobalContext::init(vm);

    NativeAnjay::register_native(env);
    NativeInputContext::register_native(env);
    NativeOutputContext::register_native(env);
    NativeBytesContext::register_native(env);
    NativeSecurityObject::register_native(env);
    NativeServerObject::register_native(env);
    NativeAttrStorage::register_native(env);
    NativeAccessControl::register_native(env);
    NativeAnjayDownload::register_native(env);
    NativeFirmwareUpdate::register_native(env);
    NativeLog::register_native(env);
    return jni::Unwrap(jni::jni_version_1_6);
} catch (std::exception &e) {
    std::cerr << "Exception in JNI_OnLoad(): " << e.what() << std::endl;
    return -1;
} catch (...) {
    return -1;
}
