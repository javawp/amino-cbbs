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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * internal anchor type used to record the current status of deque.
 * 
 * @param <E>
 *            type of element on node
 */
class AnchorType<E> {
	private static final int STABLE = 0, RPUSH = 1, LPUSH = 2;

	DequeNode<E> right;
	DequeNode<E> left;
	volatile int status;
	int numElements;

	private static final AtomicIntegerFieldUpdater<AnchorType> statusUpdater = AtomicIntegerFieldUpdater
			.newUpdater(AnchorType.class, "status");

	/**
	 * @param oldStatus
	 *            old status expected
	 */
	public void stableStatus(int oldStatus) {
		statusUpdater.compareAndSet(this, oldStatus, STABLE);
	}

	/**
	 * default constructor.
	 */
	public AnchorType() {
	}

	/**
	 * @param r
	 *            right node
	 * @param l
	 *            left node
	 * @param st
	 *            status
	 * @param ne
	 *            number of element
	 */
	public AnchorType(DequeNode<E> r, DequeNode<E> l, int st, int ne) {
		setup(r, l, st, ne);
	}

	/**
	 * @param r
	 *            right node
	 * @param l
	 *            left node
	 * @param st
	 *            status
	 * @param ne
	 *            number of element
	 */
	void setup(DequeNode<E> r, DequeNode<E> l, int st, int ne) {
		right = r;
		left = l;
		status = st;
		numElements = ne;
	}

	public int getSize() {
		return this.numElements;
	}
}