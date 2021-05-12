# Copyright 2020-2021 AVSystem <avsystem@avsystem.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cmake_minimum_required(VERSION 3.10)
project(anjay-jni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(EXTRA_CFLAGS "-Wall -Wextra -Wpedantic")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${EXTRA_CFLAGS}")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${EXTRA_CFLAGS}")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT ANDROID)
    # Android has JNI in default include paths, however it does not have
    # AWT nor JVM libraries, which CMake unnecessarily tries to find.
    find_package(JNI)
    include_directories(${JNI_INCLUDE_DIRS})
else()
    # This overrides CMake install() command with a no-op. This is required for
    # add_subdirectory(deps/mbedtls) to work, because otherwise install() calls in
    # avs_commons would depend on unexported targets from mbed TLS.
    function(install)
    endfunction()

    set(ENABLE_TESTING OFF CACHE INTERNAL "")
    set(ENABLE_PROGRAMS OFF CACHE INTERNAL "")
    set(USE_SHARED_MBEDTLS_LIBRARY OFF CACHE INTERNAL "")
    set(USE_STATIC_MBEDTLS_LIBRARY ON CACHE INTERNAL "")
    add_subdirectory(deps/mbedtls EXCLUDE_FROM_ALL)
    set(MBEDTLS_ROOT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/deps/mbedtls" CACHE INTERNAL "")
    target_include_directories(mbedtls INTERFACE "${MBEDTLS_ROOT_DIR}/include")
    target_include_directories(mbedcrypto INTERFACE "${MBEDTLS_ROOT_DIR}/include")
    target_include_directories(mbedx509 INTERFACE "${MBEDTLS_ROOT_DIR}/include")
endif()

set(WITH_EST OFF CACHE INTERNAL "")
set(WITH_DEMO OFF CACHE INTERNAL "")
set(WITH_HTTP_DOWNLOAD ON CACHE INTERNAL "")
set(WITH_POSIX_AVS_SOCKET OFF CACHE INTERNAL "")
add_subdirectory(deps/anjay EXCLUDE_FROM_ALL)

option(WITH_INTEGRATION_TEST "Enables/disables integration tests target" OFF)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}
                    deps/jni.hpp/include)

add_library(${PROJECT_NAME} SHARED
            src/util_classes/accessor_base.hpp
            src/util_classes/attributes.hpp
            src/util_classes/byte_buffer.hpp
            src/util_classes/coap_udp_tx_params.hpp
            src/util_classes/configuration.hpp
            src/util_classes/construct.hpp
            src/util_classes/download_configuration.hpp
            src/util_classes/dtls_handshake_timeouts.hpp
            src/util_classes/dtls_version.hpp
            src/util_classes/duration.hpp
            src/util_classes/exception.cpp
            src/util_classes/exception.hpp
            src/util_classes/hash_map.hpp
            src/util_classes/integer_array_by_reference.hpp
            src/util_classes/level.hpp
            src/util_classes/logger.hpp
            src/util_classes/map.hpp
            src/util_classes/native_anjay_object.hpp
            src/util_classes/native_bytes_context_pointer.hpp
            src/util_classes/native_input_context_pointer.hpp
            src/util_classes/native_output_context_pointer.hpp
            src/util_classes/native_pointer.hpp
            src/util_classes/native_socket_entry.hpp
            src/util_classes/native_transport_set.hpp
            src/util_classes/objlnk.hpp
            src/util_classes/optional.hpp
            src/util_classes/optional_tag.hpp
            src/util_classes/resource_def_array_by_reference.hpp
            src/util_classes/resource_def.hpp
            src/util_classes/resource_kind.hpp
            src/util_classes/selectable_channel.hpp
            src/util_classes/transport.hpp
            src/util_classes/security_info_cert.hpp
            src/util_classes/security_info_psk.hpp
            src/util_classes/security_config.hpp

            src/compat/avs_net_socket.hpp
            src/compat/net_impl.cpp
            src/compat/socket_address.hpp
            src/compat/socket_channel.hpp
            src/compat/socket_error.hpp
            src/compat/socket.hpp

            src/global_context.cpp
            src/global_context.hpp
            src/jni_wrapper.hpp
            src/main.cpp
            src/native_access_control.cpp
            src/native_access_control.hpp
            src/native_anjay.cpp
            src/native_anjay.hpp
            src/native_anjay_download.hpp
            src/native_anjay_download.cpp
            src/native_anjay_object_adapter.cpp
            src/native_anjay_object_adapter.hpp
            src/native_attr_storage.cpp
            src/native_attr_storage.hpp
            src/native_bytes_context.cpp
            src/native_bytes_context.hpp
            src/native_firmware_update.cpp
            src/native_firmware_update.hpp
            src/native_input_context.cpp
            src/native_input_context.hpp
            src/native_log.cpp
            src/native_log.hpp
            src/native_output_context.cpp
            src/native_output_context.hpp
            src/native_security_object.cpp
            src/native_security_object.hpp
            src/native_server_object.cpp)

target_link_libraries(${PROJECT_NAME} ${JAVA_JVM_LIBRARY} anjay)

if(WITH_INTEGRATION_TEST)
    add_custom_target(demo ALL
                      COMMAND cd ${CMAKE_SOURCE_DIR}/../ && ./gradlew :demo:build -PbuildDir=${CMAKE_BINARY_DIR} && cd -
                      COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_BINARY_DIR}/libs/demo.jar ${CMAKE_BINARY_DIR}
                      COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:${PROJECT_NAME}> ${CMAKE_BINARY_DIR}
                      WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/)
    enable_testing()
    add_custom_target(check)
    add_subdirectory(tests)
endif()
