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

package com.avsystem.anjay;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class AnjayIpsoButton implements AnjayObject {
    private final class Resource {
        public static final int DIGITAL_INPUT_STATE = 5500;
        public static final int DIGITAL_INPUT_COUNTER = 5501;
        public static final int APPLICATION_TYPE = 5750;
    }

    private class Instance {
        private final int iid;

        private String applicationType;
        private String applicationTypeBackup;
        private int counter = 0;
        private boolean pressed = false;

        public Instance(int iid, String application_type) {
            this.iid = iid;
            this.applicationType = application_type;
        }

        public int getCounter() {
            return counter;
        }

        public int getIid() {
            return iid;
        }

        public String getApplicationType() {
            return applicationType;
        }

        public void setApplicationType(String applicationType) {
            this.applicationType = applicationType;
        }

        public boolean isPressed() {
            return pressed;
        }

        public void setPressed(boolean pressed) {
            if (this.pressed != pressed) {
                this.pressed = pressed;
                if (this.pressed) {
                    this.counter++;
                    anjay.notifyChanged(BUTTON_OID, iid, Resource.DIGITAL_INPUT_COUNTER);
                }
                anjay.notifyChanged(BUTTON_OID, iid, Resource.DIGITAL_INPUT_STATE);
            }
        }
    }

    private static final Integer BUTTON_OID = 3347;

    private final Anjay anjay;
    private final Map<Integer, Instance> instances = new TreeMap<>();

    private AnjayIpsoButton(Anjay anjay) {
        this.anjay = anjay;
    }

    @Override
    public int oid() {
        return BUTTON_OID;
    }

    @Override
    public synchronized SortedSet<Integer> instances() {
        return new TreeSet<>(instances.keySet());
    }

    @Override
    public SortedSet<ResourceDef> resources(int iid) {
        TreeSet<ResourceDef> resourceDefs = new TreeSet<>();
        resourceDefs.add(new ResourceDef(Resource.APPLICATION_TYPE, ResourceKind.RW, true));
        resourceDefs.add(new ResourceDef(Resource.DIGITAL_INPUT_COUNTER, ResourceKind.R, true));
        resourceDefs.add(new ResourceDef(Resource.DIGITAL_INPUT_STATE, ResourceKind.R, true));
        return resourceDefs;
    }

    @Override
    public synchronized void resourceRead(int iid, int rid, AnjayOutputContext context) {
        switch (rid) {
            case Resource.APPLICATION_TYPE:
                context.retString(this.instances.get(iid).getApplicationType());
                break;
            case Resource.DIGITAL_INPUT_COUNTER:
                context.retInt(this.instances.get(iid).getCounter());
                break;
            case Resource.DIGITAL_INPUT_STATE:
                context.retBoolean(this.instances.get(iid).isPressed());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public synchronized void resourceWrite(int iid, int rid, AnjayInputContext context) {
        switch (rid) {
            case Resource.APPLICATION_TYPE:
                instances.get(iid).setApplicationType(context.getString());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public void transactionBegin() {
        for (Instance instance : instances.values()) {
            instance.applicationTypeBackup = instance.applicationType;
        }
    }

    @Override
    public void transactionCommit() {
        for (Instance instance : instances.values()) {
            instance.applicationTypeBackup = null;
        }
    }

    @Override
    public void transactionRollback() {
        for (Instance instance : instances.values()) {
            instance.applicationType = instance.applicationTypeBackup;
        }
    }

    @Override
    public void transactionValidate() {}

    /**
     * Installs the Ipso Button object in an Anjay object.
     *
     * @param anjay Anjay object for which the Basic Sensor is installed.
     * @return {@link AnjayIpsoButton} object.
     */
    public static AnjayIpsoButton install(Anjay anjay) {
        AnjayIpsoButton newButton = new AnjayIpsoButton(anjay);
        anjay.registerObject(newButton);
        return newButton;
    }

    /**
     * Adds an instance of a sensor object.
     *
     * @param iid IID of the added instance. Should be lower than the number of instances passed to
     *     the constructor.
     * @param applicationType Unit of the measured values.
     */
    public synchronized void instanceAdd(int iid, String applicationType) {
        if (instances.containsKey(iid)) {
            throw new InvalidParameterException("IID already in use");
        } else {
            instances.put(iid, new Instance(iid, applicationType));
            anjay.notifyInstancesChanged(BUTTON_OID);
        }
    }

    @Override
    public synchronized void instanceRemove(int iid) {
        if (instances.remove(iid) != null) {
            throw new IllegalArgumentException("Invalid IID");
        } else {
            anjay.notifyInstancesChanged(BUTTON_OID);
        }
    }

    /**
     * Updates a button object instance.
     *
     * @param iid IID of the updated instance.
     * @param pressed Determines if the button will be now pressed or not.
     */
    public synchronized void update(int iid, boolean pressed) {
        Instance inst = instances.get(iid);
        if (inst == null) {
            throw new IllegalArgumentException("Invalid IID");
        } else {
            inst.setPressed(pressed);
        }
    }
}
