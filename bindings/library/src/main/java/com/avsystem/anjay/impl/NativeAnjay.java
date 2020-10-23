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

package com.avsystem.anjay.impl;

import com.avsystem.anjay.Anjay.Configuration;
import com.avsystem.anjay.Anjay.SocketEntry;
import com.avsystem.anjay.Anjay.Transport;
import com.avsystem.anjay.AnjayException;
import com.avsystem.anjay.AnjayObject;
import java.nio.channels.SelectableChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NativeAnjay implements AutoCloseable {
    static {
        System.loadLibrary("anjay-jni");
    }

    private native NativeSocketEntry[] anjayGetSocketEntries();

    private native void anjayServe(long socketPtr);

    private native void anjaySchedRun();

    private native Duration anjaySchedTimeToNext();

    private native int anjayScheduleRegistrationUpdate(int ssid);

    private native int anjayScheduleTransportReconnect(NativeTransportSet transportSet);

    private native boolean anjayTransportIsOffline(NativeTransportSet transportSet);

    private native int anjayTransportEnterOffline(NativeTransportSet transportSet);

    private native int anjayTransportExitOffline(NativeTransportSet transportSet);

    private native int anjayDisableServer(int ssid);

    private native int anjayDisableServerWithTimeout(int ssid, Optional<Duration> duration);

    private native int anjayEnableServer(int ssid);

    private native int anjayNotifyChanged(int oid, int iid, int rid);

    private native int anjayNotifyInstancesChanged(int oid);

    private native int anjayRegisterObject(NativeAnjayObject object);

    private native boolean anjayHasSecurityConfigForUri(String uri);

    private native void init(Configuration config);

    private native void cleanup();

    public static native String getVersion();

    public static native int getSsidAny();

    public static native int getSsidBootstrap();

    public static native int getIdInvalid();

    public static native int getErrorBadRequest();

    public static native int getErrorUnauthorized();

    public static native int getErrorBadOption();

    public static native int getErrorNotFound();

    public static native int getErrorMethodNotAllowed();

    public static native int getErrorNotAcceptable();

    public static native int getErrorRequestEntityIncomplete();

    public static native int getErrorInternal();

    public static native int getErrorNotImplemented();

    public static native int getErrorServiceUnavailable();

    private long self;
    private final List<SocketEntry> sockets;
    private final Map<SocketEntry, NativeSocketEntry> nativeSockets;

    void ensureValidState() {
        if (this.self == 0) {
            throw new IllegalStateException("Attempted to use a closed NativeAnjay object");
        }
    }

    public NativeAnjay(Configuration config) {
        init(config);
        this.sockets = new ArrayList<>();
        this.nativeSockets = new HashMap<>();
    }

    @Override
    public void close() {
        cleanup();
        this.self = 0;
    }

    public List<SocketEntry> getSocketEntries() {
        ensureValidState();
        this.sockets.clear();
        this.nativeSockets.clear();
        for (NativeSocketEntry entry : this.anjayGetSocketEntries()) {
            SocketEntry socket = entry.intoSocketEntry();
            this.sockets.add(socket);
            this.nativeSockets.put(socket, entry);
        }
        return this.sockets;
    }

    public void schedRun() {
        ensureValidState();
        this.anjaySchedRun();
    }

    public void serve(SelectableChannel channel) {
        ensureValidState();
        for (SocketEntry entry : this.sockets) {
            if (entry.channel == channel) {
                this.anjayServe(this.nativeSockets.get(entry).getSocketPtr());
                return;
            }
        }
        throw new IllegalArgumentException("Passed channel does not belong to any known channels");
    }

    public Optional<Duration> timeToNext() {
        ensureValidState();
        return Optional.ofNullable(this.anjaySchedTimeToNext());
    }

    public void scheduleRegistrationUpdate(int ssid) {
        ensureValidState();
        int result = this.anjayScheduleRegistrationUpdate(ssid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_schedule_registration_update() failed");
        }
    }

    public void scheduleReconnect(Set<Transport> transportSet) {
        ensureValidState();
        int result = this.anjayScheduleTransportReconnect(NativeTransportSet.fromSet(transportSet));
        if (result < 0) {
            throw new AnjayException(result, "anjay_schedule_reconnect() failed");
        }
    }

    public boolean isOffline(Set<Transport> transportSet) {
        ensureValidState();
        return this.anjayTransportIsOffline(NativeTransportSet.fromSet(transportSet));
    }

    public void enterOffline(Set<Transport> transportSet) {
        ensureValidState();
        int result = this.anjayTransportEnterOffline(NativeTransportSet.fromSet(transportSet));
        if (result < 0) {
            throw new AnjayException(result, "anjay_transport_enter_offline() failed");
        }
    }

    public void exitOffline(Set<Transport> transportSet) {
        ensureValidState();
        int result = this.anjayTransportExitOffline(NativeTransportSet.fromSet(transportSet));
        if (result < 0) {
            throw new AnjayException(result, "anjay_transport_exit_offline() failed");
        }
    }

    public void disableServer(int ssid) {
        ensureValidState();
        int result = this.anjayDisableServer(ssid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_disable_server() failed");
        }
    }

    public void enableServer(int ssid) {
        ensureValidState();
        int result = this.anjayEnableServer(ssid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_enable_server() failed");
        }
    }

    public void disableServerWithTimeout(int ssid, Optional<Duration> timeout) {
        ensureValidState();
        int result = this.anjayDisableServerWithTimeout(ssid, timeout);
        if (result < 0) {
            throw new AnjayException(result, "anjay_disable_server_with_timeout() failed");
        }
    }

    public void notifyChanged(int oid, int iid, int rid) {
        ensureValidState();
        int result = this.anjayNotifyChanged(oid, iid, rid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_notify_changed() failed");
        }
    }

    public void notifyInstancesChanged(int oid) {
        ensureValidState();
        int result = this.anjayNotifyInstancesChanged(oid);
        if (result < 0) {
            throw new AnjayException(result, "anjay_notify_instances_changed() failed");
        }
    }

    public void registerObject(AnjayObject object) {
        ensureValidState();
        if (object == null) {
            throw new AnjayException(-1, "attempted to register null object");
        }
        int result = this.anjayRegisterObject(new NativeAnjayObject(object));
        if (result < 0) {
            throw new AnjayException(result, "anjay_register_object() failed");
        }
    }

    public boolean hasSecurityConfigForUri(String uri) {
        return this.anjayHasSecurityConfigForUri(uri);
    }
}
