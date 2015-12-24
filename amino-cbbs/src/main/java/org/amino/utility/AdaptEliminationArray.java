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

/**
 * A global elimination array class for several data structures. Two arrays are
 * created to store different tasks. And it will dynamically change size of
 * elimination array according to match rate. It can be used to queue, stack,
 * deque, and even list. The algorithm comes from follow paper, but not exactly
 * the same. <p/>
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
public class AdaptEliminationArray implements IEliminationArray {
	private volatile int succ, fail;
	private AtomicReferenceArray add_list;
	private AtomicReferenceArray remove_list;
	private volatile int arraySize;
	private final int lookahead;

	private static Object TOMB_STONE = new Object();
	static Object REMOVED = new Object();

	/**
	 * dump for debug.
	 */
	public void dump() {
		System.out.println("" + succ + " " + fail);
	}

	/**
	 * @param arraySize
	 *            the average size of internal array. Size of internal array
	 *            will vary between 1 and 2*arraySize
	 */
	public AdaptEliminationArray(int arraySize) {
		this.arraySize = arraySize;
		if (arraySize < 4)
			lookahead = arraySize;
		else
			lookahead = 4;
		this.add_list = new AtomicReferenceArray(arraySize * 2);
		this.remove_list = new AtomicReferenceArray(arraySize * 2);
	}

	/**
	  * {@inheritDoc}
	  */
	public boolean tryAdd(Object obj, int backOff) throws InterruptedException {
		adjustArraySize();

		int start = poorManRand();
		for (int i = 0; i < lookahead; i++) {
			int index = (start + i) % arraySize;
			Object remove_obj = remove_list.get(index);
			if (remove_obj == TOMB_STONE) {
				if (remove_list.compareAndSet(index, TOMB_STONE, obj)) {
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
									fail++;

									return false;
								}
							} else {
								assert new_add == REMOVED;
								succ++;
								add_list.set(index, null);
								return true;
							}
						}
					}
				}
			}
		}

		Thread.sleep(backOff);
		fail++;
		return false;
	}

	private void adjustArraySize() {
		final int cycle = 200;
		if (fail > cycle) {
			int tmp = arraySize;
			if (succ < (cycle >> 2))
				tmp >>= 1;
			else if (succ > cycle)
				tmp <<= 1;
			else {
				fail = 0;
				succ = 0;
				System.out.println("NO change succ " + succ + " fail " + fail
						+ " arraySize " + arraySize);
				return;
			}

			if (tmp < 2)
				tmp = 2;
			if (tmp > add_list.length())
				tmp = add_list.length();
			arraySize = tmp;

			System.out.println("succ " + succ + " fail " + fail + " arraySize "
					+ arraySize);

			fail = 0;
			succ = 0;
		}
	}

	/**
	  * {@inheritDoc}
	  */
	public Object tryRemove(int backOff) throws InterruptedException {
		adjustArraySize();

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
								succ++;

								remove_list.set(index, null);
								return new_remove;
							} else {
								if (remove_list.compareAndSet(index,
										TOMB_STONE, null)) {
									fail++;
									return null;
								}
							}
						}
					}
				}
			} else {
				if (add_list.compareAndSet(index, obj_add, REMOVED)) {
					return obj_add;
				}
			}
		}

		Thread.sleep(backOff);
		fail++;
		return null;
	}

	private static int seed = 5;

	private int poorManRand() {
		seed = (seed * 12000 + 5) % 24001;
		return seed;
	}
}
