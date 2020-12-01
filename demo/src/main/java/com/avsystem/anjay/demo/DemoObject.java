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

package com.avsystem.anjay.demo;

import com.avsystem.anjay.Anjay;
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
        resourceDefs.add(new ResourceDef(0, ResourceKind.RW, this.intValue.isPresent()));
        resourceDefs.add(new ResourceDef(1, ResourceKind.RW, this.longValue.isPresent()));
        resourceDefs.add(new ResourceDef(2, ResourceKind.RW, this.floatValue.isPresent()));
        resourceDefs.add(new ResourceDef(3, ResourceKind.RW, this.doubleValue.isPresent()));
        resourceDefs.add(new ResourceDef(4, ResourceKind.RW, this.stringValue.isPresent()));
        resourceDefs.add(new ResourceDef(5, ResourceKind.RW, this.objlnkValue.isPresent()));
        resourceDefs.add(new ResourceDef(6, ResourceKind.RW, this.bytesValue.isPresent()));
        resourceDefs.add(new ResourceDef(7, ResourceKind.E, true));
        resourceDefs.add(new ResourceDef(8, ResourceKind.RM, true));
        resourceDefs.add(
                new ResourceDef(9, ResourceKind.RWM, this.multipleInstanceResource.isPresent()));
        return resourceDefs;
    }

    @Override
    public void resourceExecute(int iid, int rid, Map<Integer, Optional<String>> args) {
        this.lastExecuteArgs = args;
    }

    @Override
    public void resourceReset(int iid, int rid) {
        assert rid == 9;
        this.multipleInstanceResource = Optional.empty();
    }

    @Override
    public SortedSet<Integer> resourceInstances(int iid, int rid) {
        switch (rid) {
            case 8:
                return new TreeSet<Integer>(this.lastExecuteArgs.keySet());
            case 9:
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
            case 0:
                this.intValue = Optional.of(context.getInt());
                break;
            case 1:
                this.longValue = Optional.of(context.getLong());
                break;
            case 2:
                this.floatValue = Optional.of(context.getFloat());
                break;
            case 3:
                this.doubleValue = Optional.of(context.getDouble());
                break;
            case 4:
                this.stringValue = Optional.of(context.getString());
                break;
            case 5:
                this.objlnkValue = Optional.of(context.getObjlnk());
                break;
            case 6:
                this.bytesValue = Optional.of(context.getAllBytes());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void resourceWrite(int iid, int rid, int riid, AnjayInputContext context) {
        switch (rid) {
            case 9:
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
            case 0:
                context.retInt(this.intValue.get());
                break;
            case 1:
                context.retLong(this.longValue.get());
                break;
            case 2:
                context.retFloat(this.floatValue.get());
                break;
            case 3:
                context.retDouble(this.doubleValue.get());
                break;
            case 4:
                context.retString(this.stringValue.get());
                break;
            case 5:
                context.retObjlnk(this.objlnkValue.get());
                break;
            case 6:
                context.retBytes(this.bytesValue.get());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void resourceRead(int iid, int rid, int riid, AnjayOutputContext context) {
        switch (rid) {
            case 8:
                context.retString(this.lastExecuteArgs.get(riid).orElse("<none>"));
                break;
            case 9:
                context.retInt(this.multipleInstanceResource.get().get(riid));
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }
}
