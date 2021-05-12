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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Test {
    public static void main(String argv[]) throws Exception {
        // NOTE: for some reason Logger.getLogger("") returns more "global"
        // loger than Logger.getGlobal() is.
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINEST);
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(Level.FINEST);
        }

        DemoArgs args = new DemoArgs();
        JCommander cmd = JCommander.newBuilder().addObject(args).build();
        cmd.setProgramName("demo");
        try {
            cmd.parse(argv);
        } catch (ParameterException e) {
            logger.log(Level.SEVERE, e.toString());
            args.help = true;
        }
        if (args.help) {
            cmd.usage();
            return;
        }

        Thread thread = new Thread(new DemoClient(args));
        thread.start();
        thread.join();
    }
}
