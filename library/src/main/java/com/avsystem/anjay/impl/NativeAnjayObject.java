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

import com.avsystem.anjay.Anjay;
import com.avsystem.anjay.AnjayAttributes.ObjectInstanceAttrs;
import com.avsystem.anjay.AnjayAttributes.ResourceAttrs;
import com.avsystem.anjay.AnjayInputContext;
import com.avsystem.anjay.AnjayObject;
import com.avsystem.anjay.AnjayObject.ResourceDef;
import com.avsystem.anjay.AnjayObjectAttrHandlers;
import com.avsystem.anjay.AnjayOutputContext;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

public final class NativeAnjayObject {
    private final AnjayObject object;
    private static final Integer[] EMPTY_INSTANCES_ARRAY = new Integer[] {};
    private static final ResourceDef[] EMPTY_RESOURCES_ARRAY = new ResourceDef[] {};

    private static class IntegerArrayByReference {
        // Used on C++ side.
        @SuppressWarnings("unused")
        public Integer[] value;
    }

    private static class ResourceDefArrayByReference {
        // Used on C++ side.
        @SuppressWarnings("unused")
        public ResourceDef[] value;
    }

    public static final class ObjectInstanceAttrsByReference {
        // Used on C++ side.
        @SuppressWarnings("unused")
        public ObjectInstanceAttrs value;
    }

    public static final class ResourceAttrsByReference {
        // Used on C++ side.
        @SuppressWarnings("unused")
        public ResourceAttrs value;
    }

    public NativeAnjayObject(AnjayObject object) {
        this.object = object;
    }

    int oid() {
        return this.object.oid();
    }

    String version() {
        return this.object.version();
    }

    int resourceWrite(int iid, int rid, int riid, NativeInputContextPointer ptr) {
        try (AnjayInputContext wrapper = new AnjayInputContext(ptr)) {
            if (riid == Anjay.ID_INVALID) {
                this.object.resourceWrite(iid, rid, wrapper);
            } else {
                this.object.resourceWrite(iid, rid, riid, wrapper);
            }
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceRead(int iid, int rid, int riid, NativeOutputContextPointer ptr) {
        try (AnjayOutputContext wrapper = new AnjayOutputContext(ptr)) {
            if (riid == Anjay.ID_INVALID) {
                this.object.resourceRead(iid, rid, wrapper);
            } else {
                this.object.resourceRead(iid, rid, riid, wrapper);
            }
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceExecute(int iid, int rid, Map<Integer, Optional<String>> args) {
        try {
            this.object.resourceExecute(iid, rid, args);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceReset(int iid, int rid) {
        try {
            this.object.resourceReset(iid, rid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instances(IntegerArrayByReference result) {
        try {
            SortedSet<Integer> instances = this.object.instances();
            if (instances == null) {
                result.value = EMPTY_INSTANCES_ARRAY;
            } else {
                result.value = instances.toArray(new Integer[instances.size()]);
            }
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resources(int iid, ResourceDefArrayByReference result) {
        try {
            SortedSet<ResourceDef> resources = this.object.resources(iid);
            if (resources == null) {
                result.value = EMPTY_RESOURCES_ARRAY;
            } else {
                result.value = resources.toArray(new ResourceDef[resources.size()]);
            }
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceInstances(int iid, int rid, IntegerArrayByReference result) {
        try {
            SortedSet<Integer> instances = this.object.resourceInstances(iid, rid);
            if (instances == null) {
                result.value = EMPTY_INSTANCES_ARRAY;
            } else {
                result.value = instances.toArray(new Integer[instances.size()]);
            }
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instanceReset(int iid) {
        try {
            this.object.instanceReset(iid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instanceCreate(int iid) {
        try {
            this.object.instanceCreate(iid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instanceRemove(int iid) {
        try {
            this.object.instanceRemove(iid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int transactionBegin() {
        try {
            this.object.transactionBegin();
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int transactionValidate() {
        try {
            this.object.transactionValidate();
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int transactionCommit() {
        try {
            this.object.transactionCommit();
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int transactionRollback() {
        try {
            this.object.transactionRollback();
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    boolean implementsAttrHandlers() {
        return this.object instanceof AnjayObjectAttrHandlers;
    }

    int objectReadDefaultAttrs(int ssid, ObjectInstanceAttrsByReference attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            attrs.value = handlers.objectReadDefaultAttrs(ssid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int objectWriteDefaultAttrs(int ssid, ObjectInstanceAttrs attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            handlers.objectWriteDefaultAttrs(ssid, attrs);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instanceReadDefaultAttrs(int iid, int ssid, ObjectInstanceAttrsByReference attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            attrs.value = handlers.instanceReadDefaultAttrs(iid, ssid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int instanceWriteDefaultAttrs(int iid, int ssid, ObjectInstanceAttrs attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            handlers.instanceWriteDefaultAttrs(iid, ssid, attrs);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceReadAttrs(int iid, int rid, int ssid, ResourceAttrsByReference attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            attrs.value = handlers.resourceReadAttrs(iid, rid, ssid);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }

    int resourceWriteAttrs(int iid, int rid, int ssid, ResourceAttrs attrs) {
        assert implementsAttrHandlers()
                : "bug: should not be called when object doesn't implement attribute handlers";
        AnjayObjectAttrHandlers handlers = (AnjayObjectAttrHandlers) this.object;
        try {
            handlers.resourceWriteAttrs(iid, rid, ssid, attrs);
            return 0;
        } catch (Throwable t) {
            return Utils.handleException(t);
        }
    }
}
