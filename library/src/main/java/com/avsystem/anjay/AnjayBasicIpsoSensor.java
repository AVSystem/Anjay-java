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

public final class AnjayBasicIpsoSensor implements AnjayObject {
    private final class Resource {
        public static final int MIN_MEASURED_VALUE = 5601;
        public static final int MAX_MEASURED_VALUE = 5602;
        public static final int MIN_RANGE_VALUE = 5603;
        public static final int MAX_RANGE_VALUE = 5604;
        public static final int RESET_MIN_AND_MAX_MEASURED_VALUES = 5605;
        public static final int SENSOR_VALUE = 5700;
        public static final int SENSOR_UNITS = 5701;
    }

    private final class Instance {
        private final int iid;

        private final String unit;
        private final Optional<Double> minRangeValue;
        private final Optional<Double> maxRangeValue;
        private double minMeasuredValue;
        private double maxMeasuredValue;
        private double currentValue;

        private final Supplier<Double> readValue;

        public Instance(
                Integer iid,
                String unit,
                Optional<Double> minRangeValue,
                Optional<Double> maxRangeValue,
                Supplier<Double> readValue) {
            this.iid = iid;
            this.unit = unit;
            this.minRangeValue = minRangeValue;
            this.maxRangeValue = maxRangeValue;
            this.readValue = readValue;

            currentValue = readValue.get();
            this.resetMinMaxMeasured();
        }

        public final String getUnit() {
            return unit;
        }

        public final Optional<Double> getMinRangeValue() {
            return minRangeValue;
        }

        public final Optional<Double> getMaxRangeValue() {
            return maxRangeValue;
        }

        public final void updateValues() {
            double newValue = readValue.get();

            if (newValue != currentValue) {
                currentValue = newValue;
                anjay.notifyChanged(oid, iid, Resource.SENSOR_VALUE);

                if (currentValue < minMeasuredValue) {
                    minMeasuredValue = Double.min(minMeasuredValue, currentValue);
                    anjay.notifyChanged(oid, iid, Resource.MIN_MEASURED_VALUE);
                }

                if (currentValue > maxMeasuredValue) {
                    maxMeasuredValue = currentValue;
                    anjay.notifyChanged(oid, iid, Resource.MAX_MEASURED_VALUE);
                }
            }
        }

        public final double getMinMeasuredValue() {
            updateValues();
            return minMeasuredValue;
        }

        public final double getMaxMeasuredValue() {
            updateValues();
            return maxMeasuredValue;
        }

        public final Double getValue() {
            updateValues();
            return currentValue;
        }

        public void resetMinMaxMeasured() {
            minMeasuredValue = currentValue;
            anjay.notifyChanged(oid, iid, Resource.MIN_MEASURED_VALUE);

            maxMeasuredValue = currentValue;
            anjay.notifyChanged(oid, iid, Resource.MAX_MEASURED_VALUE);
        }
    }

    private final Anjay anjay;
    private final int oid;

    private final Map<Integer, Instance> instances = new TreeMap<>();

    private AnjayBasicIpsoSensor(Anjay anjay, int oid) {
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
        resourceDefs.add(new ResourceDef(Resource.MIN_MEASURED_VALUE, ResourceKind.R, true));
        resourceDefs.add(new ResourceDef(Resource.MAX_MEASURED_VALUE, ResourceKind.R, true));
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
        resourceDefs.add(
                new ResourceDef(Resource.RESET_MIN_AND_MAX_MEASURED_VALUES, ResourceKind.E, true));
        resourceDefs.add(new ResourceDef(Resource.SENSOR_VALUE, ResourceKind.R, true));
        return resourceDefs;
    }

    @Override
    public synchronized void resourceRead(int iid, int rid, AnjayOutputContext context) {
        switch (rid) {
            case Resource.MIN_MEASURED_VALUE:
                context.retDouble(this.instances.get(iid).getMinMeasuredValue());
                break;
            case Resource.MAX_MEASURED_VALUE:
                context.retDouble(this.instances.get(iid).getMaxMeasuredValue());
                break;
            case Resource.MIN_RANGE_VALUE:
                context.retDouble(this.instances.get(iid).getMinRangeValue().get());
                break;
            case Resource.MAX_RANGE_VALUE:
                context.retDouble(this.instances.get(iid).getMaxRangeValue().get());
                break;
            case Resource.SENSOR_VALUE:
                context.retDouble(this.instances.get(iid).getValue());
                break;
            case Resource.SENSOR_UNITS:
                context.retString(this.instances.get(iid).getUnit());
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    @Override
    public synchronized void resourceExecute(
            int iid, int rid, Map<Integer, Optional<String>> args) {
        switch (rid) {
            case Resource.RESET_MIN_AND_MAX_MEASURED_VALUES:
                instances.get(iid).resetMinMaxMeasured();
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource " + rid);
        }
    }

    /**
     * Installs the Basic Ipso Sensor object in an Anjay object.
     *
     * @param anjay Anjay object for which the Basic Sensor is installed.
     * @param oid OID of the installed sensor.
     * @return {@link AnjayBasicIpsoSensor} object.
     */
    public static AnjayBasicIpsoSensor install(Anjay anjay, int oid) {
        AnjayBasicIpsoSensor newSensor = new AnjayBasicIpsoSensor(anjay, oid);
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
     * @param readValue Callback for reading the sensor value.
     */
    public synchronized void instanceAdd(
            int iid,
            String unit,
            Optional<Double> minRangeValue,
            Optional<Double> maxRangeValue,
            Supplier<Double> readValue) {
        if (instances.containsKey(iid)) {
            throw new InvalidParameterException("IID already in use");
        }

        instances.put(iid, new Instance(iid, unit, minRangeValue, maxRangeValue, readValue));
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
     * Updates a basic sensor object instance.
     *
     * @param iid IID of the updated instance.
     */
    public synchronized void update(int iid) {
        Instance inst = instances.get(iid);
        if (inst == null) {
            throw new IllegalArgumentException("Invalid IID");
        } else {
            inst.getValue();
        }
    }
}
