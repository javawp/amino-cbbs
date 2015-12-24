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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.amino.util.RandomArrayGenerator;
import org.amino.util.Parallelized;
import org.amino.util.Parallelized.CheckFor;
import org.amino.util.Parallelized.InitFor;
import org.amino.util.Parallelized.ParallelSetting;
import org.amino.util.Parallelized.Threaded;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unittest of Queue.
 * @author Xiao Jun Dai
 *
 */
@RunWith(Parallelized.class)
@ParallelSetting(threadNumber = { 1, 2, 4, 6, 8})
public class QueueTest {
    private static final int NELEMENT = 512;
    Queue<String> queue;
    String[] testData = RandomArrayGenerator.getRandStringArray(NELEMENT);;
    List<String> polledData;

    @Before
    public void setUp() throws Exception {
        queue = new ConcurrentLinkedQueue<String>();
    }

    @Test
    public void testPollST() {
        for (int i = 0; i < testData.length; i++) {
            queue.offer(testData[i]);
        }

        for (int i = 0; i < NELEMENT; i++) {
        	assertEquals(testData[i], queue.peek());
            assertEquals(testData[i], queue.poll());
        }

        assertTrue(queue.isEmpty());
    }

    @Test
    public void testOfferST() {
        for (int i = 0; i < testData.length; i++) {
            queue.offer(testData[i]);
        }

        int count = 0;
        for (Iterator<String> i = queue.iterator(); i.hasNext();) {
            assertEquals(testData[count++], i.next());
        }
    }

    @Test
    public void testSizeST() {
        for (int i = 0; i < testData.length; i++) {
            queue.offer(testData[i]);
        }
        assertEquals(testData.length, queue.size());

        for (int i = 0; i < testData.length; i++) {
            assertNotNull(queue.poll());
        }
        assertEquals(0, queue.size());
    }

    @InitFor("testOfferMT")
    public void initForOfferMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
    }

    @Threaded
    public void testOfferMT(int threadId, int nThread) {
        for (int i = 0; i < NELEMENT; i++) {
            queue.offer(testData[NELEMENT * threadId + i]);
        }
    }

    @CheckFor("testOfferMT")
    public void checkForOfferMT(int nThread) {
        assertEquals(NELEMENT * nThread, queue.size());
        for (int i = 0; i < testData.length; i++) {
            assertTrue(queue.contains(testData[i]));
        }
    }

    @InitFor("testPollMT")
    public void initForPollMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
        polledData = new LockFreeList<String>();

        for (int i = 0; i < testData.length; i++) {
            queue.offer(testData[i]);
        }
    }

    @Threaded
    public void testPollMT(int threadId, int nThread) {
        String temp;
        for (int i = 0; i < NELEMENT; i++) {
            temp = queue.poll();
            assertNotNull(temp);
            polledData.add(temp);
        }
    }

    @CheckFor("testPollMT")
    public void checkForPollMT(int nThread) {
        assertNull(queue.poll());
        assertTrue(queue.isEmpty());

        assertEquals(testData.length, polledData.size());
        for (String data : testData) {
            assertTrue(polledData.contains(data));
        }
    }

    @InitFor("testEnDePairMT")
    public void initForEnDePairMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
        polledData = new LockFreeList<String>();
        queue = new ConcurrentLinkedQueue<String>();

        // for (int i = 0; i < testData.length; i++) {
        // queue.offer(testData[i]);
        // }
    }

    @Threaded
    public void testEnDePairMT(int threadId, int nThread) {
        if (threadId % 2 == 0) {// even threads
            for (int i = 0; i < NELEMENT; i++) {
                queue.offer(testData[NELEMENT * threadId + i]);
            }
        } else { // odd threads
            for (int i = 0; i < NELEMENT; i++) {
                String temp = queue.poll();
                if (temp != null) {
                    polledData.add(temp);
                }
            }
        }
    }

    @CheckFor("testEnDePairMT")
    public void checkForEnDePairMT(int nThread) {

        List<String> list = new ArrayList<String>();

        while (!queue.isEmpty()) {
            list.add(queue.poll());
        }


        assertEquals(NELEMENT * ((nThread + 1) / 2) - polledData.size(), list
                .size());
    }
}
