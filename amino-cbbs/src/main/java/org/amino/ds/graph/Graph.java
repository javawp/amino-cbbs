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

import java.util.Collection;

/**
 * A Graph provides basic operations to create, search and change itself. This
 * graph doesn't allow duplicated key for all nodes.
 * 
 * @author Zhi Gan
 * 
 * @param <E> Type of elements
 */
public interface Graph<E> extends Collection<E>, Cloneable {
	/**
	 * get all nodes which value equals to e.
	 * 
	 * @param e
	 *            value to be got
	 * @return a collection of nodes
	 */
	public Collection<Node<E>> getNodes(E e);

	/**
	 * get all the nodes in the graph.
	 * 
	 * @return a collection of all the nodes in the graph
	 */
	public Collection<Node<E>> getAllNodes();

	/**
	 * get all all the edges which start from node start and end with node end.
	 * 
	 * @param start
	 *            the start node of the edge
	 * @param end
	 *            the end node of the edge
	 * @return collection of all the edges which start from node start and end
	 *         with node end
	 */
	public Collection<Edge<E>> getEdges(Node<E> start, Node<E> end);

	/**
	 * get all the edge start from the nodes which contain value start and end.
	 * to nodes which contain value end
	 * 
	 * @param start
	 *            start value
	 * @param end
	 *            end value
	 * @return collection of all the edge start from the nodes which contain
	 *         value start and end to nodes which contain value end
	 */
	public Collection<Edge<E>> getEdges(E start, E end);

	/**
	 * add a node which contains the value e to graph.
	 * 
	 * @param e
	 *            the vaule
	 * @return a node in graph which contains the value
	 */
	public Node<E> addNode(E e);

	/**
	 * add a node to graph.
	 * 
	 * @param node
	 *            the node
	 * @return return the node in the graph
	 */
	public Node<E> addNode(Node<E> node);

	/**
	 * add all the nodes to graph.
	 * 
	 * @param nodes
	 *            nodes to be added
	 * @return true if the operation is successful
	 */
	public boolean addAllNodes(Collection<Node<E>> nodes);

	/**
	 * add one edge to graph.
	 * 
	 * @param start
	 *            start node
	 * @param end
	 *            end node
	 * @param weight
	 *            weight
	 * @return true if the operation is successful
	 */
	public boolean addEdge(E start, E end, double weight);

	/**
	 * add one edge to graph with weight.
	 * 
	 * @param start
	 *            start value
	 * @param end
	 *            end value
	 * @param weight
	 *            weight of edge
	 * @return true if the operation is successful
	 */
	public boolean addEdge(Node<E> start, Node<E> end, double weight);

	/**
	 * Add an edge to this graph.
	 * 
	 * @param edge
	 *            adding edge
	 * @return true if succeed
	 */
	public boolean addEdge(Edge<E> edge);

	/**
	 * get all the linked nodes of the node.
	 * 
	 * @param node
	 *            start node
	 * @return collection of all linked nodes
	 */
	public Collection<AdjacentNode<E>> getLinkedNodes(Node<E> node);

	/**
	 * get all the linked edges of the node.
	 * 
	 * @param node
	 *            start node
	 * @return collection of all linked edges
	 */
	public Collection<Edge<E>> getLinkedEdges(Node<E> node);

	/**
	 * remove all the edges which start from start and end to end.
	 * 
	 * @param start
	 *            start node
	 * @param end
	 *            end node
	 * @return true if the operation is successful
	 */
	public boolean removeEdge(Node<E> start, Node<E> end);

	/**
	 * exactly remove the edge from graph.
	 * 
	 * @param edge
	 *            edge removed
	 * @return true if the operation is successful
	 */
	public boolean removeEdge(Edge<E> edge);

	/**
	 * remove all the edges which start from start and end to end.
	 * 
	 * @param start
	 *            start value
	 * @param end
	 *            end value
	 * @return true if the operation is successful
	 */
	public boolean removeEdge(E start, E end);

	/**
	 * remove the node from the graph.
	 * 
	 * @param node
	 *            node removed
	 * @return true if the operation is successful
	 */
	public boolean removeNode(Node<E> node);

	/**
	 * Returns whether this graph contains an edge between start and end.
	 * 
	 * @param start
	 *            starting node of edge
	 * @param end
	 *            ending node of edge
	 * @return true if this graph contains edge between start and end, false
	 *         otherwise.
	 */
	public boolean containsEdge(E start, E end);

	/**
	 * Clone this graph.
	 * 
	 * @return a graph
	 * @throws CloneNotSupportedException
	 *             clone not supported
	 */
	public Graph<E> clone() throws CloneNotSupportedException;

	/**
	 * whether this graph contains the specified node.
	 * 
	 * @param start
	 *            the node
	 * @return true if this graph contains the node
	 */
	public boolean containsNode(Node<E> start);
}
