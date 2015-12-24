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

package org.amino.ds.lockfree;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.amino.ds.lockfree.LockFreeDictionary;
import org.amino.util.AbstractBaseTest;
import org.amino.util.ThreadRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Zhi Gan (ganzhi@gmail.com)
 *
 */
@RunWith(Parameterized.class)
public class MapTest extends AbstractBaseTest {
    private ThreadRunner runner;
    private Map<Integer, Integer> mapInt;
    private Map<String, String> mapStr;

    @Parameters
    public static Collection<Object[]> maps() {
        List<Object[]> args = new ArrayList<Object[]>();

        args.addAll(genWorkLoad1(LockFreeDictionary.class,
                new Object[] { }));

        return args;
    }

    public MapTest(Object setclass, Object[] params, int nthread,
            int nelement) {
        super(setclass, params, nthread, nelement);
    }

    @Before
    public void setUp() throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        mapStr = (Map<String, String>) getInstance();

        runner = ThreadRunner.getRunner(classTested, NTHREAD, NELEMENT);
    }

    private void readWriteTest(final int readcount, final int writecount,
            String testName) throws Throwable {
        for (int threadID = 0; threadID < NTHREAD; threadID++) {
            if (threadID % 2 == 0) {
                for (int i = 0; i < NELEMENT; i++) {
                    String key = "test " + (i + threadID * NELEMENT);
                    Assert.assertNull(mapStr.put(key, key));
                }
            }
        }
        // ((LockFreeSet)setStr).dump();

        Runnable[] tasks = new Runnable[NTHREAD];
        for (int threadID = 0; threadID < NTHREAD; threadID++) {
            final int index = threadID;
            tasks[threadID] = new Runnable() {
                public void run() {
                    for (int j = 0; j < AbstractBaseTest.getRepeatCount(); j++) {
                        for (int i = 0; i < NELEMENT; i++) {
                            String genKey = "test " + (i + index * NELEMENT);
                            if (i % (readcount + writecount) < readcount) {
                                mapStr.get(genKey);
                            } else if (index % 2 == 0) {
                                mapStr.remove(genKey);
                            } else {
                                mapStr.put(genKey, genKey);
                            }
                        }
                    }
                }
            };
        }

        runner.runThreads(tasks, testName);
    }

    @Test
    public void read95Write5() throws Throwable {
        readWriteTest(19, 1, "read95Write5");
    }
}
