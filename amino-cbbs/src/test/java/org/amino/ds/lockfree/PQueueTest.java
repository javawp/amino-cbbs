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
import java.util.Arrays;
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
 * Unittest of Priority Queue.
 * @author Xiao Jun Dai
 *
 */
@RunWith(Parallelized.class)
@ParallelSetting(threadNumber = { 1, 2, 4, 6, 8})
public class PQueueTest {
    private static final int NELEMENT = 512;
    Queue<String> pqueue;
    String[] testData = RandomArrayGenerator.getRandStringArray(NELEMENT);
    String[] sortedTestData = Arrays.copyOf(testData, testData.length);

    List<String> polledData;

    @Before
    public void setUp() throws Exception {
        pqueue = new LockFreePriorityQueue<String>();
        Arrays.sort(sortedTestData);
    }

    @Test
    public void testPollST() {
        for (int i = 0; i < testData.length; i++) {
            pqueue.offer(testData[i]);
        }

        for (int i = 0; i < NELEMENT; i++) {
            assertEquals(sortedTestData[i], pqueue.poll());
        }

        assertTrue(pqueue.isEmpty());
    }

    @Test
    public void testSizeST() {
        for (int i = 0; i < testData.length; i++) {
            pqueue.offer(testData[i]);
        }
        assertEquals(testData.length, pqueue.size());

        for (int i = 0; i < testData.length; i++) {
            assertNotNull(pqueue.poll());
        }
        assertEquals(0, pqueue.size());
    }

    @InitFor("testOfferMT")
    public void initForOfferMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
    }

    @Threaded
    public void testOfferMT(int threadId, int nThread) {
        for (int i = 0; i < NELEMENT; i++) {
            pqueue.offer(testData[NELEMENT * threadId + i]);
        }
    }

    @CheckFor("testOfferMT")
    public void checkForOfferMT(int nThread) {
        assertEquals(NELEMENT * nThread, pqueue.size());
        for (int i = 0; i < testData.length; i++) {
            assertTrue(pqueue.contains(testData[i]));
        }
    }

    @InitFor("testPollMT")
    public void initForPollMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
        polledData = new LockFreeList<String>();

        for (int i = 0; i < testData.length; i++) {
            pqueue.offer(testData[i]);
        }
    }

    @Threaded
    public void testPollMT(int threadId, int nThread) {
        String temp;
        for (int i = 0; i < NELEMENT; i++) {
            temp = pqueue.poll();
            assertNotNull(temp);
            polledData.add(temp);
        }
    }

    @CheckFor("testPollMT")
    public void checkForPollMT(int nThread) {
        assertNull(pqueue.poll());
        assertTrue(pqueue.isEmpty());

        assertEquals(testData.length, polledData.size());
        for (String data : testData) {
            assertTrue(polledData.contains(data));
        }
    }

    @InitFor("testEnDePairMT")
    public void initForEnDePairMT(int nThread) {
        testData = RandomArrayGenerator.getRandStringArray(NELEMENT * nThread);
        polledData = new LockFreeList<String>();
        pqueue = new ConcurrentLinkedQueue<String>();
    }

    @Threaded
    public void testEnDePairMT(int threadId, int nThread) {
        if (threadId % 2 == 0) {// even threads
            for (int i = 0; i < NELEMENT; i++) {
                pqueue.offer(testData[NELEMENT * threadId + i]);
            }
        } else { // odd threads
            for (int i = 0; i < NELEMENT; i++) {
                String temp = pqueue.poll();
                if (temp != null) {
                    polledData.add(temp);
                }
            }
        }
    }

    @CheckFor("testEnDePairMT")
    public void checkForEnDePairMT(int nThread) {
        List<String> list = new ArrayList<String>();

        while (!pqueue.isEmpty()) {
            list.add(pqueue.poll());
        }

        assertEquals(NELEMENT * ((nThread + 1) / 2) - polledData.size(), list
                .size());
    }
}
