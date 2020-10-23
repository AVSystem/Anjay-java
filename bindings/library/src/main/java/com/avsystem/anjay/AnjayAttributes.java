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

package com.avsystem.anjay;

import com.avsystem.anjay.impl.NativeAttrStorage;

/**
 * Class containing definitions of Attributes for Objects, Instances, Resources and Resource
 * Instances.
 */
public final class AnjayAttributes {
    /** Attributes for Objects and Object Instances. */
    public static final class ObjectInstanceAttrs {
        /** Minimum Period as defined by LwM2M spec */
        public int minPeriod;
        /** Maximum Period as defined by LwM2M spec */
        public int maxPeriod;
        /** Minimum Evaluation Period as defined by LwM2M spec */
        public int minEvalPeriod;
        /** Maximum Evaluation Period as defined by LwM2M spec */
        public int maxEvalPeriod;

        /** Creates attributes object with no attributes set. */
        public ObjectInstanceAttrs() {
            this.minPeriod = PERIOD_NONE;
            this.maxPeriod = PERIOD_NONE;
            this.minEvalPeriod = PERIOD_NONE;
            this.maxEvalPeriod = PERIOD_NONE;
        }

        /**
         * Creates attributes object with given attributes values.
         *
         * @param minPeriod Minimum Period as defined by LwM2M spec
         * @param maxPeriod Maximum Period as defined by LwM2M spec
         * @param minEvalPeriod Minimum Evaluation Period as defined by LwM2M spec
         * @param maxEvalPeriod Maximum Evaluation Period as defined by LwM2M spec
         */
        public ObjectInstanceAttrs(
                int minPeriod, int maxPeriod, int minEvalPeriod, int maxEvalPeriod) {
            this.minPeriod = minPeriod;
            this.maxPeriod = maxPeriod;
            this.minEvalPeriod = minEvalPeriod;
            this.maxEvalPeriod = maxEvalPeriod;
        }
    }

    /** Attributes for Resources and Resource Instances. */
    public static final class ResourceAttrs {
        /** Attributes shared with Objects/Object Instances */
        public ObjectInstanceAttrs common;
        /** Greater Than attribute as defined by LwM2M spec */
        public double greaterThan;
        /** Less Than attribute as defined by LwM2M spec */
        public double lessThan;
        /** Step attribute as defined by LwM2M spec */
        public double step;

        /** Creates attributes object with no attributes set. */
        public ResourceAttrs() {
            this.common = new ObjectInstanceAttrs();
            this.greaterThan = VALUE_NONE;
            this.lessThan = VALUE_NONE;
            this.step = VALUE_NONE;
        }

        /**
         * Creates attributes object with given attributes values.
         *
         * @param common Attributes shared with Objects/Object Instances
         * @param greaterThan Greater Than attribute as defined by LwM2M spec
         * @param lessThan Less Than attribute as defined by LwM2M spec
         * @param step Step attribute as defined by LwM2M spec
         */
        public ResourceAttrs(
                ObjectInstanceAttrs common, double greaterThan, double lessThan, double step) {
            this.common = common;
            this.greaterThan = greaterThan;
            this.lessThan = lessThan;
            this.step = step;
        }
    }

    /** A value indicating that the Min/Max Period attribute is not set. */
    public static final int PERIOD_NONE = NativeAttrStorage.getAttrPeriodNone();

    /** A value indicating that the Less Than/Greater Than/Step attribute is not set. */
    public static final double VALUE_NONE = NativeAttrStorage.getAttrValueNone();
}
