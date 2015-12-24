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
 * Interface of directed graph.
 *
 * @author Zhi Gan
 *
 * @param <E> type of element in the node of the graph
 *
 */
public interface DirectedGraph<E> extends Graph<E> {

    /**
     * Get weighted edges started with start node.
     *
     * @param start
     *            start node
     * @return collection of weighted edges started with start node
     */
    public Collection<AdjacentNode<E>> getWeightDestinations(Node<E> start);

    /**
     * Get nodes started with start node.
     *
     * @param start
     *            start node
     * @return collection of nodes started with start node
     */
    public Collection<Node<E>> getDestinations(Node<E> start);

    /**
     * Get nodes end with end node.
     *
     * @param end
     *            end node
     * @return collection of nodes ended with end node
     */
    public Collection<Node<E>> getSources(Node<E> end);

    /**
     * Get weighted edges ended with end node.
     *
     * @param end
     *            end node
     * @return collection of weighted edges ended with end node
     */
    public Collection<AdjacentNode<E>> getWeightSources(Node<E> end);

    /**
     * Get edges ended with end node.
     *
     * @param node
     *            end node
     * @return collection of edges ended with node
     */
    public Collection<Edge<E>> getIncoming(Node<E> node);

    /**
     * Get edges started with start node.
     *
     * @param node
     *            start node
     * @return collection of edges started with start node
     */
    public Collection<Edge<E>> getOutgoing(Node<E> node);
}
