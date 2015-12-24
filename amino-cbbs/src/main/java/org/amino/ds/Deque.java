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

package org.amino.ds;

import java.util.Iterator;
import java.util.Queue;

/**
 * Deque interface definition from Java 6
 * See java.util.Deque Java 6 API documentation for more information.
 *
 * @author blainey
 *
 * @param <E>
 */
public  interface Deque<E> extends Queue<E> {
	/**
	 * Inserts element e into the front of the deque.
	 * @param e element being added
	 */
	void addFirst (E e);

	/**
	 * Inserts element ea at the end of the deque.
	 * @param ea element being added to the end of the deque.
	 */
	void addLast (E ea);

	/**
	 * Returns an iterator over this deque which traverses the elements from last
	 * to first.
	 * @return an iterator over this deque.
	 */
	Iterator<E> descendingIterator();
	/**
	 * Returns head (first element) of this deque, but does not remove it.
	 * @return head of this deque.
	 * @throws NoSuchElementException
	 */
	E getFirst();


	/**
	 * Returns tail (last element) of this deque, but does not remove it.
	 * @return tail of this deque.
	 * @throws NoSuchElementException
	 */
	E getLast();


	/**
	 * Inserts element e at the front of this deque.  Preferable to
	 *  <code>addFirst(E)</code>.
	 * @param e element to insert.
	 * @return true if the element was successfully added, false otherwise.
	 */
	boolean offerFirst (E e);


	/**
	 * Inserts element e at the tail of this deque.  Preferable
	 * to <code>addLast(E)</code>.
	 * @param e element to insert
	 * @return true if the element was successfully added, false otherwise.
	 */
	boolean offerLast (E e);


	/**
	 * Returns the first element of this deque, or null if deque is empty.
	 * Element is not removed.
	 * @return head of this deque, or null if deque is empty.
	 */
	E peekFirst();


	/**
	 * Returns the last element of this deque, or null if deque is empty.
	 * Element is not removed.
	 * @return tail of this deque, or null if deque is empty.
	 */
	E peekLast();


	/**
	 * Returns and removes first (head) element of this deque, or null if
	 * this deque is empty.
	 * @return head of this deque, or null if empty.
	 */
	E pollFirst();

	/**
	 *Returns and removes last (tail) element of this deque, or null if
	 * this deque is empty.
	 * @return tail of this deque, or null if empty.
	 */
	E pollLast();


	/**
	 * Returns and removes the first element in this deque.
	 * @return the head of this deque.
	 */
	E removeFirst();


	/**
	 * Returns and removes the last element in this deque.
	 * @return the tail of this deque.
	 */
	E removeLast();


	/**
	 * Remove first occurrence of specified object o in this deque.
	 * @param o element to be removed, if present.
	 * @return true if an element was removed.
	 */
	boolean removeFirstOccurrence (Object o);

	/**
	  * Remove last occurrence of specified object o in this deque.
	 * @param o element to be removed, if present.
	 * @return true if an element was removed.
	 */
	boolean removeLastOccurrence (Object o);
}
