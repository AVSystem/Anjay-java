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

package com.avsystem.anjay.impl;

import com.avsystem.anjay.AnjayException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static int handleException(Throwable t) {
        LOGGER.log(Level.FINE, "Exception occurred", t);
        if (t instanceof AnjayException) {
            return ((AnjayException) t).errorCode();
        } else if (t instanceof UnsupportedOperationException) {
            return AnjayException.METHOD_NOT_ALLOWED;
        } else if (t instanceof IllegalStateException || t instanceof IllegalArgumentException) {
            return AnjayException.BAD_REQUEST;
        } else {
            return -1;
        }
    }

    private Utils() {}
}
