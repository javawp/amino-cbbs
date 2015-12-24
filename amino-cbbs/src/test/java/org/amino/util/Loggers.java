/*
 * Copyright (c) 2007 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.amino.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Xiao Jun Dai
 *
 */
public abstract class Loggers {
    private final static Logger performance = Logger.getLogger("performance");

    private final static Logger debug = Logger.getLogger("debug");

    static {
        String filePrefix = AbstractBaseTest.needLogFile();
        if (filePrefix != null) {
            try {
                Date date = new Date();
                SimpleDateFormat datef = new SimpleDateFormat("yyyyMMdd");

                FileHandler handlerPerf = new FileHandler("log/" + filePrefix
                        + "_" + datef.format(date) + ".txt");
                handlerPerf.setFormatter(new SimpleFormatter());

                FileHandler handlerDebug = new FileHandler("log/" + filePrefix
                        + "_" + datef.format(date) + "_debug.txt");
                handlerDebug.setFormatter(new SimpleFormatter());

                performance.addHandler(handlerPerf);
                debug.addHandler(handlerDebug);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void debug(String msg) {
        debug.info(msg);
    }

    public static void performance(String msg) {
        performance.info(msg);
    }

    public static void performance(String className, int nThread, int nElement,
            String funName, long nanoTime) {
        performance("class:\t" + className + "\tnthread:\t" + nThread
                + "\tnelement:\t" + nElement + "\ttestname:\t" + funName
                + "\ttakes\t" + nanoTime / 1000 + "\tmicroseconds");
    }

    public static void performance(Class<?> className, int nThread,
            int nElement, String funName, long nanoTime) {
        performance(className.getCanonicalName(), nThread, nElement, funName,
                nanoTime);
    }

}
