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

#include <cstring>

#include "../jni_wrapper.hpp"

#include "./accessor_base.hpp"
#include "./exception.hpp"

namespace utils {

class ByteBuffer {
    jni::JNIEnv &env_;
    jni::Global<jni::Object<ByteBuffer>> self_;

    bool is_direct() {
        return utils::AccessorBase<ByteBuffer>{ env_, self_ }
                .get_method<jni::jboolean()>("isDirect")();
    }

public:
    static constexpr auto Name() {
        return "java/nio/ByteBuffer";
    }

    ByteBuffer(jni::JNIEnv &env, const jni::Local<jni::Object<ByteBuffer>> &buf)
            : env_(env), self_(jni::NewGlobal(env_, buf)) {}

    ByteBuffer(jni::JNIEnv &env, size_t size) : env_(env), self_() {
        if (size > static_cast<size_t>(std::numeric_limits<jni::jint>::max())) {
            avs_throw(IllegalArgumentException(
                    env, "Buffer size exceeds jni::jint max value"));
        }
        self_ = jni::NewGlobal(
                env_,
                utils::AccessorBase<ByteBuffer>::get_static_method<
                        jni::Object<ByteBuffer>(jni::jint)>(
                        env, "allocateDirect")(static_cast<jni::jint>(size)));
    }

    jni::Global<jni::Object<ByteBuffer>> into_java() {
        return jni::NewGlobal(env_, self_);
    }

    void put(const std::vector<jni::jbyte> &data) {
        auto accessor = utils::AccessorBase<ByteBuffer>{ env_, self_ };
        auto appender = accessor.get_method<jni::Object<ByteBuffer>(
                jni::Array<jni::jbyte>)>("put");
        appender(jni::Make<jni::Array<jni::jbyte>>(env_, data));
    }

    size_t capacity() {
        return utils::AccessorBase<ByteBuffer>{ env_, self_ }
                .get_method<jni::jint()>("capacity")();
    }

    size_t remaining() {
        return utils::AccessorBase<ByteBuffer>{ env_, self_ }
                .get_method<jni::jint()>("remaining")();
    }

    void rewind() {
        struct Buffer {
            static constexpr auto Name() {
                return "java/nio/Buffer";
            }
        };
        utils::AccessorBase<ByteBuffer>{ env_, self_ }
                .get_method<jni::Object<Buffer>()>("rewind")();
    }

    size_t copy_to(void *data, size_t size) {
        if (!is_direct()) {
            avs_throw(UnsupportedOperationException(
                    env_,
                    "Sorry. Copying from non-directly allocated buffers is not "
                    "supported"));
        }
        const void *buffer = jni::GetDirectBufferAddress(env_, *self_);
        const size_t to_copy = std::min(remaining(), size);
        memcpy(data, buffer, to_copy);
        return to_copy;
    }
};

/**
 * Used as a lightweight wrapper over C/C++-side allocated buffer. Comes in
 * handy when trying to exchange the data between Java and C++ - e.g. in socket
 * receive() methods and similar, at the same time avoiding unnecessary copies.
 */
class BufferView {
    BufferView(const BufferView &) = delete;
    BufferView(BufferView &&) = delete;
    BufferView &operator=(const BufferView &) = delete;
    BufferView &&operator=(BufferView &&) = delete;

    ByteBuffer buffer_;

public:
    /**
     * CAUTION: The lifetime of @p native_buffer MUST EXCEED the lifetime
     * of an instance of this class and ALL instances returned by
     * BufferView::into_java().
     *
     * IMPORTANT: @p native_buffer is not const, because there is no way
     * to guarantee that Java won't attempt to modify the buffer if it's
     * passed to it. Use with const pointers (and const_cast) only if you're
     * 100% sure Java does not touch the buffer contents, otherwise the behavior
     * is undefined.
     */
    BufferView(jni::JNIEnv &env, void *native_buffer, size_t length)
            : buffer_(env,
                      jni::Local<jni::Object<ByteBuffer>>(
                              env,
                              &jni::NewDirectByteBuffer(
                                      env, native_buffer, length))) {}

    jni::Global<jni::Object<ByteBuffer>> into_java() {
        return buffer_.into_java();
    }
};

} // namespace utils
