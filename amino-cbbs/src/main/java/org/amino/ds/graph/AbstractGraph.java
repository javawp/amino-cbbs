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

package org.amino.ds.graph;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 * Default implementation of {@link Graph} interface.
 *
 * @param <E>
 *            Type of elements
 *
 * @author Zhi Gan
 *
 */
public abstract class AbstractGraph<E> implements Graph<E> {

    ConcurrentHashMap<E, AdjacentList<E>> mainList;
    private AtomicInteger globalSequenceOfNode;

    /**
     * Default constructor.
     */
    public AbstractGraph() {
        mainList = new ConcurrentHashMap<E, AdjacentList<E>>();
        globalSequenceOfNode = new AtomicInteger(0);
    }

    /**
     * get all ownership of every element in targets.
     *
     * @param targets
     *            Collection of elements need ownership
     * @return true if the operation is successful
     */
    @SuppressWarnings("unchecked")
    protected boolean getMultiOwnerShip(AdjacentList<E>[] targets) {
        Arrays.sort(targets);

        for (int i = 0, len = targets.length; i < len; ++i) {
            if (!targets[i].getOwnership()) {
                AdjacentList<E>[] free = new AdjacentList[i];
                System.arraycopy(targets, 0, free, 0, i);
                freeMultiOwnerShip(free);
                return false;
            }
        }
        return true;
    }

    /**
     * release all ownership of every element in targets.
     *
     * @param targets  Collection of elements whose ownership is released
     * @return true if operation is successful
     */
    protected boolean freeMultiOwnerShip(AdjacentList<E>[] targets) {
        Arrays.sort(targets);

        for (int i = targets.length - 1; i > -1; --i) {
            targets[i].freeOwnership();
        }

        return true;
    }

    /**
     *  manage contention of multithreads in the rush for ownership. Policy is aborting itself
     *
     * @param targets Array of elements whose ownership need to be got
     * @return true if get all ownership of elements in the array
     */
    protected boolean simpleContentionManager(AdjacentList<E>[] targets) {
        int backoff = 1;
        int i = 0;

        while (!getMultiOwnerShip(targets) && i < 16) {
            try {
                Thread.sleep(backoff << (++i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /**
         * our contention manager policy here... abort my self ,since I no any
         * availabe API in java can safely abort the other thread.
         */
        if (i == 16)
            return false;

        else
            return true;
    }

    /**
     *  manage contention of multithreads in the rush for ownership. Policy is aborting itself
     *
     * @param targets element whose ownership need to be got
     * @return true if get ownership of element
     */
    protected boolean simpleContentionManager(AdjacentList<E> targets) {
        int backoff = 1;
        int i = 0;

        while (!targets.getOwnership() && i < 16) {
            try {
                Thread.sleep(backoff << (++i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /**
         * our contention manager policy here... abort my self ,since I no any
         * availabe API in java can safely abort the other thread.
         */
        if (i == 16)
            return false;

        else
            return true;
    }

	/**
	 * {@inheritDoc}
	 */
    public Node<E> addNode(E e) {
        // Node<E> node = bufferNodeStart.get();
        // node.setValue(e);
        if (mainList.containsKey(e))
            return mainList.get(e).getHeadNode();

        Node<E> node = new Node<E>(e);

        node.setCompare(globalSequenceOfNode.getAndIncrement());
        int gs = globalSequenceOfNode.get();
        if (gs < 0)
            globalSequenceOfNode.compareAndSet(gs, 0);

        AdjacentList<E> al = new AdjacentList<E>(node);
        AdjacentList<E> prev;

        prev = mainList.putIfAbsent(e, al);
        if (prev == null)
            return al.getHeadNode();
        else
            return prev.getHeadNode();
    }

	/**
	 * {@inheritDoc}
	 */
    public Node<E> addNode(Node<E> node) {
        E e = node.getValue();
        if (mainList.containsKey(e)) {
            return mainList.get(e).getHeadNode();
        }

        node.setCompare(globalSequenceOfNode.getAndIncrement());
        int gs = globalSequenceOfNode.get();
        if (gs < 0)
            globalSequenceOfNode.compareAndSet(gs, 0);

        AdjacentList<E> al = new AdjacentList<E>(node);
        AdjacentList<E> prev;

        prev = mainList.putIfAbsent(e, al);
        if (prev == null)
            return al.getHeadNode();
        else
            return prev.getHeadNode();
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean addAllNodes(Collection<Node<E>> nodes) {
        Iterator<Node<E>> iter = nodes.iterator();
        while (iter.hasNext()) {
            addNode(iter.next());
        }

        return true;
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean containsEdge(E start, E end) {
        // Node<E> s = bufferNodeStart.get();
        // s.setValue(start);
        //
        // Node<E> e = bufferNodeEnd.get();
        // e.setValue(end);

        AdjacentList<E> al = mainList.get(start);
        if (al == null)
            return false;

        return al.containsEdge(end);
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<AdjacentNode<E>> getLinkedNodes(Node<E> node) {
        AdjacentList<E> al = mainList.get(node.getValue());
        if (al == null)
            return new ArrayList<AdjacentNode<E>>();

        return al.getLinkedNodes();
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<Edge<E>> getLinkedEdges(Node<E> node) {
        AdjacentList<E> al = mainList.get(node.getValue());
        if (al == null)
            return new ArrayList<Edge<E>>();

        return al.getLinkedEdges();
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            addNode(e);
        }
        return true;
    }

	/**
	 * {@inheritDoc}
	 */
    public void clear() {
        mainList.clear();
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean contains(Object o) {
        return mainList.containsKey(o);
    }

    /**
     *   If node is contained in graph.
     * @param node target node
     * @return true if node is contained in mainList
     */
    public boolean containsNode(Node<E> node) {
        return mainList.containsKey(node.getValue());
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
	 * {@inheritDoc}
	 */
    public boolean isEmpty() {
        return mainList.isEmpty();
    }

	/**
	 * {@inheritDoc}
	 */
    public Iterator<E> iterator() {
        return mainList.keySet().iterator();
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean removeAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        while (e.hasNext())
            if (!remove(e.next()))
                return false;
        return true;
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

	/**
	 * {@inheritDoc}
	 */
    public int size() {
        return mainList.size();
    }

	/**
	 * {@inheritDoc}
	 */
    public Object[] toArray() {
        Object[] e = new Object[mainList.size()];

        int index = 0;
        Iterator<E> iter = iterator();
        while (iter.hasNext())
            e[index++] = iter.next();

        return e;

    }

	/**
	 * {@inheritDoc}
	 */
    @SuppressWarnings({ "unchecked", "hiding" })
    public <E> E[] toArray(E[] a) {
        E[] array = (E[]) toArray();
        int size = array.length;

        if (a.length < size())
            a = (E[]) java.lang.reflect.Array.newInstance(a.getClass()
                    .getComponentType(), size);

        System.arraycopy(array, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;

        return a;
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean add(E o) {
        return addNode(o) != null;
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean remove(Object o) {
        return mainList.remove(o) != null;
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean removeNode(Node<E> node) {
        return remove(node.getValue());
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<Node<E>> getAllNodes() {
        return new KeySet();
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<Edge<E>> getEdges(E start, E end) {
        AdjacentList<E> al = mainList.get(start);
        if (al == null)
            return new ArrayList<Edge<E>>();
        return al.getEdge(end);
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<Edge<E>> getEdges(Node<E> start, Node<E> end) {
        AdjacentList<E> al = mainList.get(start.getValue());
        if (al == null)
            return new ArrayList<Edge<E>>();

        return al.getEdge(end);
    }

	/**
	 * {@inheritDoc}
	 */
    public Collection<Node<E>> getNodes(E e) {
        Node<E> n;
        Collection<Node<E>> coll = new ArrayList<Node<E>>();
        Iterator<Node<E>> iter = getAllNodes().iterator();
        while (iter.hasNext()) {
            n = iter.next();
            if (n.getValue().equals(e))
                coll.add(n);
        }
        return coll;
    }

    /**
     * for testing.
     *
     */
    public void dumpGraph() {
        Enumeration<AdjacentList<E>> enu = mainList.elements();
        while (enu.hasMoreElements()) {
            AdjacentList<E> al = enu.nextElement();
            System.out.println();
            // System.out.printf("%6d",al.headNode.getCompare());
            System.out.printf("%6s", al.getHeadNode().getValue());
            AdjacentNode<E> adj;

            List<AdjacentNode<E>> linkedNodes = al.getLinkedNodes();
            // System.out.println("size = " + linkedNodes.size());
            for (int i = 0, len = linkedNodes.size(); i < len; ++i) {
                adj = linkedNodes.get(i);
                // if(adj == null){
                // System.out.println("NULLPointer");
                // System.out.println(" " + i + " " + len + " " + al.times);
                // }
                // if(adj.getNode() == null){
                // System.out.println("1");
                // }
                System.out.printf("%6s(w:%6f)", adj.getNode().getValue(), adj
                        .getWeight());
            }
        }

        System.out.println();
    }

    /**
     * Internal iterator implementation.
     *
     */
    private class Itr implements Iterator<Node<E>> {
        Iterator<E> iter;
        E cur;

        public Itr() {
            iter = mainList.keySet().iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Node<E> next() {
            return mainList.get(iter.next()).getHeadNode();
        }

        public void remove() {
            iter.remove();
        }

    }

    /**
     * @author ganzhi
     *
     */
    final class KeySet extends AbstractSet<Node<E>> {

        Iterator<E> iter;


        /**
          * {@inheritDoc}
          */
        @Override
        public Iterator<Node<E>> iterator() {
            return new Itr();
        }

        /**
          * {@inheritDoc}
          */
        @Override
        public int size() {
            return mainList.size();
        }

        /**
          * {@inheritDoc}
          */
        @SuppressWarnings("unchecked")
		public boolean contains(Object o) {
            return mainList.contains(((Node<E>) o).getValue());
        }

        /**
          * {@inheritDoc}
          */
        @SuppressWarnings("unchecked")
		public boolean remove(Object o) {
            return mainList.remove(((Node<E>) o).getValue()) != null;
        }

        /**
          * {@inheritDoc}
          */
        public void clear() {
            mainList.clear();
        }

        /**
          * {@inheritDoc}
          */
        public Object[] toArray() {
            Collection<Node<E>> c = new ArrayList<Node<E>>();
            for (Iterator<Node<E>> i = iterator(); i.hasNext();)
                c.add(i.next());
            return c.toArray();
        }

        /**
          * {@inheritDoc}
          */
        public <T> T[] toArray(T[] a) {
            Collection<Node<E>> c = new ArrayList<Node<E>>();
            for (Iterator<Node<E>> i = iterator(); i.hasNext();)
                c.add(i.next());
            return c.toArray(a);
        }

    }

	/**
	 * {@inheritDoc}
	 */
    abstract public Graph<E> clone() throws CloneNotSupportedException;
}
