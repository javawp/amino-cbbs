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

package org.amino.mcas;

import sun.misc.Unsafe;

/**
 * Lockfree binary search tree. It is based on Practical lock-freedom by Keir
 * Fraser. Implemented by MCAS.
 *
 * @param <T>
 *            Type of key
 * @param <V>
 *            Type value
 *
 * @author Xiao Jun Dai
 */
public class LockFreeBSTree<T, V> {
    private static final Unsafe unsafe = UnsafeWrapper.getUnsafe();

    private Node<T, V> root;

    private final Node<T, V> maxDummyNode = new ThreadNode<T, V>(
            new Node<T, V>());

    private final Node<T, V> minDummyNode = new ThreadNode<T, V>(
            new Node<T, V>());

    /**
     * internal node definition of tree.
     *
     * @param <T> type of key in node
     * @param <V> type of value in node
     */
    private static class Node<T, V> extends ObjectID {
        T key; // immutable
        V value;
        ThreadNode<T, V> tnode;
        volatile Node<T, V> left;
        volatile Node<T, V> right;

        static final long VALUE_OFFSET;
        static final long LEFT_OFFSET;
        static final long RIGHT_OFFSET;

        static {
            try {
                VALUE_OFFSET = unsafe.objectFieldOffset(Node.class
                        .getDeclaredField("value"));
                LEFT_OFFSET = unsafe.objectFieldOffset(Node.class
                        .getDeclaredField("left"));
                RIGHT_OFFSET = unsafe.objectFieldOffset(Node.class
                        .getDeclaredField("right"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Node() {
            key = null;
            value = null;
            left = null;
            right = null;
            tnode = null;
        }

        Node(T key, V value) {
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            tnode = null;
        }

        public boolean isThread() {
            return this instanceof ThreadNode;
        }

        public Node<T, V> thread() {
            if (null == tnode) {
                tnode = new ThreadNode<T, V>(this);
            }
            return tnode;
        }

        public Node<T, V> unthread() {
            return ((ThreadNode<T, V>) this).node;
        }
    }

    /**
     * Definition of threaded node use to point to the root of tree.
     *
     * @param <T> type of key in node
     * @param <V> type of value in node
     */
    private static class ThreadNode<T, V> extends Node<T, V> {
        Node<T, V> node;

        ThreadNode(Node<T, V> node) {
            this.node = node;
        }
    }

    /**
     * internal definition of node pair.
     *
     * @param <T> type of
     * @param <V>
     */
    private static class Pair<T, V> {
        Node<T, V> prev;
        Node<T, V> curr;

        Pair(Node<T, V> p, Node<T, V> c) {
            prev = p;
            curr = c;
        }
    }

    /**
     * Default constructor.
     */
    public LockFreeBSTree() {
        root = new Node<T, V>();
    }

    @SuppressWarnings("unchecked")
    private Pair<T, V> search(Node<T, V> root, T key) {
        Node<T, V> c;
        retry: while (true) {
            Node<T, V> p = root;
            Node<T, V> n = (Node<T, V>) MultiCAS.mcasRead(root,
                    Node.LEFT_OFFSET);

            // empty tree
            if (null == n) {
                return new Pair<T, V>(p, n);
            }

            while (!n.isThread()) {
                int comp = ((Comparable) key).compareTo(n.key);
                if (comp < 0)
                    c = (Node<T, V>) MultiCAS.mcasRead(n, Node.LEFT_OFFSET);
                else if (comp > 0)
                    c = (Node<T, V>) MultiCAS.mcasRead(n, Node.RIGHT_OFFSET);
                else
                    return new Pair<T, V>(p, n);
                /* We retry if we read from a stale link. */
                if (c == null) {
                    continue retry;
                }
                p = n;
                n = c;
            }
            /* If the thread matches, retry to find parent. */
            if (key == n.unthread().key) {
                continue retry;
            }
            return new Pair<T, V>(p, n);
        }
    }

    /**
     * Find key in the tree.
     *
     * @param tree
     *            tree to be search
     * @param key
     *            key to search
     * @return value with key,, otherwise null
     */
    public V find(LockFreeBSTree<T, V> tree, T key) {
        return find(tree.root, key);
    }

    /**
     * Find key in the tree from root.
     *
     * @param root
     *            root of tree to search
     * @param key
     *            key to search
     * @return value with key
     */
    @SuppressWarnings("unchecked")
    private V find(Node<T, V> root, T key) {
        Node<T, V> n = search(root, key).curr;
        return n.isThread() ? null : (V) MultiCAS
                .mcasRead(n, Node.VALUE_OFFSET);
    }

    /**
     * update value with key in the tree.
     *
     * @param tree
     *            tree to be updated
     * @param key
     *            key to be updated
     * @param value
     *            new value
     * @return oldValue if key is found, otherwise return null
     */
    public V update(LockFreeBSTree<T, V> tree, T key, V value) {
        return update(tree.root, key, value);
    }

    @SuppressWarnings("unchecked")
    private V update(Node<T, V> root, T key, V value) {
        Node<T, V> node = new Node<T, V>(key, value);
        V oldValue;
        Node<T, V> n;
        Node<T, V> p;
        Pair<T, V> pair;
        retry: while (true) {
            do {
                pair = search(root, key);
                p = pair.prev;
                n = pair.curr;

                // empty tree
                if (null == n) {
                    node.left = minDummyNode;
                    node.right = maxDummyNode;
                    if (!unsafe.compareAndSwapObject(p, Node.LEFT_OFFSET, n,
                            node)) {
                        continue retry;
                    } else {
                        return null;
                    }
                }

                if (!n.isThread()) {
                    do {
                        oldValue = (V) MultiCAS.mcasRead(n, Node.VALUE_OFFSET);
                        if (oldValue == null) {
                            continue retry;
                        }
                    } while (!unsafe.compareAndSwapObject(n, Node.VALUE_OFFSET,
                            oldValue, value));
                    return oldValue;
                }

                if (((Comparable) p.key).compareTo((Comparable) key) < 0) {
                    if ((n != maxDummyNode)
                            && ((Comparable) n.unthread().key)
                                    .compareTo((Comparable) key) < 0)
                        continue retry;
                    node.left = p.thread();
                    node.right = n;
                } else {
                    if ((n != minDummyNode)
                            && ((Comparable) n.unthread().key)
                                    .compareTo((Comparable) key) > 0)
                        continue retry;
                    node.left = n;
                    node.right = p.thread();
                }
            } while (!unsafe.compareAndSwapObject(p, ((Comparable) p.key)
                    .compareTo((Comparable) key) < 0 ? Node.RIGHT_OFFSET
                    : Node.LEFT_OFFSET, n, node));
            return null;
        }
    }

    /**
     * Remove key in the tree. deletion is the most time-consuming operation to
     * implement because of the number of different tree configurations which
     * must be handled.
     *
     * @param tree
     *            tree with key needed to be removed
     * @param key
     *            key to be removed
     * @return value with key, otherwise null
     */
    public V remove(LockFreeBSTree<T, V> tree, T key) {
        return remove(tree.root, key);
    }

    /**
     * Remove key in the tree.
     *
     * @param root
     *            root of tree
     * @param key
     *            key to be removed
     * @return value with key
     */
    @SuppressWarnings("unchecked")
    private V remove(Node<T, V> root, T key) {
        Pair<T, V> pair;
        Node<T, V> p;
        Node<T, V> d;

        Node<T, V> pred;
        Node<T, V> ppred = null;
        Node<T, V> cpred;
        Node<T, V> succ;
        Node<T, V> psucc = null;
        Node<T, V> csucc;
        Node<T, V> succR;
        Node<T, V> predL;

        retry: while (true) {
            pair = search(root, key);
            p = pair.prev;
            d = pair.curr;

            if ((null == d) || d.isThread()) {
                return null;
            }

            /* Read contents of node: retry if node is garbage */
            Node<T, V> dl = (Node<T, V>) MultiCAS.mcasRead(d, Node.LEFT_OFFSET);
            Node<T, V> dr = (Node<T, V>) MultiCAS
                    .mcasRead(d, Node.RIGHT_OFFSET);
            V dv = (V) MultiCAS.mcasRead(d, Node.VALUE_OFFSET);

            if ((dl == null) || (dr == null) || (dv == null))
                continue retry;

            /* deep into the left branch */
            if (((p == root) || (((Comparable) p.key)
                    .compareTo((Comparable) d.key) > 0))) {
                if (!dl.isThread() && !dr.isThread()) {
                    /* Find predecessor, and its parent (pred, ppred) */
                    pred = d;
                    cpred = dl;
                    while (!cpred.isThread()) {
                        ppred = pred;
                        pred = cpred;
                        cpred = (Node<T, V>) MultiCAS.mcasRead(pred,
                                Node.RIGHT_OFFSET);
                        if (cpred == null)
                            continue retry;
                    }

                    /* Find successor, and its parent (cuss, psucc) */
                    succ = d;
                    csucc = dr;
                    while (!csucc.isThread()) {
                        psucc = succ;
                        succ = csucc;
                        csucc = (Node<T, V>) MultiCAS.mcasRead(succ,
                                Node.LEFT_OFFSET);
                        if (csucc == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[8];
                    long[] offset = new long[8];
                    Object[] oldValue = new Object[8];
                    Object[] newValue = new Object[8];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = succ;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d.thread();
                    newValue[3] = dl;

                    obj[4] = p;
                    offset[4] = Node.LEFT_OFFSET;
                    oldValue[4] = d;
                    newValue[4] = succ;

                    obj[5] = pred;
                    offset[5] = Node.RIGHT_OFFSET;
                    oldValue[5] = d.thread();
                    newValue[5] = succ.thread();

                    if (succ == dr) { /* Case 4, Fig. 4.8 */
                        if (!MultiCAS.mcas(6, obj, offset, oldValue, newValue)) {
                            continue retry;
                        }
                    } else {
                        succR = (Node<T, V>) MultiCAS.mcasRead(succ,
                                Node.RIGHT_OFFSET);
                        obj[6] = succ;
                        offset[6] = Node.RIGHT_OFFSET;
                        oldValue[6] = succR;
                        newValue[6] = dr;

                        assert psucc != null;
                        obj[7] = psucc;
                        offset[7] = Node.LEFT_OFFSET;
                        oldValue[7] = succ;
                        newValue[7] = succR.isThread() ? succ.thread() : succR;

                        if (!MultiCAS.mcas(8, obj, offset, oldValue, newValue)) {
                            continue retry;
                        }
                    }
                } else if (dl.isThread() && !dr.isThread()) {
                    /* Case 2, Fig. 4.8 */
                    /* Find successor, and its parent (cuss, psucc) */
                    succ = d;
                    csucc = dr;
                    while (!csucc.isThread()) {
                        psucc = succ;
                        succ = csucc;
                        csucc = (Node<T, V>) MultiCAS.mcasRead(succ,
                                Node.LEFT_OFFSET);
                        if (csucc == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[5];
                    long[] offset = new long[5];
                    Object[] oldValue = new Object[5];
                    Object[] newValue = new Object[5];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = succ;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d.thread();
                    newValue[3] = dl;

                    obj[4] = p;
                    offset[4] = Node.LEFT_OFFSET;
                    oldValue[4] = d;
                    newValue[4] = dr;

                    if (!MultiCAS.mcas(5, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }
                } else if (!dl.isThread() && dr.isThread()) {
                    /* Case 3, Fig. 4.8 */
                    /* Find predecessor, and its parent (pred, ppred) */
                    pred = d;
                    cpred = dl;
                    while (!cpred.isThread()) {
                        ppred = pred;
                        pred = cpred;
                        cpred = (Node<T, V>) MultiCAS.mcasRead(pred,
                                Node.RIGHT_OFFSET);
                        if (cpred == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[5];
                    long[] offset = new long[5];
                    Object[] oldValue = new Object[5];
                    Object[] newValue = new Object[5];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = p;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d;
                    newValue[3] = dl;

                    obj[4] = pred;
                    offset[4] = Node.RIGHT_OFFSET;
                    oldValue[4] = d.thread();
                    newValue[4] = dr;

                    /* Case 2, Fig. 4.8 */
                    if (!MultiCAS.mcas(5, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }
                } else if (dl.isThread() && dr.isThread()) {
                    /* Case 1, Fig. 4.8 */
                    /* prepare array for MCAS */
                    Object[] obj = new Object[4];
                    long[] offset = new long[4];
                    Object[] oldValue = new Object[4];
                    Object[] newValue = new Object[4];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = p;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d;
                    newValue[3] = dl;

                    /* Case 1, Fig. 4.8 */
                    if (!MultiCAS.mcas(4, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }
                } else {
                    /* cannot reach here */
                    assert false;
                }
            } else {/* All symmetric and simpler cases omitted. */
                if (!dl.isThread() && !dr.isThread()) {
                    /* Find predecessor, and its parent (pred, ppred) */
                    pred = d;
                    cpred = dl;
                    while (!cpred.isThread()) {
                        ppred = pred;
                        pred = cpred;
                        cpred = (Node<T, V>) MultiCAS.mcasRead(pred,
                                Node.RIGHT_OFFSET);
                        if (cpred == null)
                            continue retry;
                    }

                    /* Find successor, and its parent (cuss, psucc) */
                    succ = d;
                    csucc = dr;
                    while (!csucc.isThread()) {
                        psucc = succ;
                        succ = csucc;
                        csucc = (Node<T, V>) MultiCAS.mcasRead(succ,
                                Node.LEFT_OFFSET);
                        if (csucc == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[8];
                    long[] offset = new long[8];
                    Object[] oldValue = new Object[8];
                    Object[] newValue = new Object[8];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = succ;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d.thread();
                    newValue[3] = pred.thread();

                    obj[4] = p;
                    offset[4] = Node.RIGHT_OFFSET;
                    oldValue[4] = d;
                    newValue[4] = pred;

                    obj[5] = pred;
                    offset[5] = Node.RIGHT_OFFSET;
                    oldValue[5] = d.thread();
                    newValue[5] = dr;

                    if (pred == dl) { /* Case 4, Fig. 4.8 */
                        if (!MultiCAS.mcas(6, obj, offset, oldValue, newValue)) {
                            continue retry;
                        }
                    } else {
                        predL = (Node<T, V>) MultiCAS.mcasRead(pred,
                                Node.LEFT_OFFSET);
                        obj[6] = pred;
                        offset[6] = Node.LEFT_OFFSET;
                        oldValue[6] = predL;
                        newValue[6] = dl;

                        assert ppred != null;
                        obj[7] = ppred;
                        offset[7] = Node.RIGHT_OFFSET;
                        oldValue[7] = pred;
                        if (pred == null || predL == null) {
                            System.out.println("predL = " + predL);
                            System.out.println("pred = " + pred);
                            System.out.println("ppred = " + ppred);
                        }
                        newValue[7] = predL.isThread() ? pred.thread() : predL;

                        if (!MultiCAS.mcas(8, obj, offset, oldValue, newValue)) {
                            continue retry;
                        }
                    }
                } else if (dl.isThread() && !dr.isThread()) {/*
                                                                 * Case 3, Fig.
                                                                 * 4.8
                                                                 */
                    /* Find successor, and its parent (cuss, psucc) */
                    succ = d;
                    csucc = dr;
                    while (!csucc.isThread()) {
                        psucc = succ;
                        succ = csucc;
                        csucc = (Node<T, V>) MultiCAS.mcasRead(succ,
                                Node.LEFT_OFFSET);
                        if (csucc == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[5];
                    long[] offset = new long[5];
                    Object[] oldValue = new Object[5];
                    Object[] newValue = new Object[5];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = succ;
                    offset[3] = Node.LEFT_OFFSET;
                    oldValue[3] = d.thread();
                    newValue[3] = dl;

                    obj[4] = p;
                    offset[4] = Node.RIGHT_OFFSET;
                    oldValue[4] = d;
                    newValue[4] = dr;

                    if (!MultiCAS.mcas(5, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }

                } else if (!dl.isThread() && dr.isThread()) {
                    /* Case 2, Fig. 4.8 */
                    /* Find predecessor, and its parent (pred, ppred) */
                    pred = d;
                    cpred = dl;
                    while (!cpred.isThread()) {
                        ppred = pred;
                        pred = cpred;
                        cpred = (Node<T, V>) MultiCAS.mcasRead(pred,
                                Node.RIGHT_OFFSET);
                        if (cpred == null)
                            continue retry;
                    }

                    /* prepare array for MCAS */
                    Object[] obj = new Object[5];
                    long[] offset = new long[5];
                    Object[] oldValue = new Object[5];
                    Object[] newValue = new Object[5];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = p;
                    offset[3] = Node.RIGHT_OFFSET;
                    oldValue[3] = d;
                    newValue[3] = dl;

                    obj[4] = pred;
                    offset[4] = Node.RIGHT_OFFSET;
                    oldValue[4] = d.thread();
                    newValue[4] = dr;

                    /* Case 2, Fig. 4.8 */
                    if (!MultiCAS.mcas(5, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }

                } else if (dl.isThread() && dr.isThread()) {/* Case 1, Fig. 4.8 */
                    /* Case 1, Fig. 4.8 */
                    /* prepare array for MCAS */
                    Object[] obj = new Object[4];
                    long[] offset = new long[4];
                    Object[] oldValue = new Object[4];
                    Object[] newValue = new Object[4];

                    obj[0] = d;
                    offset[0] = Node.LEFT_OFFSET;
                    oldValue[0] = dl;
                    newValue[0] = null;

                    obj[1] = d;
                    offset[1] = Node.RIGHT_OFFSET;
                    oldValue[1] = dr;
                    newValue[1] = null;

                    obj[2] = d;
                    offset[2] = Node.VALUE_OFFSET;
                    oldValue[2] = dv;
                    newValue[2] = null;

                    obj[3] = p;
                    offset[3] = Node.RIGHT_OFFSET;
                    oldValue[3] = d;
                    newValue[3] = dr;

                    /* Case 1, Fig. 4.8 */
                    if (!MultiCAS.mcas(4, obj, offset, oldValue, newValue)) {
                        continue retry;
                    }
                } else {
                    /* cannot reach here */
                    assert false;
                }
            }

            return dv;
        }
    }

    /**
     * Is the tree empty?
     *
     * @return true if the tree is empty, otherwise false
     */
    public boolean isEmpty() {
        return root.right == null;
    }
}
