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
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

public final class Anjay3dIpsoSensor implements AnjayObject {
    public static final class Coordinates {
        private final Double x;
        private final Double y;
        private final Double z;

        public Coordinates(Double x, Double y, Double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Double getX() {
            return x;
        }

        public Double getY() {
            return y;
        }

        public Double getZ() {
            return z;
        }
    }

    private final class Resource {
        public static final int MIN_RANGE_VALUE = 5603;
        public static final int MAX_RANGE_VALUE = 5604;
        public static final int SENSOR_UNITS = 5701;
        public static final int SENSOR_X_VALUE = 5702;
        public static final int SENSOR_Y_VALUE = 5703;
        public static final int SENSOR_Z_VALUE = 5704;
    }

    private final class Instance {
        private final int iid;

        private final Optional<Double> minRangeValue;
        private final Optional<Double> maxRangeValue;
        private final String unit;
        private Coordinates currentValues;

        private final Supplier<Coordinates> readValues;

        public Instance(
                Integer iid,
                String unit,
                Optional<Double> minRangeValue,
                Optional<Double> maxRangeValue,
                Supplier<Coordinates> readValues) {
            this.iid = iid;
            this.unit = unit;
            this.minRangeValue = minRangeValue;
            this.maxRangeValue = maxRangeValue;

            this.readValues = readValues;
            currentValues = readValues.get();
        }

        public String getUnit() {
            return unit;
        }

        public Optional<Double> getMinRangeValue() {
            return minRangeValue;
        }

        public Optional<Double> getMaxRangeValue() {
            return maxRangeValue;
        }

        private void updateValues() {
            Coordinates oldValues = currentValues;
            currentValues = readValues.get();

            if (oldValues.getX() != currentValues.getX()) {
                anjay.notifyChanged(oid, iid, Resource.SENSOR_X_VALUE);
            }
            if (oldValues.getY() != currentValues.getY()) {
                anjay.notifyChanged(oid, iid, Resource.SENSOR_Y_VALUE);
            }
            if (oldValues.getZ() != currentValues.getZ()) {
                anjay.notifyChanged(oid, iid, Resource.SENSOR_Z_VALUE);
            }
        }

        public Double getXValue() {
            updateValues();
            return currentValues.getX();
        }

        public Double getYValue() {
            updateValues();
            return currentValues.getY();
        }

        public Double getZValue() {
            updateValues();
            return currentValues.getZ();
        }
    }

    private final Anjay anjay;
    private final int oid;

    private final Map<Integer, Instance> instances = new TreeMap<>();

    private Anjay3dIpsoSensor(Anjay anjay, int oid) {
        this.anjay = anjay;
        this.oid = oid;
    }

    @Override
    public int oid() {
        return oid;
    }

    @Override
    public synchronized SortedSet<Integer> instances() {
        return new TreeSet<>(instances.keySet());
    }

    @Override
    public synchronized SortedSet<ResourceDef> resources(int iid) {
        TreeSet<ResourceDef> resourceDefs = new TreeSet<>();
        resourceDefs.add(
                new ResourceDef(
                        Resource.MIN_RANGE_VALUE,
                        ResourceKind.R,
                        this.instances.get(iid).minRangeValue.isPresent()));
        resourceDefs.add(
                new ResourceDef(
                        Resource.MAX_RANGE_VALUE,
                        ResourceKind.R,
                        this.instances.get(iid).maxRangeValue.isPresent()));
        resourceDefs.add(new ResourceDef(Resource.SENSOR_UNITS, ResourceKind.R, true));
        resourceDefs.add(new ResourceDef(Resource.SENSOR_X_VALUE, ResourceKind.R, true));
        resourceDefs.add(new ResourceDef(Resource.SENSOR_Y_VALUE, ResourceKind.R, true));
        resourceDefs.add(new ResourceDef(Resource.SENSOR_Z_VALUE, ResourceKind.R, true));
        return resourceDefs;
    }

    @Override
    public synchronized void resourceRead(int iid, int rid, AnjayOutputContext context) {
        switch (rid) {
            case Resource.MIN_RANGE_VALUE:
                context.retDouble(this.instances.get(iid).getMinRangeValue().get());
                break;
            case Resource.MAX_RANGE_VALUE:
                context.retDouble(this.instances.get(iid).getMaxRangeValue().get());
                break;
            case Resource.SENSOR_UNITS:
                context.retString(this.instances.get(iid).getUnit());
                break;
            case Resource.SENSOR_X_VALUE:
                context.retDouble(this.instances.get(iid).getXValue());
                break;
            case Resource.SENSOR_Y_VALUE:
                context.retDouble(this.instances.get(iid).getYValue());
                break;
            case Resource.SENSOR_Z_VALUE:
                context.retDouble(this.instances.get(iid).getZValue());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    /**
     * Installs the Three Axis Ipso Sensor object in an Anjay object.
     *
     * @param anjay Anjay object for which the Three Axis Sensor is installed.
     * @param oid OID of the installed sensor.
     * @return {@link Anjay3dIpsoSensor} object.
     */
    public static Anjay3dIpsoSensor install(Anjay anjay, int oid) {
        Anjay3dIpsoSensor newSensor = new Anjay3dIpsoSensor(anjay, oid);
        anjay.registerObject(newSensor);
        return newSensor;
    }

    /**
     * Adds an instance of a sensor object.
     *
     * @param iid IID of the added instance. Should be lower than the number of instances passed to
     *     the constructor.
     * @param unit Unit of the measured values.
     * @param minRangeValue The minimum value that can be measured by the sensor. If it is empty the
     *     resource won't be created.
     * @param maxRangeValue The maximum value that can be measured by the sensor. If it is empty the
     *     resource won't be created.
     * @param readValues Callback for reading the current set of values for the 3 axis sensor.
     */
    public synchronized void instanceAdd(
            int iid,
            String unit,
            Optional<Double> minRangeValue,
            Optional<Double> maxRangeValue,
            Supplier<Coordinates> readValues) {
        if (instances.containsKey(iid)) {
            throw new InvalidParameterException("IID already in use");
        }

        instances.put(iid, new Instance(iid, unit, minRangeValue, maxRangeValue, readValues));
        anjay.notifyInstancesChanged(oid);
    }

    @Override
    public synchronized void instanceRemove(int iid) {
        if (instances.remove(iid) != null) {
            anjay.notifyInstancesChanged(oid);
        } else {
            throw new IllegalArgumentException("Invalid IID");
        }
    }

    /**
     * Updates a three axis sensor object instance.
     *
     * @param iid IID of the updated instance.
     */
    public synchronized void update(int iid) {
        Instance inst = instances.get(iid);
        if (inst == null) {
            throw new IllegalArgumentException("Invalid IID");
        } else {
            inst.updateValues();
        }
    }
}
