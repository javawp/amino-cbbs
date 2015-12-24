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

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import org.amino.ds.Deque;

/**
 * This Deque implementation is based on the algorithm defined in the follwoing
 * paper: CAS-Based Lock-Free Algorithm for Shared Deques By Maged M. Michael
 *
 * <p>
 * As the terminology between this implementation differs from the Deque
 * interface defined in Java 6, please translate left = last and right = first.
 *
 *
 * Right => First, Left => Last
 *
 * @author raja
 *
 * @param <E> type of element in the deque
 */

public class LockFreeDeque<E> extends AbstractQueue<E> implements Deque<E> {
    private static final int STABLE = 0, RPUSH = 1, LPUSH = 2;

    /**
     * Returns head (first element) of this deque, but does not remove it.
     *
     * @return head of this deque.
     * @throws NoSuchElementException
     */
    public E element() {
        return getFirst();
    }

    /**
     * Inserts element e at the tail of this deque. Preferable to
     * <code>addLast(E)</code>.
     *
     * @param e
     *            element to insert
     * @return true if the element was successfully added, false otherwise.
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * Returns the first element of this deque, or null if deque is empty.
     * Element is not removed.
     *
     * @return head of this deque, or null if deque is empty.
     */
    public E peek() {
        return peekFirst();
    }

    /**
     * Returns and removes first (head) element of this deque, or null if this
     * deque is empty.
     *
     * @return head of this deque, or null if empty.
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Returns and removes the first element in this deque.
     *
     * @return the head of this deque.
     */
    public E remove() {
        return removeFirst();
    }

    /**
      * {@inheritDoc}
      */
    public Iterator<E> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public E getFirst() {
        if (isEmpty())
            throw new NoSuchElementException();
        return peekFirst();
    }

    /**
      * {@inheritDoc}
      */
    public E getLast() {
        if (isEmpty())
            throw new NoSuchElementException();
        return peekLast();
    }

    /**
      * {@inheritDoc}
      */
    public boolean offerFirst(E e) {
        try {
            addFirst(e);
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    /**
      * {@inheritDoc}
      */
    public boolean offerLast(E e) {
        try {
            addLast(e);
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    /**
      * {@inheritDoc}
      */
    public E removeFirst() {
        E result = pollFirst();
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    /**
      * {@inheritDoc}
      */
    public boolean removeFirstOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public E removeLast() {
        E result = pollLast();
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    /**
      * {@inheritDoc}
      */
    public boolean removeLastOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts element e at the end of the deque.
     *
     * @param e
     *            element being added to the end of the deque.
     */
    /**
      * {@inheritDoc}
      */
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * Removes all elements, clears the deque.
     */
    public void clear() {
        AnchorType<E> oanchor, nanchor;
        // int ostamp, nstamp;

        while (true) {
            // oanchor = anchor.getReference();
            // ostamp = anchor.getStamp();
            oanchor = anchor.get();

            // Already empty?
            if (oanchor.right == null)
                return;

            // Create a new anchor with empty left/right pointers
            nanchor = new AnchorType<E>(null, null, STABLE, 0);

            // Replace the anchor with this new anchor
            // if (anchor.compareAndSet(oanchor, nanchor, ostamp, nstamp))
            // return;
            if (anchor.compareAndSet(oanchor, nanchor))
                return;

            if (backoff) {
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Contains function is not thread safe.
     *
     * @param o
     *            is the Object we are looking for.
     * @throws UnsupportedOperationException
     */
    /**
      * {@inheritDoc}
      */
    public boolean contains(Object o) {
        E element;
        Iterator<E> itr = new DeqIterator();
        while (itr.hasNext()) {
            element = itr.next();
            if (element.equals(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the deque is empty or not.
     *
     * @return true if deque is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Iterators not supported.
     *
     * @throws UnsupportedOperationException
     */
    /**
      * {@inheritDoc}
      */
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    /**
     * Remove first occurrence of specified object o in this deque.
     *
     * @param o
     *            element to be removed, if present.
     * @return true if an element was removed.
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * @return size of the deque
     */
    public int size() {
        return anchor.get().numElements;
    }

    // private AtomicStampedReference<AnchorType<E>> anchor = new
    // AtomicStampedReference<AnchorType<E>>(new AnchorType<E>(), 1);
    private AtomicReference<AnchorType<E>> anchor = new AtomicReference<AnchorType<E>>(
            new AnchorType<E>());

    final static boolean backoff = false;

    /*
     * This is the method that does a right push into the Deque. It takes the
     * data as input, creates a deque node with the data and then pushes it onto
     * the deque from right.
     */
    /**
     *
      * {@inheritDoc}
      */
    public void addFirst(E d) {
        DequeNode<E> newtop;
        AnchorType<E> oanchor;
        AnchorType<E> nanchor = new AnchorType<E>();
        // int oStamp, n1Stamp, n2Stamp;

        // Create new stack node with the data
        newtop = new DequeNode<E>(d);

        while (true) {
            // oanchor = anchor.getReference();
            // oStamp = anchor.getStamp();
            oanchor = anchor.get();

            if (oanchor.right == null) // deque is empty
            {
                // Create a the new anchor
                nanchor.setup(newtop, newtop, oanchor.status, 1);
                /*
                 * n1anchor.right = newtop; n1anchor.left = newtop;
                 * n1anchor.numElements = 1;; n1anchor.status = oanchor.status;
                 */
                // n1Stamp = oStamp + 1;
                // replace the anchor with this new anchor
                if (anchor.compareAndSet(oanchor, nanchor))
                    return;
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n1anchor, oStamp, n1Stamp))
                // return;
            } else if (oanchor.status == STABLE) {
                // set the newtop left pointer to the old anchor right pointer
                newtop.setLeft(oanchor.right);

                // create a new anchor with the right pointer pointing to the
                // new node
                // and the left pointer pointing to the old anchor left
                nanchor.setup(newtop, oanchor.left, RPUSH,
                        oanchor.numElements + 1);
                /*
                 * n2anchor.left = oanchor.left; n2anchor.right = newtop;
                 * n2anchor.numElements = oanchor.numElements + 1;
                 * n2anchor.status = RPUSH;
                 */
                // n2Stamp = oStamp + 1;
                // replace the anchor with this new anchort
                if (anchor.compareAndSet(oanchor, nanchor))
                // if(anchor.compareAndSet(oanchor, n2anchor, oStamp, n2Stamp))
                {
                    // now have to call stabilize
                    this.stabilizeRight(nanchor);
                    return;

                }
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
            } else {
                // deque is not in a stable state
                this.stabilize(oanchor);
            }
        }
    }

    /**
      * {@inheritDoc}
      */
    public E peekFirst() {
        AnchorType<E> oanchor = anchor.get();
        if (oanchor.right == null)
            return null;
        return oanchor.right.data;
    }

    /*
     * This is the method to pop the Right node from the Deque
     */
    /**
      * {@inheritDoc}
      */
    public E pollFirst() {
        DequeNode<E> prev;
        AnchorType<E> oanchor;
        AnchorType<E> nanchor = new AnchorType<E>();
        // int oStamp, n1Stamp, n2Stamp;

        while (true) {
            // oanchor = anchor.getReference();
            // oStamp = anchor.getStamp();
            oanchor = anchor.get();

            // deque is empty
            if (oanchor.right == null)
                return null;

            // deque has just one node
            if (oanchor.right == oanchor.left) {
                // Create a the new anchor
                nanchor.setup(null, null, oanchor.status, 0);
                /*
                 * n1anchor.right = null; n1anchor.left = null;
                 * n1anchor.numElements = 0; n1anchor.status = oanchor.status;
                 */
                // n1Stamp = oStamp + 1;
                if (anchor.compareAndSet(oanchor, nanchor))
                    break;
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n1anchor, oStamp, n1Stamp))
                // break;
            } else if (oanchor.status == STABLE) {
                // get the node previous to the right top
                prev = oanchor.right.left.get();

                // create a new anchor with the right pointer pointing to the
                // prev node
                // and the left pointer pointing to the old anchor left
                nanchor.setup(prev, oanchor.left, oanchor.status,
                        oanchor.numElements - 1);
                /*
                 * n2anchor.left = oanchor.left; n2anchor.right = prev;;
                 * n2anchor.numElements = oanchor.numElements - 1;
                 * n2anchor.status = oanchor.status;
                 */
                // n2Stamp = oStamp + 1;
                if (anchor.compareAndSet(oanchor, nanchor)) {
                    prev.right.compareAndSet(oanchor.right, null); // prevent
                    // memory
                    // leak
                    break;
                }
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n2anchor, oStamp, n2Stamp))
                // break;
            } else
                // deque is not stable
                stabilize(oanchor);
        }

        return oanchor.right.data;
    }

    /*
     * This is the method that does a left push into the Deque. It takes the
     * data as input, creates a deque node with the data and then pushes it onto
     * the deque from left.
     */
    /**
      * {@inheritDoc}
      */
    public void addLast(E d) {
        DequeNode<E> newtop;
        AnchorType<E> oanchor;
        AnchorType<E> nanchor = new AnchorType<E>();

        newtop = new DequeNode<E>(d);

        while (true) {
            // oanchor = anchor.getReference();
            // oStamp = anchor.getStamp();
            oanchor = anchor.get();

            if (oanchor.left == null) // deque is empty
            {
                nanchor.setup(newtop, newtop, oanchor.status, 1);

                if (anchor.compareAndSet(oanchor, nanchor))
                    return;
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n1anchor, oStamp, n1Stamp))
                // return;
            } else if (oanchor.status == STABLE) {
                // set the newtop right pointer to the old anchor left pointer
                newtop.setRight(oanchor.left);

                // create a new anchor with the right pointer pointing to the
                // new node
                // and the left pointer pointing to the old anchor left
                nanchor.setup(oanchor.right, newtop, LPUSH,
                        oanchor.numElements + 1);
                /*
                 * n2anchor.left = newtop; n2anchor.right = oanchor.right;
                 * n2anchor.numElements = oanchor.numElements + 1;
                 * n2anchor.status = LPUSH;
                 */
                // n2Stamp = oStamp + 1;
                if (anchor.compareAndSet(oanchor, nanchor))
                // if(anchor.compareAndSet(oanchor, n2anchor, oStamp, n2Stamp))
                {
                    // need to call stabilize routine
                    this.stabilizeLeft(nanchor);
                    return;

                }
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
            } else {
                // deque is not in a stable state
                this.stabilize(oanchor);
            }
        }

    }

    /**
      * {@inheritDoc}
      */
    public E peekLast() {
        AnchorType<E> oanchor;
        // oanchor = anchor.getReference();
        oanchor = anchor.get();
        if (oanchor.left == null)
            return null;
        return oanchor.left.data;
    }

    /*
     * This is the method to pop the Left node from the Deque
     */
    /**
      * {@inheritDoc}
      */
    public E pollLast() {
        AnchorType<E> oanchor;
        AnchorType<E> nanchor = new AnchorType<E>();
        // int oStamp, n1Stamp, n2Stamp;

        while (true) {
            // oanchor = anchor.getReference();
            // oStamp = anchor.getStamp();
            oanchor = anchor.get();

            // deque is empty
            if (oanchor.right == null)
                return null;

            // deque has just one node
            if (oanchor.right == oanchor.left) {
                nanchor.setup(null, null, oanchor.status, 0);
                /*
                 * n1anchor.right = null; n1anchor.left = null;
                 * n1anchor.numElements = 0; n1anchor.status = oanchor.status;
                 */
                // n1Stamp = oStamp + 1;
                if (anchor.compareAndSet(oanchor, nanchor))
                    break;
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n1anchor, oStamp, n1Stamp))
                // break;
            } else if (oanchor.status == STABLE) {
                // get the node previous to the right top
                DequeNode<E> prev = oanchor.left.right.get();

                // create a new anchor with the right pointer pointing to the
                // old anchor
                // right and the left pointer pointing to prev node
                nanchor.setup(oanchor.right, prev, oanchor.status,
                        oanchor.numElements - 1);
                /*
                 * n2anchor.left = prev; n2anchor.right = oanchor.right;
                 * n2anchor.numElements = oanchor.numElements - 1;
                 * n2anchor.status = oanchor.status;
                 */
                // n2Stamp = oStamp + 1;
                if (anchor.compareAndSet(oanchor, nanchor)) {
                    prev.left.compareAndSet(oanchor.left, null); // prevent
                    // memory
                    // leakage
                    break;
                }
                if (backoff) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }
                // if(anchor.compareAndSet(oanchor, n2anchor, oStamp, n2Stamp))
                // break;
            } else
                // deque is not stable
                stabilize(oanchor);
        }

        return oanchor.left.data;

    }

    /*
     * The base routine to stabilize the deque. It calls the StabilizeRight or
     * StabilizeLeft based on the status flag
     */
    private void stabilize(AnchorType<E> a) {
        if (a.status == RPUSH)
            stabilizeRight(a);
        else if (a.status == LPUSH)
            // status = LPUSH
            stabilizeLeft(a);
    }

    /*
     * Stabilize the deque after a right push
     */
    private void stabilizeRight(AnchorType<E> a) {
        DequeNode<E> prev, prevnext;
        // int Stamp = anchor.getStamp();

        prev = a.right.left.get();
        // if(!(anchor.getReference() == a)) return;
        if (a.status != RPUSH)
            return;
        prevnext = prev.right.get();
        if (prevnext != a.right) {
            // if (!(anchor.getReference() == a)) return;
            if (a.status != RPUSH)
                return;
            if (!prev.right.compareAndSet(prevnext, a.right))
                return;
        }

        a.stableStatus(RPUSH);
    }

    /*
     * Stabilize the deque after a left push
     */
    private void stabilizeLeft(AnchorType<E> a) {
        DequeNode<E> prev, prevnext;
        // int Stamp = anchor.getStamp();

        prev = a.left.right.get();
        // if (!(anchor.getReference() == a)) return;
        if (a.status != LPUSH)
            return;
        prevnext = prev.left.get();
        if (prevnext != a.left) {
            // if (!(anchor.getReference() == a)) return;
            if (a.status != LPUSH)
                return;
            if (!prev.left.compareAndSet(prevnext, a.left))
                return;
        }

        a.stableStatus(LPUSH);
    }

    /**
     * @return element popped
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * @param e element pushed
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Iterator definition of deque.
     *
     */
    private class DeqIterator implements Iterator<E> {

        private DequeNode<E> cursor = anchor.get().left;

        public boolean hasNext() {
            return cursor != null;
        }

        public E next() {
            if (cursor == null)
                throw new NoSuchElementException();

            E result = cursor.data;
            cursor = cursor.right.get();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
