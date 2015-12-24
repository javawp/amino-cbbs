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

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
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
 * An unbounded thread-safe linked list. A <tt>LockfreeList</tt> is an
 * appropriate choice when many threads will share access to a common
 * collection. This list does not permit <tt>null</tt> elements.
 *
 * <p>
 * This is a lock-free implementation intended for highly scalable add,
 * remove and contains which is thread safe. All method related to index is not
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
public class LockFreeList<E> extends AbstractSequentialList<E> implements
        List<E> {

    /**
     * Pointer to header node, initialized to a dummy node. The first actual
     * node is at head.getNext().
     */
    protected AtomicMarkableReference<Entry<E>> head;

    /**
     * size of the list.
     */
    protected final AtomicInteger size;

    /**
     * Constructs an empty list.
     */
    public LockFreeList() {
        head = new AtomicMarkableReference<Entry<E>>(null, false);
        size = new AtomicInteger(0);
    }

    /**
     * Constructs a list containing the elements of the specified collection, in
     * the order they are returned by the collection's iterator.
     *
     * @param c
     *            the collection of elements to initially contain
     */
    public LockFreeList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list.
     */
    public E getFirst() {
        if (size.get() == 0)
            throw new NoSuchElementException();

        return head.getReference().element;
    }

    /**
     * Adds the specified element to the head of this list.
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
        ori_add(0, e);
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation). Shifts the element currently at that position (if
     * any) and any subsequent elements to the right (adds one to their
     * indices).
     *
     * <p>
     * Not Thread Safe. The behavior of this operation is unspecified if the
     * list is modified while the operation is in progress.
     *
     */
    /**
      * {@inheritDoc}
      */
    public void add(int index, E e) {
        ori_add(size.get() - index, e);
    }

    private void ori_add(int index, E e) {
        // Input verification
        if (null == e || index < 0)
            throw new NullPointerException();

        // Create a new node
        final Entry<E> node = new Entry<E>(e);
        final ListStateHolder<E> holder = new ListStateHolder<E>();
        // Optimization: remove object creation in loop
        // node.next = new AtomicMarkableReference<Entry<E>>(null, false);

        while (true) {
            // Find right position
            find(index, holder);
            Entry<E> cur = holder.cur;

            // Set node->next
            // Optimization: remove object creation in loop
            node.next = new AtomicMarkableReference<Entry<E>>(cur, false);

            // Set prev->next = newNode
            if (holder.prev.compareAndSet(cur, node, false, false)) {
                size.incrementAndGet();
                return;
            }
            // Retry if CAS is not success
        }
    }

    /**
     * Appends all of the elements in the specified collection to the head of
     * this list in the reverse order. They are returned by the specified
     * collection's iterator
     *
     */
    /**
      * {@inheritDoc}
      */
    public boolean addAll(Collection<? extends E> c) {
        Iterator<? extends E> i = c.iterator();
        while (i.hasNext()) {
            if (!add(i.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inserts all of the elements in the specified collection into this list at
     * the specified position (optional operation). Shifts the element currently
     * at that position (if any) and any subsequent elements to the right
     * (increases their indices). The new elements will appear in this list in
     * the reverse order that they are returned by the specified collection's
     * iterator. The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress. (Note that
     * this will occur if the specified collection is this list, and it's
     * nonempty.)
     *
     * <p>
     * Not Thread Safe. The behavior of this operation is unspecified if the
     * list is modified while the operation is in progress.
     *
     */
    /**
      * {@inheritDoc}
      */
    public boolean addAll(int index, Collection<? extends E> c) {
        Iterator<? extends E> i = c.iterator();
        while (i.hasNext()) {
            add(index, i.next());
        }
        return true;
    }

    /**
     * Removes all of the elements from this list (optional operation). This
     * list will be empty after this call returns (unless it throws an
     * exception).
     */
    public void clear() {
        head = new AtomicMarkableReference<Entry<E>>(null, false);
        size.set(0);
    }

    /**
     * Returns true if this list contains the specified element. More formally,
     * returns true if and only if this list contains at least one element e
     * such that o.equals(e).
     *
     * <p>
     * Thread Safe
     *
     * @param o
     *            element whose presence in this list is to be tested.
     * @return true if this list contains the specified element.
     */
    public boolean contains(Object o) {
        ListStateHolder<E> holder = new ListStateHolder<E>();
        return find(o, head, holder).found;
    }

    /**
      * {@inheritDoc}
      */
    public boolean containsAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        while (e.hasNext())
            if (!contains(e.next()))
                return false;

        return true;
    }

    /**
     * Find position by index and keep information in holder
     *
     * <p>
     * Not Thread Safe. The behavior of this operation is unspecified if the
     * list is modified while the operation is in progress.
     *
     * @param ind
     *            index at which the specified element is to be inserted.
     * @param holder
     *            information of position found
     */
    private void find(int ind, ListStateHolder<E> holder) {
        AtomicMarkableReference<Entry<E>> prev;
        int index = holder.index;
        Entry<E> cur = holder.cur;
        Entry<E> next = holder.next;

        try_again: while (true) {
            prev = head;
            index = 0;
            cur = prev.getReference();

            try {
                while (index < ind) {
                    AtomicMarkableReference<Entry<E>> nextP = cur.next;
                    next = nextP.getReference();

                    if (!nextP.isMarked()) {
                        prev = cur.next;
                    } else {
                        // help remove a marked node
                        if (!prev.compareAndSet(cur, next, false, false)) {
                            continue try_again;
                        }
                    }
                    cur = next;
                    index++;
                }
            } catch (NullPointerException e) {
                holder.prev = prev;
                holder.cur = cur;
                holder.next = next;
                holder.index = index;
                holder.found = false;
                return;
            }

            holder.prev = prev;
            holder.cur = cur;
            holder.next = next;
            holder.index = index;
            return;
        }
    }

    /**
     * Find position by index and keep information in holder
     *
     * <p>
     * Thread Safe.
     *
     * @param o
     *            element whose presence in this list is to be tested.
     * @param start
     *            start position to find.
     * @param holder
     *            information of position found.
     *
     *  @return state holder of list
     */
    protected ListStateHolder<E> find(Object o,
            AtomicMarkableReference<Entry<E>> start, ListStateHolder<E> holder) {

        AtomicMarkableReference<Entry<E>> prev = holder.prev;
        int index = holder.index;
        Entry<E> cur = holder.cur;
        Entry<E> next = holder.next;

        try_again: while (true) {
            prev = start;
            index = 0;
            cur = prev.getReference();

            try {
                while (true) {
                    AtomicMarkableReference<Entry<E>> next1 = cur.next;

                    next = next1.getReference();

                    if (!next1.isMarked()) {
                        Object cKey = cur.element;
                        // Optimization: inline method continueCompare()
                        // if (cKey != o && !cKey.equals(o)) { // not continue
                        // // compare
                        // // return false;
                        // prev = next1;
                        // } else { // continue compare
                        // holder.found = true;
                        // holder.prev = prev;
                        // holder.cur = cur;
                        // holder.next = next;
                        // holder.index = index;
                        // return holder;
                        // // return true;
                        // }

                        if (!continueCompare(o, holder, cur))
                            prev = next1;
                        else {
                            holder.prev = prev;
                            holder.cur = cur;
                            holder.next = next;
                            holder.index = index;
                            return holder;
                        }
                    } else {
                        // help remove a marked node
                        if (!prev.compareAndSet(cur, next, false, false)) {
                            continue try_again;
                        }
                    }

                    // move forward
                    cur = next;
                    index++;
                }
            } catch (NullPointerException e) {
                // This exception will only happen when cur==null
                holder.prev = prev;
                holder.cur = cur;
                holder.next = next;
                holder.index = index;
                holder.found = false;
                return holder;
            }
        }
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * <p>
     * Not Thread Safe
     *
     * @param index
     *            index of element to return.
     * @return the element at the specified position in this list.
     */
    public E get(int index) {
        index = size.get() - index - 1;
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: "
                    + size);

        Iterator<E> iter = iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }

        return iter.next();
    }

    /**
     * Returns the index in this list of the first occurrence of the specified
     * element, or -1 if this list does not contain this element. More formally,
     * returns the lowest index i such that o.equals(get(i)), or -1 if there is
     * no such index.
     *
     * <p>
     * Not Thread Safe
     *
     * @param o
     *            element to search for.
     * @return the index in this list of the first occurrence of the specified
     *         element, or -1 if this list does not contain this element.
     */
    public int indexOf(Object o) {
        ListStateHolder<E> holder = new ListStateHolder<E>();
        return size.get() - 1 - find(o, head, holder).index;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return 0 == size.get();
    }

    /**
      * {@inheritDoc}
      */
    public Iterator<E> iterator() {
        return new ListItr();
    }

    /**
      * {@inheritDoc}
      */
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the first occurrence in this list of the specified element. If
     * this list does not contain the element, it is unchanged. More formally,
     * removes the element with the lowest index i such that o.equals(get(i))
     * (if such an element exists).
     *
     * @param o
     *            element to be removed from this list, if present.
     * @return true if this list contained the specified element.
     */
    public boolean remove(Object o) {
        return remove(o, head);
    }

    /**
     * @param o object
     * @param start start node
     * @return true if remove successful, otherwise false
     */
    protected boolean remove(Object o, AtomicMarkableReference<Entry<E>> start) {

        ListStateHolder<E> holder = new ListStateHolder<E>();
        while (true) {
            // Find right position
            find(o, start, holder);
            if (!holder.found) {
                return false;
            }
            Entry<E> next = holder.next;
            // remove node logically
            if (!holder.cur.next.compareAndSet(next, next, false, true)) {
                continue;
            }
            // remove node physically
            if (!holder.prev.compareAndSet(holder.cur, holder.next, false,
                    false)) {
                // retry from start
                // FIXME can be removed? because other threads will do the job
                // find(o, start, holder);
            }
            size.decrementAndGet();
            return true;
        }
    }

     /**
      * {@inheritDoc}
      */
    public boolean removeAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        while (e.hasNext()) {
            remove(e.next());
        }
        return true;
    }

    /**
      * {@inheritDoc}
      */
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public E set(int index, E e) {
        int sizef = size.get();
        if (null == e || index < 0 || index > sizef)
            throw new NullPointerException();
        index = sizef - index - 1;

        Entry<E> node = new Entry<E>(e);
        ListStateHolder<E> holder = new ListStateHolder<E>();

        while (true) {
            find(index, holder);
            node.next = new AtomicMarkableReference<Entry<E>>(holder.next,
                    false);
            // replace node with current node
            if (holder.prev.compareAndSet(holder.cur, node, false, false)) {
                size.incrementAndGet();
                return holder.cur.element;
            }
        }
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list.
     */
    public int size() {
        return size.get();
    }

    /**
      * {@inheritDoc}
      */
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    /**
      * {@inheritDoc}
      */
    public Object[] toArray() {
        List<E> l = new ArrayList<E>();
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            l.add(e.next());
        }
        return l.toArray();
    }

    /**
      * {@inheritDoc}
      */
    public <T> T[] toArray(T[] a) {
        List<E> l = new ArrayList<E>();
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            l.add(e.next());
        }
        return l.toArray(a);
    }

    /**
     * @param o object
     * @param holder state holder
     * @param cur current node
     * @return true if continuing compare
     */
    protected boolean continueCompare(Object o, ListStateHolder<E> holder,
            Entry<E> cur) {
        Object cKey = cur.element;
        if (cKey != o && !cKey.equals(o)) {
            return false;
        } else {
            holder.found = true;
            return true;
        }
    }

    /**
     * internal node definition.
     *
     * @param <E> type of element on node
     */
    protected static class Entry<E> {
        E element;
        AtomicMarkableReference<Entry<E>> next;

        /**
         * @param element default value of element
         */
        public Entry(E element) {
            this.element = element;
            next = null;
        }
    }

    /**
     * iterator definition of list.
     *
     */
    private class ListItr implements Iterator<E> {
        private final ListStateHolder<E> holder = new ListStateHolder<E>();

        ListItr() {
            holder.prev = head;
            holder.cur = head.getReference();
        }

        private E advance() {
            // at the end of list
            Entry<E> cur = holder.cur;
            if (null == cur) {
                return null;
            }

            E curItem = cur.element;

            holder.next = cur.next.getReference();

            if (!cur.next.isMarked()) {
                holder.prev = cur.next;
            } else {
                // help remove a marked node
                holder.prev.compareAndSet(cur, holder.next, false, false);
            }
            holder.cur = holder.next;

            return curItem;
        }

        public boolean hasNext() {
            return null != holder.cur;
        }

        public E next() {
            E result = advance();

            if (null == result)
                throw new NoSuchElementException();
            else {
                return result;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * hold state between two function calls. It take place of thread local
     * variable because the performance of thread local variable is not good
     *
     * @param <E> type of element on node
     */
    protected static class ListStateHolder<E> {
        boolean found; // Found the node with the expected element
        int index = -1; // Record the
        // index
        // of the iteration
        AtomicMarkableReference<Entry<E>> prev; // Pointer to the
        // previous node
        Entry<E> cur; // Pointer to the current node
        Entry<E> next; // Pointer to the next node
    }
}
