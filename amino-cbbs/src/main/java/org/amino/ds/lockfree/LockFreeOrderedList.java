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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 *
 * This implementation employs an efficient &quot;lock-free&quot; algorithm
 * based on one described in <a
 * href="http://www.research.ibm.com/people/m/michael/spaa-2002.pdf"> High
 * Performance Dynamic Lock-Free Hash Tables and List-Based Sets</a> by Maged
 * M. Michael.
 *
 * <p>
 * An unbounded thread-safe linked list which its element is ordered.
 *
 * A <tt>LockFreeOrderedList</tt> is an appropriate choice when many threads
 * will share access to a common collection. This list does not permit
 * <tt>null</tt> elements. All elements in the list is ordered according to
 * compare()
 *
 * <p>
 * This is a lock-free implementation intended for highly scalable add,
 * remove and contains which is thread safe. All mothed related to index is not
 * thread safe. Add() will add the element to the head of the list which is
 * different with the normal list.
 *
 *
 * @author Xiao Jun Dai
 *
 * @param <E>
 *            the type of elements held in this collection
 *
 */
public class LockFreeOrderedList<E> extends LockFreeList<E> {
	/**
	 * Creates a <tt>LockfreeList</tt> that is initially empty.
	 */
	public LockFreeOrderedList() {
		super();
	}

	/**
	 * Creates a <tt>LockfreeList</tt> initially containing the elements of
	 * the given collection, added in traversal order of the collection's
	 * iterator.
	 *
	 * @param c
	 *            the collection of elements to initially contain
	 */
	public LockFreeOrderedList(Collection<? extends E> c) {
		this();
		addAll(c);
	}

	private boolean add(E e, AtomicMarkableReference<Entry<E>> start) {
		// Create a new node
		Entry<E> node = new Entry<E>(e);
		ListStateHolder<E> holder = new ListStateHolder<E>();

		while (true) {
			find(e, start, holder);

			Entry<E> cur = holder.cur;
			node.next = new AtomicMarkableReference<Entry<E>>(cur, false);

			if (holder.prev.compareAndSet(cur, node, false, false)) {
				size.incrementAndGet();
				return true;
			}
		}
	}

	/**
	 * Adds the specified element to this list.
	 *
	 * <p>
	 * Thread Safe
	 *
	 * @param e
	 *            the element to add.
	 * @return <tt>true</tt> (as per the general contract of
	 *         <tt>Collection.add</tt>).
	 *
	 */
	public boolean add(E e) {
		return add(e, head);
	}

	/**
	  * {@inheritDoc}
	  */
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	/**
	  * {@inheritDoc}
	  */
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	/**
	  * {@inheritDoc}
	  */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean continueCompare(Object o, ListStateHolder<E> holder,
			Entry<E> cur) {
		Comparable cKey = (Comparable) cur.element;
		int cr = cKey.compareTo(o);

		if (cr < 0)
			return false;
		else if (cr > 0) {
			holder.found = false;
			return true;
		} else if (cr == 0 && (cKey == o || cKey.equals(o))) {
			holder.found = true;
			return true;
		} else
			return false;
	}
}
