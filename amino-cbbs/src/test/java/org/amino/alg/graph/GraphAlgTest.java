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

/**
 *
 */
package org.amino.alg.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.amino.ds.graph.DirectedGraph;
import org.amino.ds.graph.DirectedGraphImpl;
import org.amino.ds.graph.Node;
import org.amino.ds.graph.UndirectedGraph;
import org.amino.util.AbstractBaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Zhi Gan
 * 
 */

@RunWith(Parameterized.class)
public class GraphAlgTest extends AbstractBaseTest {
	@Parameters
	public static Collection parameters() {
		Collection<Integer> tn = getThreadNums();
		int elementNum = getElementNum();

		List<Object[]> args = new ArrayList<Object[]>();

		for (int threadNum : tn) {
			Object[] item1 = { elementNum, threadNum, new Object[] {} };
			args.add(item1);
		}
		return args;
	}

	private DirectedGraphImpl<String> d_graph;

	// TODO: buggy code
	private void generateRandomGraph(int edgeNum) {
		Object[] array = { new int[] { 0, 1, 1 }, new int[] { 1, 3, 2 },
				new int[] { 0, 3, 3 }, new int[] { 0, 4, 4 },
				new int[] { 3, 4, 5 }, new int[] { 3, 5, 6 },
				new int[] { 4, 5, 7 }, new int[] { 6, 10, 8 },
				new int[] { 9, 10, 9 }, new int[] { 1, 5, 10 },
				new int[] { 1, 2, 11 }, new int[] { 2, 5, 12 },
				new int[] { 8, 9, 13 }, new int[] { 2, 6, 14 },
				new int[] { 7, 8, 15 }, new int[] { 4, 7, 16 },
				new int[] { 5, 9, 17 }, new int[] { 5, 7, 18 },
				new int[] { 6, 9, 19 }, new int[] { 4, 8, 20 },
				new int[] { 2, 10, 21 }, new int[] { 5, 6, 22 } };
		NELEMENT = 11;
		for (int i = 0; i < array.length; ++i) {
			u_graph.addEdge("" + ((int[]) array[i])[0], ""
					+ ((int[]) array[i])[1], ((int[]) array[i])[2]);
		}
	}

	/**
	 * 
	 * @param vertex
	 * @param threadNum
	 * @param para
	 */
	public GraphAlgTest(int vertex, int threadNum, Object[] para) {
		super(GraphAlgTest.class, para, threadNum, vertex);

		u_graph = new UndirectedGraph<String>();
		d_graph = new DirectedGraphImpl<String>();
		generateRandomGraph(2 * NELEMENT);
	}

	private UndirectedGraph<String> u_graph;

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMST() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(NTHREAD);
		GraphAlg.getMST(u_graph, executor);
		executor.shutdown();
	}

	private UndirectedGraph<String> generateRandomGraph_cc(int nodeNum,
			float complete) {
		UndirectedGraph<String> graph = new UndirectedGraph<String>();
		Random rand = new Random(System.currentTimeMillis());
		HashMap<Integer, String> map = new HashMap<Integer, String>(nodeNum);
		// undirected graph, so to divide 2
		int numEdges = (int) (nodeNum * (nodeNum - 1) * complete) / 2;

		// add node
		for (int i = 0; i < nodeNum; i++) {
			map.put(i, String.valueOf(i));
			graph.addNode(new Node<String>(String.valueOf(i)));
		}

		// add edge
		for (int i = 0; i < numEdges; i++) {
			int from, to;
			from = Math.abs(rand.nextInt()) % nodeNum;
			to = Math.abs(rand.nextInt()) % nodeNum;

			graph.addEdge(map.get(from).trim(), map.get(to).trim(), 0);
			// System.out.println(map.get(from) + "---" + map.get(to));
		}
		map = null;

		return graph;
	}

	@Test
	public void testCC() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(NTHREAD);
		UndirectedGraph<String> graph = generateRandomGraph_cc(1000, 0.1f);
		Collection<Collection<Node<String>>> result = GraphAlg
				.getConnectedComponents(graph, executor);
		executor.shutdown();
	}

	private DirectedGraph<String> generateRandomGraph_scc(int nodeNum,
			float complete) {
		DirectedGraph<String> graph = new DirectedGraphImpl<String>();
		Random rand = new Random(System.currentTimeMillis());
		HashMap<Integer, String> map = new HashMap<Integer, String>(nodeNum);
		int numEdges = (int) (nodeNum * (nodeNum - 1) * complete);

		// add node
		for (int i = 0; i < nodeNum; i++) {
			map.put(i, String.valueOf(i));
			graph.addNode(new Node<String>(String.valueOf(i)));
		}

		// add edge
		for (int i = 0; i < numEdges; i++) {
			int from, to;
			from = Math.abs(rand.nextInt()) % nodeNum;
			to = Math.abs(rand.nextInt()) % nodeNum;

			String fromStr = map.get(from);
			String toStr = map.get(to);

			while (graph.containsEdge(fromStr, toStr)) {
				from = Math.abs(rand.nextInt()) % nodeNum;
				to = Math.abs(rand.nextInt()) % nodeNum;
				fromStr = map.get(from);
				toStr = map.get(to);
			}

			graph.addEdge(map.get(from).trim(), map.get(to).trim(), 0);
			// System.out.println(map.get(from) + "-->" + map.get(to));
		}
		map = null;
		System.gc();

		return graph;
	}

	@Test
	public void testSCC() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(NTHREAD);
		DirectedGraph<String> graph = generateRandomGraph_scc(10000, 0.001f);
		Collection<Collection<Node<String>>> result = GraphAlg
				.getStrongComponents(graph, executor);
		executor.shutdown();
	}
}
