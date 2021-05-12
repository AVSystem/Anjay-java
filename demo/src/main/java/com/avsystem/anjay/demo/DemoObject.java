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

package com.avsystem.anjay.demo;

import com.avsystem.anjay.Anjay;
import com.avsystem.anjay.AnjayException;
import com.avsystem.anjay.AnjayInputContext;
import com.avsystem.anjay.AnjayObject;
import com.avsystem.anjay.AnjayOutputContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public final class DemoObject implements AnjayObject {
    private Optional<Integer> intValue = Optional.empty();
    private Optional<Long> longValue = Optional.empty();
    private Optional<Float> floatValue = Optional.empty();
    private Optional<Double> doubleValue = Optional.empty();
    private Optional<String> stringValue = Optional.empty();
    private Optional<Anjay.Objlnk> objlnkValue = Optional.empty();
    private Optional<byte[]> bytesValue = Optional.empty();
    private Map<Integer, Optional<String>> lastExecuteArgs = new HashMap<>();
    private Optional<Map<Integer, Integer>> multipleInstanceResource = Optional.empty();

    private final SortedSet<Integer> INSTANCES = new TreeSet<>(Arrays.asList(1));

    private final class Resource {
        public static final int INTEGER = 0;
        public static final int LONG = 1;
        public static final int FLOAT = 2;
        public static final int DOUBLE = 3;
        public static final int STRING = 4;
        public static final int OBJLNK = 5;
        public static final int BYTES = 6;
        public static final int EXECUTABLE = 7;
        public static final int LAST_EXECUTE_ARGS = 8;
        public static final int MULTIPLE = 9;
        public static final int INCREMENT_INTEGER = 10;
    }

    @Override
    public int oid() {
        return 1337;
    }

    @Override
    public SortedSet<Integer> instances() {
        return INSTANCES;
    }

    @Override
    public void instanceReset(int iid) {
        this.intValue = Optional.empty();
        this.longValue = Optional.empty();
        this.floatValue = Optional.empty();
        this.doubleValue = Optional.empty();
        this.stringValue = Optional.empty();
        this.objlnkValue = Optional.empty();
        this.bytesValue = Optional.empty();
        this.multipleInstanceResource = Optional.empty();
    }

    @Override
    public void instanceCreate(int iid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void instanceRemove(int iid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<ResourceDef> resources(int iid) {
        TreeSet<ResourceDef> resourceDefs = new TreeSet<>();
        resourceDefs.add(
                new ResourceDef(Resource.INTEGER, ResourceKind.RW, this.intValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.LONG, ResourceKind.RW, this.longValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.FLOAT, ResourceKind.RW, this.floatValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.DOUBLE, ResourceKind.RW, this.doubleValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.STRING, ResourceKind.RW, this.stringValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.OBJLNK, ResourceKind.RW, this.objlnkValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(Resource.BYTES, ResourceKind.RW, this.bytesValue.isPresent()));
        resourceDefs.add(new ResourceDef(Resource.EXECUTABLE, ResourceKind.E, true));
        resourceDefs.add(new ResourceDef(Resource.LAST_EXECUTE_ARGS, ResourceKind.RM, true));
        resourceDefs.add(
                new ResourceDef(
                        Resource.MULTIPLE,
                        ResourceKind.RWM,
                        this.multipleInstanceResource.isPresent()));
        resourceDefs.add(new ResourceDef(Resource.INCREMENT_INTEGER, ResourceKind.E, true));
        return resourceDefs;
    }

    @Override
    public void resourceExecute(int iid, int rid, Map<Integer, Optional<String>> args) {
        switch (rid) {
            case Resource.EXECUTABLE:
                this.lastExecuteArgs = args;
                break;

            case Resource.INCREMENT_INTEGER:
                if (this.intValue.isEmpty()) {
                    throw new AnjayException(
                            AnjayException.INTERNAL, "Integer resource not initialized");
                }
                this.intValue = Optional.of(this.intValue.get() + 1);
                break;
        }
    }

    @Override
    public void resourceReset(int iid, int rid) {
        assert rid == Resource.MULTIPLE;
        this.multipleInstanceResource = Optional.empty();
    }

    @Override
    public SortedSet<Integer> resourceInstances(int iid, int rid) {
        switch (rid) {
            case Resource.LAST_EXECUTE_ARGS:
                return new TreeSet<Integer>(this.lastExecuteArgs.keySet());
            case Resource.MULTIPLE:
                return new TreeSet<Integer>(this.multipleInstanceResource.get().keySet());
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void transactionBegin() {
        // TODO Auto-generated method stub

    }

    @Override
    public void transactionValidate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void transactionCommit() {
        // TODO Auto-generated method stub

    }

    @Override
    public void transactionRollback() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resourceWrite(int iid, int rid, AnjayInputContext context) {
        switch (rid) {
            case Resource.INTEGER:
                this.intValue = Optional.of(context.getInt());
                break;
            case Resource.LONG:
                this.longValue = Optional.of(context.getLong());
                break;
            case Resource.FLOAT:
                this.floatValue = Optional.of(context.getFloat());
                break;
            case Resource.DOUBLE:
                this.doubleValue = Optional.of(context.getDouble());
                break;
            case Resource.STRING:
                this.stringValue = Optional.of(context.getString());
                break;
            case Resource.OBJLNK:
                this.objlnkValue = Optional.of(context.getObjlnk());
                break;
            case Resource.BYTES:
                this.bytesValue = Optional.of(context.getAllBytes());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void resourceWrite(int iid, int rid, int riid, AnjayInputContext context) {
        switch (rid) {
            case Resource.MULTIPLE:
                if (!this.multipleInstanceResource.isPresent()) {
                    this.multipleInstanceResource = Optional.of(new HashMap<>());
                }
                this.multipleInstanceResource.get().put(riid, context.getInt());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void resourceRead(int iid, int rid, AnjayOutputContext context) {
        switch (rid) {
            case Resource.INTEGER:
                context.retInt(this.intValue.get());
                break;
            case Resource.LONG:
                context.retLong(this.longValue.get());
                break;
            case Resource.FLOAT:
                context.retFloat(this.floatValue.get());
                break;
            case Resource.DOUBLE:
                context.retDouble(this.doubleValue.get());
                break;
            case Resource.STRING:
                context.retString(this.stringValue.get());
                break;
            case Resource.OBJLNK:
                context.retObjlnk(this.objlnkValue.get());
                break;
            case Resource.BYTES:
                context.retBytes(this.bytesValue.get());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void resourceRead(int iid, int rid, int riid, AnjayOutputContext context) {
        switch (rid) {
            case Resource.LAST_EXECUTE_ARGS:
                context.retString(this.lastExecuteArgs.get(riid).orElse("<none>"));
                break;
            case Resource.MULTIPLE:
                context.retInt(this.multipleInstanceResource.get().get(riid));
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }
}
