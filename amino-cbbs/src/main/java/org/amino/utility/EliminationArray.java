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

package org.amino.utility;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A global elimination array class for several data structures. Two arrays are
 * created to store different tasks. It can be used to stack, deque, and even
 * list. The algorithm comes from following paper, but not exactly the same.
 * <p/>
 * 
 * <pre>
 * A Scalable Lock-free Stack Algorithm
 * Danny Hendler                Nir Shavit            Lena Yerushalmi
 * School of Computer Science Tel-Aviv University &amp; School of Computer Science
 *  Tel-Aviv University     Sun Microsystems           Tel-Aviv University
 *  Tel Aviv, Israel 69978      Laboratories          Tel Aviv, Israel 69978
 *  hendlerd@post.tau.ac.il    shanir@sun.com          lenay@post.tau.ac.il
 * </pre>
 * 
 * @author Zhi Gan (ganzhi@gmail.com)
 */
public class EliminationArray implements IEliminationArray {
	private static final boolean COUNT = false;
	private AtomicInteger succ, fail;
	private AtomicReferenceArray add_list;
	private AtomicReferenceArray remove_list;
	private int arraySize;
	private final int lookahead;

	private static Object TOMB_STONE = new Object();
	static Object REMOVED = new Object();

	/**
	 * dump for debug.
	 */
	public void dump() {
		if (succ != null && fail != null)
			System.out.println("" + succ.get() + " " + fail.get());
	}

	/**
	 * @param arraySize
	 *            Size of internal array
	 */
	public EliminationArray(int arraySize) {
		this.arraySize = arraySize;
		lookahead = 4;
		this.add_list = new AtomicReferenceArray(arraySize);
		this.remove_list = new AtomicReferenceArray(arraySize);

		if (COUNT) {
			this.succ = new AtomicInteger(0);
			this.fail = new AtomicInteger(0);
		}
	}

	/**
	  * {@inheritDoc}
	  */
	public boolean tryAdd(Object obj, int backOff) throws InterruptedException {
		int start = poorManRand();
		for (int i = 0; i < lookahead; i++) {
			int index = (start + i) % arraySize;
			Object remove_obj = remove_list.get(index);
			if (remove_obj == TOMB_STONE) {
				if (remove_list.compareAndSet(index, TOMB_STONE, obj)) {
					if (COUNT)
						succ.incrementAndGet();
					return true;
				}
			} else {
				Object old_add = add_list.get(index);
				if (old_add == null) {
					if (add_list.compareAndSet(index, null, obj)) {
						Thread.sleep(backOff);
						while (true) {
							Object new_add = add_list.get(index);

							if (new_add == obj) {
								if (add_list.compareAndSet(index, obj, null)) {
									if (COUNT)
										fail.incrementAndGet();

									return false;
								}
							} else {
								assert new_add == REMOVED;
								if (COUNT)
									succ.incrementAndGet();
								add_list.set(index, null);
								return true;
							}
						}
					}
				}
			}
		}

		Thread.sleep(backOff);
		if (COUNT)
			fail.incrementAndGet();
		return false;
	}

	/**
	  * {@inheritDoc}
	  */
	public Object tryRemove(int backOff) throws InterruptedException {
		int start = poorManRand();
		for (int i = 0; i < lookahead; i++) {
			int index = (start + i) % arraySize;

			Object obj_add = add_list.get(index);
			if (obj_add == null || obj_add == REMOVED) {
				Object old_remove = remove_list.get(index);
				if (old_remove == null) {
					if (remove_list.compareAndSet(index, null, TOMB_STONE)) {
						Thread.sleep(backOff);
						while (true) {
							Object new_remove = remove_list.get(index);
							if (new_remove != TOMB_STONE) {
								if (COUNT)
									succ.incrementAndGet();

								remove_list.set(index, null);
								return new_remove;
							} else {
								if (remove_list.compareAndSet(index,
										TOMB_STONE, null)) {
									if (COUNT)
										fail.incrementAndGet();
									return null;
								}
							}
						}
					}
				}
			} else {
				if (add_list.compareAndSet(index, obj_add, REMOVED)) {
					if (COUNT)
						succ.incrementAndGet();
					return obj_add;
				}
			}
		}

		Thread.sleep(backOff);
		if (COUNT)
			fail.incrementAndGet();
		return null;
	}

	private static int seed = 5;

	private int poorManRand() {
		seed = (seed * 12000 + 5) % 24001;
		return seed;
	}
}
