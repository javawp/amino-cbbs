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

import org.amino.util.AbstractBaseTest;
import org.amino.util.ThreadRunner;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Zhi Gan
 *
 */
public class VectorTest {

	private LockFreeVector<Integer> lfv;
	private int NELEMENT;
	private int NTHREAD;
	private ThreadRunner runner;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		lfv = new LockFreeVector<Integer>();
		NELEMENT = AbstractBaseTest.getElementNum();
		NTHREAD = AbstractBaseTest.getThreadNums().get(0);
		runner = ThreadRunner
				.getRunner(LockFreeVector.class, NTHREAD, NELEMENT);

		for (int i = 0; i < NELEMENT; i++) {
			lfv.push_back(i);
		}
	}

	/**
	 * Test method for
	 * {@link org.amino.ds.lockfree.LockFreeVector#push_back(java.lang.Object)}.
	 */
	@Test
	public void testPush_back() {
		for (int i = NELEMENT; i < 2 * NELEMENT; i++) {
			lfv.push_back(i);
		}
		assertEquals(2 * NELEMENT, lfv.size());
	}

	/**
	 * Test method for {@link org.amino.ds.lockfree.LockFreeVector#pop_back()}.
	 */
	@Test
	public void testPop_back() {
		for (int i = 0; i < NELEMENT; i++) {
			lfv.pop_back();
		}
		assertEquals(0, lfv.size());
	}

	/**
	 * Test method for {@link org.amino.ds.lockfree.LockFreeVector#get(int)}.
	 */
	@Test
	public void testGet() {
		for (int i = 0; i < NELEMENT; i++) {
			assertEquals(i, lfv.get(i));
		}
	}

	/**
	 * Test method for
	 * {@link org.amino.ds.lockfree.LockFreeVector#set(int, java.lang.Object)}.
	 */
	@Test
	public void testSet() {
		for (int i = 0; i < NELEMENT; i++) {
			lfv.set(i, NELEMENT - i);
		}

		for (int i = 0; i < NELEMENT; i++) {
			assertEquals(NELEMENT - i, lfv.get(i));
		}
	}

	@Test
	public void testSize() {
		assertEquals(lfv.size(), NELEMENT);
	}

	@Test
	public void concurrentSetGet() throws Throwable {
		Runnable[] tasks = new Runnable[NTHREAD];
		for (int i = 0; i < NTHREAD; i++) {
			final int index = i;
			tasks[i] = new Runnable() {

				public void run() {
					if (index % 2 == 0) {
						for (int i = 0; i < NELEMENT; i++) {
							lfv.set(i, i * i);
						}
					} else {
						for (int i = 0; i < NELEMENT; i++) {
							int vi = lfv.get(i);
							assertTrue(vi == i || vi == i * i);
						}
					}
				}
			};
		}

		runner.runThreads(tasks, "concurrentSetGet");

	}

	@Test
	public void concurrentPushPop() throws Throwable {
		Runnable[] tasks = new Runnable[NTHREAD];
		for (int i = 0; i < NTHREAD; i++) {
			tasks[i] = new Runnable() {

				public void run() {
					for (int i = 0; i < NELEMENT; i++) {
						lfv.push_back(i * i);
						lfv.pop_back();
					}
				}
			};
		}

		runner.runThreads(tasks, "concurrentPushPop");
		assertEquals(NELEMENT, lfv.size());
	}
}
