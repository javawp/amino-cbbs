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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.amino.ds.Deque;
import org.amino.util.AbstractBaseTest;
import org.amino.util.ThreadRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Zhi Gan
 *
 */
@RunWith(Parameterized.class)
public class DequeTest extends AbstractBaseTest {

	@Parameters
	public static Collection paras() {
		List<Object[]> args = new ArrayList<Object[]>();

		args.addAll(genWorkLoad1(LockFreeDeque.class, new Object[] {}));
		args.addAll(genWorkLoad1(EBDeque.class, new Object[] {}));
		args.addAll(genWorkLoad1(EBDeque.class, new Object[] { -1 }));

		return args;
	}

	public DequeTest(Class classTested, Object[] params, int nthread,
			int nelement) {
		super(classTested, params, nthread, nelement);
	}

	public Deque<Long> defaultDeque;
	public Deque<Long> backoffDeque;

	boolean verbose = false;
	private ThreadRunner runner;

	@Before
	public void init() throws InstantiationException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException {
		defaultDeque = (Deque<Long>) getInstance();

		runner = ThreadRunner.getRunner(classTested, NTHREAD, NELEMENT);
	}

	@Test
	public void testAddAndPoll() throws Throwable {
		Thread[] threads = new Thread[NTHREAD];

		for (int threadID = 0; threadID < NTHREAD; threadID++) {
			threads[threadID] = new Thread("Thread-" + threadID) {
				public void run() {
					for (int i = 0; i < NELEMENT; i++) {
						assertTrue(defaultDeque.add((long) i));
						defaultDeque.addFirst((long) i);
					}
				}
			};
		}
		runner.runThreads(threads, "testAddSameElement");

		assertEquals(NTHREAD * NELEMENT * 2, defaultDeque.size());

		threads = new Thread[NTHREAD];
		for (int threadID = 0; threadID < NTHREAD; threadID++) {
			threads[threadID] = new Thread("Thread-" + threadID) {
				public void run() {
					for (int i = 0; i < NELEMENT; i++) {
						defaultDeque.poll();
						defaultDeque.pollLast();
					}
				}
			};
		}
		runner.runThreads(threads, "testRemoveSameElement");

		assertEquals(0, defaultDeque.size());
	}

	@Test
	public void testClear() {
		for (int threadID = 0; threadID < NTHREAD; threadID++) {
			for (int i = 0; i < NELEMENT; i++) {
				assertTrue(defaultDeque.add((long) i));
				defaultDeque.addFirst((long) i);
			}
		}
		defaultDeque.clear();
		assertEquals(0, defaultDeque.size());
		defaultDeque.offer(5L);
		assertEquals(5L, defaultDeque.pollFirst());
		defaultDeque.offer(6L);
		assertEquals(6L, defaultDeque.pollFirst());
		assertEquals(0, defaultDeque.size());
	}

	@Test(expected = NoSuchElementException.class)
	public void testEmptyRF() {
		Assert.assertNull(defaultDeque.peek());
		defaultDeque.remove();
	}

	@Test(expected = NoSuchElementException.class)
	public void testEmptyRL() {
		Assert.assertNull(defaultDeque.peekLast());
		defaultDeque.removeLast();
	}

	@Test(expected = NoSuchElementException.class)
	public void testGetFirst() {
		Assert.assertNull(defaultDeque.peekLast());
		defaultDeque.getFirst();
	}

	@Test(expected = NoSuchElementException.class)
	public void testGetLast() {
		Assert.assertNull(defaultDeque.peekLast());
		defaultDeque.getLast();
	}

	@Test
	public void testOffer(){
		defaultDeque.offer(0L);
		defaultDeque.offerFirst(0L);
	}

	@Test
	public void testElement(){
		defaultDeque.offer(0L);
		defaultDeque.element();
	}
}