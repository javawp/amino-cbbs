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

import java.util.EmptyStackException;
import java.util.concurrent.atomic.AtomicReference;

import org.amino.utility.EliminationArray;

/**
 * @author Zhi Gan (ganzhi@gmail.com)
 * 
 * @param <E>
 *            Type of elements
 */
public class EBStack<E> implements IStack<E> {
	/**
	 * BACKOFF in milli second.
	 */
	private static final int BACKOFF = 6;
	// AtomicStampedReference<Node<E>> top = new
	// AtomicStampedReference<Node<E>>(null,1);
	AtomicReference<Node<E>> top = new AtomicReference<Node<E>>(null);
	EliminationArray ea;

	/**
	 * default constructor.
	 */
	public EBStack() {
		ea = new EliminationArray(8);
	}

	/**
	 * dump array.
	 */
	public void dump() {
		ea.dump();
	}

	/**
	 * Specify size of internal elimination array.
	 * 
	 * @param size
	 *            default size of stack
	 */
	public EBStack(int size) {
		ea = new EliminationArray(size);
	}

	final static boolean backoff = true;

	/**
	 * Node definition for stack.
	 * 
	 * @param <E>
	 *            type of element in node
	 */
	static class Node<E> {
		final E data;
		Node<E> next;

		/**
		 * @param d
		 *            data
		 */
		public Node(E d) {
			super();
			this.data = d;
		}

	}

	/**
	 * Pop data from the Stack.
	 * 
	 * @return topmost element of the stack.
	 */
	public E pop() {
		Node<E> oldTop, newTop;
		// int oldStamp, newStamp;
		int exp;
		if (backoff)
			exp = 1;
		while (true) {
			// oldStamp = top.getStamp();
			// oldTop = top.getReference();
			oldTop = top.get();
			if (oldTop == null)
				throw new EmptyStackException();
			newTop = oldTop.next;
			// newStamp = oldStamp + 1;
			// if(top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) break;
			if (top.compareAndSet(oldTop, newTop))
				break;
			if (backoff) {
				Object res;
				try {
					res = ea.tryRemove(BACKOFF * exp);
					if (res != null)
						return (E) res;
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
				exp = exp << 1;
			}
		}

		return oldTop.data;
	}

	/**
	 * Push data onto Stack.
	 * 
	 * @param d
	 *            data to be pushed onto the stack.
	 */
	public void push(E d) {
		Node<E> oldTop, newTop;
		// int oldStamp, newStamp;

		newTop = new Node<E>(d);
		int exp;
		if (backoff)
			exp = 1;
		while (true) {
			// oldStamp = top.getStamp();
			// oldTop = top.getReference();
			oldTop = top.get();
			newTop.next = oldTop;
			// newStamp = oldStamp + 1;
			// if(top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) return;
			if (top.compareAndSet(oldTop, newTop))
				return;
			if (backoff) {
				try {
					if (ea.tryAdd(d, BACKOFF))
						return;
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
				exp = exp << 1;
			}
		}
	}

	/**
	 * Check to see if Stack is empty.
	 * 
	 * @return true if stack is empty.
	 */
	public boolean isEmpty() {
		// if (top.getReference() == null)
		if (top.get() == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Return copy of the top data on the Stack.
	 * 
	 * @return copy of top of stack, or null if empty.
	 */
	public E peek() {
		// if (top.getReference() == null)
		if (top.get() == null) {
			return null;
		} else {
			// return top.getReference().data;
			return top.get().data;
		}
	}
}
