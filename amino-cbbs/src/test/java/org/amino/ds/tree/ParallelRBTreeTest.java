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

package org.amino.ds.tree;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.amino.ds.tree.RelaxedRBTree.Node;
import org.amino.pattern.internal.Doable;
import org.amino.util.Parallelized;
import org.amino.util.Parallelized.CheckFor;
import org.amino.util.Parallelized.InitFor;
import org.amino.util.Parallelized.ParallelSetting;
import org.amino.util.Parallelized.Threaded;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unittest of ParallelRBTree.
 *
 * @author Xiao Jun Dai
 *
 */
@RunWith(Parallelized.class)
@ParallelSetting(threadNumber = { 1, 2, 4 })
public class ParallelRBTreeTest {

	private static final int NELEMENT = 1000;
	RelaxedRBTree<Integer> prbt;
	Random rand = new Random();

	@Before
	public void setUp() throws Exception {
		prbt = new RelaxedRBTree<Integer>();
	}

	@Threaded
	public void testInsert(int threadId, int nThread) {
		for (int i = threadId * NELEMENT; i < NELEMENT + threadId * NELEMENT; i++) {
			// System.out.println("Inserted " + i);
			prbt.insert(Integer.valueOf(i));
		}
	}

	@CheckFor("testInsert")
	public void checkInsert(int nThread) {
		for (int i = 0; i < NELEMENT; i++) {
			assertTrue("Didn't find " + i, prbt.find(Integer.valueOf(i)));
		}

		// assertTrue(prbt.verifyRBTree());
		prbt.shutdown();
	}

	@InitFor("testRemove")
	public void initRemove(int nThread) {
		for (int i = 0; i < NELEMENT; i++) {
			prbt.insert(Integer.valueOf(i));
		}
	}

	@Threaded
	public void testRemove(int threadId, int nThread) {
		for (int i = 0; i < NELEMENT; i++) {
			prbt.remove(Integer.valueOf(i));
		}
		assertTrue(prbt.verifyRBTree());
	}

	@CheckFor("testRemove")
	public void checkRemove(int nThread) {
		for (int i = 0; i < NELEMENT; i++) {
			assertFalse("Didn't find " + i, prbt.find(Integer.valueOf(i)));
		}

		// assertTrue(prbt.verifyRBTree());
		prbt.shutdown();
	}

	@Test
	public void testWalk() {
		for (int i = 0; i < NELEMENT; i++) {
			prbt.insert(Integer.valueOf(i));
		}

		prbt.inOrderWalk(new Doable<Integer, Integer>() {

			public Integer run(Integer input) {
				System.out.println(input);
				return null;
			}

		});
	}

	@Test
	public void testSearchNode(){
		for (int i = 0; i < NELEMENT; i++) {
			prbt.insert(Integer.valueOf(i));
		}
		Node<Integer> resNode = prbt.search(NELEMENT/2);
		assertEquals(resNode.getValue(), NELEMENT/2);
	}
}
