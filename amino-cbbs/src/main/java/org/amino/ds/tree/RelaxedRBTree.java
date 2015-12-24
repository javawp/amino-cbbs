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

package org.amino.ds.tree;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.amino.pattern.internal.Doable;

/**
 * high performance parallel red-black tree. The implementation is based on The
 * performance of concurrent RB tree algorithms by Hanke, 1998
 *
 * @author Xiao Jun Dai
 *
 * @param <E>
 *            Type of element
 */
public class RelaxedRBTree<E> {
	@SuppressWarnings("unchecked")
	private final static Node sentinel;
	static {
		sentinel = new Node();
		sentinel.left = sentinel.right = sentinel.parent = sentinel;
	}

	/**
	 * internal node of tree.
	 *
	 * @param <E>
	 *            Type of element in node
	 */
	public static class Node<E> {
		private Color color;
		private E value;
		private Node<E> left;
		private Node<E> right;
		private Node<E> parent;

		/**
		 * @return value
		 */
		public E getValue() {
			return value;
		}

		List<Request> req;

		/**
		 * default constructor.
		 */
		Node() {
			color = Color.BLACK;
			value = null;
			left = sentinel;
			right = sentinel;
			parent = sentinel;
			req = new CopyOnWriteArrayList<Request>();
		}

		/**
		 * @param element
		 *            element
		 */
		public Node(E element) {
			color = Color.BLACK;
			value = element;
			left = sentinel;
			right = sentinel;
			parent = sentinel;
			req = new CopyOnWriteArrayList<Request>();
		}

		/**
		 * @param element
		 *            element
		 * @param aParent
		 *            aParent
		 */
		public Node(E element, Node<E> aParent) {
			color = Color.BLACK;
			value = element;
			this.left = sentinel;
			this.right = sentinel;
			parent = aParent;
			req = new CopyOnWriteArrayList<Request>();
		}

		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			return value.toString() + ((color == Color.BLACK) ? " B" : "R");
		}
	}

	private Node<E> root;

	private final RelaxedBalancer balancer;

	private Lock lock = new ReentrantLock();

	/**
	 * default constructor.
	 */
	@SuppressWarnings("unchecked")
	public RelaxedRBTree() {
		root = sentinel;
		balancer = new RelaxedBalancer(lock);
	}

	/**
	 * shutdown balancer.
	 */
	public void shutdown() {
		balancer.shutdown();
	}

	/**
	 * @param element
	 *            element
	 */
	public void insert(E element) {
		lock.lock();
		try {
			insert(new Node<E>(element));
		} finally {
			lock.unlock();
		}
	}

	/**
	 * insert a node to the leaf
	 *
	 * @param z
	 *            node to be insert
	 */
	@SuppressWarnings("unchecked")
	private void insert(Node<E> z) {
		Node<E> y = sentinel; // aParent of x
		Node<E> x = root;
		// locate position to insert
		while (sentinel != x) {
			y = x;
			if (((Comparable) z.value).compareTo((Comparable) x.value) < 0) {
				x = x.left;
			} else {
				x = x.right;
			}
		}

		assert y.left == sentinel && y.right == sentinel;
		// y is the location to insert and always leaf
		if (y.req.contains(Request.REMOVAL)) {
			y.req.remove(Request.REMOVAL);
			y.value = z.value;
		} else {
			z.parent = y;
			if (sentinel == y) { // empty tree
				root = z;
				root.color = Color.BLACK;
			} else {
				// replace the leaf by an internal red node with two black
				// leaves
				if (((Comparable) z.value).compareTo((Comparable) y.value) < 0) {
					y.left = z;
					y.right = new Node<E>(y.value, y);
				} else {
					y.right = z;
					y.left = new Node<E>(y.value, y);
				}

				y.color = Color.RED;
				assert y.left.color == Color.BLACK
						&& y.right.color == Color.BLACK;
				if (y.req.contains(Request.UP_OUT)) {
					y.req.remove(Request.UP_OUT);
					y.color = Color.BLACK;
				}

				if (y.parent.color == Color.RED) {
					/*
					 * aParent of new internal node p is red (as well as p
					 * itself) then the resulting tree is no longer a standard
					 * rb tree
					 */
					y.req.add(Request.UP_IN);
					balancer.addRequest(y);
				}
			}
		}
	}

	/**
	 * Removing element from tree
	 *
	 * @param element
	 *            element
	 * @return node removed, or null if no such element
	 */
	public Node<E> remove(E element) {
		lock.lock();
		try {
			// search the position first
			Node<E> res = remove(search(root, element));
			if (res == sentinel)
				return null;
			return res;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param z
	 *            start node
	 * @return node removed
	 */
	private Node<E> remove(Node<E> z) {
		if (sentinel == z) { // empty tree
			return sentinel;
		}

		assert ((sentinel == z.left) && (sentinel == z.right));

		Node<E> y = z.parent;

		if (sentinel == y) {
			root = sentinel;
		} else if ((Color.RED == y.color) && (y.req.contains(Request.UP_IN))) {
			y.req.remove(Request.UP_IN);
			removeLeafAndParent(z, y);

			// if (Color.BLACK == y.color) {
			// x.req = Request.UP_OUT;
			// // removeFixup(x);
			// }
		} else {
			z.req.add(Request.REMOVAL);
			balancer.addRequest(z);
		}
		return z;
	}

	/**
	 * @param z
	 *            node expected to be removed
	 * @param y
	 *            aParent node
	 * @return node removed
	 */
	private Node<E> removeLeafAndParent(Node<E> z, Node<E> y) {
		// remove leaf and its prarent
		Node<E> x = null; // remaining sibling
		if (z == y.left) { // z is left child
			x = y.right;
		} else {
			x = y.left;
		}
		x.parent = y.parent;
		if (y.parent == sentinel) {
			root = x;
		} else {
			if (y == y.parent.left) {
				y.parent.left = x;
			} else {
				y.parent.right = x;
			}
		}
		return x;
	}

	/**
	 * @param x
	 *            start node
	 * @param operation
	 *            operation done during walk
	 */
	private void inorderWalk(Node<E> x, Doable<E, E> operation) {
		if (sentinel != x) {
			inorderWalk(x.left, operation);
			operation.run(x.value);
			inorderWalk(x.right, operation);
		}
	}

	/**
	 * @param operation
	 *            operation done during walk
	 */
	public void inOrderWalk(Doable<E, E> operation) {
		inorderWalk(root, operation);
	}

	/**
	 * Search key in the tree
	 *
	 * @param k
	 *            key
	 * @return true if found, otherwise false
	 */
	public boolean find(E k) {
		lock.lock();
		try {
			return search(root, k) == sentinel ? false : true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param k
	 *            key
	 * @return node found
	 */
	public Node<E> search(E k) {
		lock.lock();
		try {
			Node<E> res = search(root, k);
			if (res == sentinel)
				return null;
			return res;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param x
	 *            start node
	 * @param k
	 *            key
	 * @return node found
	 */
	@SuppressWarnings("unchecked")
	private Node<E> search(Node<E> x, E k) {
		if (sentinel == x) { // empty tree
			return sentinel;
		}

		int comp;
		while (sentinel != x.left) {
			comp = ((Comparable) k).compareTo((Comparable) x.value);
			if (comp == 0) {
				if (((Comparable) x.value).compareTo((Comparable) x.left.value) == 0) {
					x = x.left;
				} else {
					x = x.right;
				}
			} else if (comp < 0) {
				x = x.left;
			} else {
				x = x.right;
			}
		}

		if (x.value.equals(k) && !x.req.contains(Request.REMOVAL)) {
			if (x.req.size() > 0)
				System.out.println("Request = " + x.req);
			return x;
		} else {
			return sentinel;
		}
	}

	/**
	 * @param x
	 *            start node
	 * @return minimal node of the tree with x as root
	 */
	private Node<E> min(Node<E> x) {
		while (sentinel != x.left) {
			x = x.left;
		}
		return x;
	}

	/**
	 * @param x
	 *            start node
	 * @return minimal node of the tree with x as root
	 */
	private Node<E> max(Node<E> x) {
		while (sentinel != x.right) {
			x = x.right;
		}
		return x;
	}

	/**
	 * @param x
	 *            start node
	 * @return successor node of x
	 */
	private Node<E> successor(Node<E> x) {
		if (sentinel != x.right) {
			return min(x.right);
		}
		Node<E> y = x.parent;
		while ((sentinel != y) && (x == y.right)) {
			x = y;
			y = y.parent;
		}
		return y;
	}

	/**
	 * @param x
	 *            start node
	 * @return predeccessor node of x
	 */
	private Node<E> predeccessor(Node<E> x) {
		if (sentinel != x.left) {
			return min(x.left);
		}
		Node<E> y = x.parent;
		while ((sentinel != y) && (x == y.left)) {
			x = y;
			y = y.parent;
		}
		return y;
	}

	/**
	 * @return true if tree is red-black tree, otherwise false
	 */
	public boolean verifyRBTree() {
		return verifyRBTree(root);
	}

	/**
	 * @param x
	 *            start node
	 * @return true if tree start with x is red-black tree, otherwise false
	 */
	private boolean verifyRBTree(Node<E> x) {
		if (sentinel != x) {
			if ((x.left == sentinel)) {
				if (x.color == Color.RED) {
					System.out.println("leaf is red");
					return false;
				}
			}

			if (x.color == Color.RED) {
				if (x.parent.color == Color.RED) {
					System.out.println("aParent of red node " + x + " is red");
					return false;
				}
			}

			return verifyRBTree(x.left) && verifyRBTree(x.right);
		}
		return true;
	}

	/**
	 * worker doing relaxed balance action.
	 *
	 */
	private class RelaxedBalancer {
		private ExecutorService exec = Executors.newSingleThreadExecutor();
		private Lock lock;

		/**
		 * @param lock
		 *            lock for synchronization
		 */
		public RelaxedBalancer(Lock lock) {
			this.lock = lock;
		}

		/**
		 * shutdown executor.
		 */
		public void shutdown() {
			exec.shutdown();
			try {
				exec.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * @param node
		 *            node
		 * @return future object generated by executor
		 */
		public Future<?> addRequest(final Node<E> node) {
			// return exec.submit(new Callable<Boolean>() {
			// public Boolean call() {
			// for (Request req : node.req)
			// switch (req) {
			// case REMOVAL:
			// // System.out.println("process request: Removel@"
			// // + node);
			// handleRemoval(node);
			// break;
			// case UP_IN:
			// handleUpIn(node);
			// // System.out.println("process request: UpIn@"
			// // + node);
			// break;
			// case UP_OUT:
			// // System.out
			// // .println("process request: UpOut@" + node);
			// handleUpOut(node);
			// break;
			// default:
			// return false;
			// }
			// return true;
			// }
			// });
			return null;
		}

		/**
		 * @param z
		 *            node
		 */
		private void handleRemoval(Node<E> z) {
			lock.lock();
			try {
				assert (z.req.contains(Request.REMOVAL));
				assert ((sentinel == z.left) && (sentinel == z.right));
				if (z.req.contains(Request.UP_OUT)) {
					handleUpOut(z);
				}
				if (z.parent.req.contains(Request.UP_OUT)) {
					handleUpOut(z.parent);
				}

				Node<E> y = z.parent;
				Node<E> x = null; // remaining sibling
				if (z == y.left) { // z is left child
					x = y.right;
				} else {
					x = y.left;
				}
				if (x.req.contains(Request.UP_IN)) {
					handleUpIn(x);
				}

				Node<E> sibling = removeLeafAndParent(z, y);
				if (Color.BLACK == y.color) {
					if (Color.RED == sibling.color) {
						sibling.color = Color.BLACK;
					} else {
						sibling.req.add(Request.UP_OUT);
						balancer.addRequest(sibling);
					}
				}
			} finally {
				lock.unlock();
			}
		}

		/**
		 * @param z
		 *            node
		 */
		private void handleUpIn(Node<E> z) {
			lock.lock();
			try {
				// System.out.println("handleUpIn");
				assert (z.req.contains(Request.UP_IN));
				z.req.remove(Request.UP_IN);
				Node<E> y = null; // sibling of aParent

				if (root == z.parent) {
					root.color = Color.RED;
					return;
				}

				if (Color.RED == z.parent.color) {
					if (z.parent == z.parent.parent.left) { // aParent is left
															// child
						y = z.parent.parent.right;
						if (Color.RED == y.color) {
							// red sibling, shift request up, case 2.e
							z.parent.color = Color.BLACK;
							y.color = Color.BLACK;
							z.parent.parent.color = Color.RED;
							z.parent.parent.req.add(Request.UP_IN);
							balancer.addRequest(z.parent.parent);
						} else {
							if (z == z.parent.right) { // rotation, case 2.d
								z = z.parent;
								leftRotate(z);
							}
							z.parent.color = Color.BLACK; // double rotation,
															// case
							// 2.c
							z.parent.parent.color = Color.RED;
							rightRotate(z.parent.parent);
						}
					} else { // same as if clause with "right" and "left"
						// exchanged
						y = z.parent.parent.left;
						if (Color.RED == y.color) {
							z.parent.color = Color.BLACK;
							y.color = Color.BLACK;
							z.parent.parent.color = Color.RED;
							z.parent.parent.req.add(Request.UP_IN);
							balancer.addRequest(z.parent.parent);
						} else {
							if (z == z.parent.left) {
								z = z.parent;
								rightRotate(z);
							}
							z.parent.color = Color.BLACK;
							z.parent.parent.color = Color.RED;
							leftRotate(z.parent.parent);
						}
					}
				}
			} finally {
				lock.unlock();
			}
		}

		/**
		 * @param x
		 *            node
		 */
		private void handleUpOut(Node<E> x) {
			lock.lock();
			try {
				assert (x.req.contains(Request.UP_OUT));
				x.req.remove(Request.UP_OUT);
				Node<E> w = null;
				// while ((x != root) && (Color.BLACK == x.color)) {
				if ((x != root) && (Color.BLACK == x.color)) {
					if (x == x.parent.left) {
						w = x.parent.right;
						if (Color.RED == w.color) { // case 4.a
							w.color = Color.BLACK;
							x.parent.color = Color.RED;
							leftRotate(x.parent);
							w = x.parent.right;
						}
						if ((Color.BLACK == w.left.color) // case 4.b
								&& (Color.BLACK == w.right.color)) {
							w.color = Color.RED;
							if (Color.RED == x.parent.color) {
								x.parent.color = Color.BLACK;
							} else {
								x.parent.req.add(Request.UP_OUT);
								balancer.addRequest(x.parent);
							}
						} else {
							if (Color.BLACK == w.right.color) { // case 4.d
								w.left.color = Color.BLACK;
								w.color = Color.RED;
								rightRotate(w);
								w = x.parent.right;
							}
							w.color = x.parent.color; // case 4.c
							x.parent.color = Color.BLACK;
							w.right.color = Color.BLACK;
							leftRotate(x.parent);
							x = root;
						}
					} else { // same as if clause with "right" and "left"
						// exchanged
						w = x.parent.left;
						if (Color.RED == w.color) {
							w.color = Color.BLACK;
							x.parent.color = Color.RED;
							rightRotate(x.parent);
							w = x.parent.left;
						}
						if ((Color.BLACK == w.right.color)
								&& (Color.BLACK == w.left.color)) {
							w.color = Color.RED;
							if (Color.RED == x.parent.color) {
								x.parent.color = Color.BLACK;
							} else {
								x.parent.req.add(Request.UP_OUT);
								balancer.addRequest(x.parent);
							}
						} else {
							if (Color.BLACK == w.left.color) {
								w.right.color = Color.BLACK;
								w.color = Color.RED;
								leftRotate(w);
								w = x.parent.left;
							}
							w.color = x.parent.color;
							x.parent.color = Color.BLACK;
							w.left.color = Color.BLACK;
							rightRotate(x.parent);
							x = root;
						}
					}
				}
				x.color = Color.BLACK;
			} finally {
				lock.unlock();
			}
		}

		/**
		 * Addtional transformation for tuning.
		 *
		 * @param x
		 */
		private void handleUpOutUpOut(Node<E> x) {
			Node<E> y = x.parent;
			assert (y.left.req.contains(Request.UP_OUT) && y.right.req
					.contains(Request.UP_OUT));
			y.left.req.remove(Request.UP_OUT);
			y.right.req.remove(Request.UP_OUT);

			if (Color.RED == y.color) {
				y.color = Color.BLACK;
			} else {
				y.req.add(Request.UP_OUT);
				balancer.addRequest(y);
			}
		}

		/**
		 * @param z
		 *            start node
		 * @param y
		 *            Parent node
		 * @return node removed
		 */
		private Node<E> removeLeafAndParent(Node<E> z, Node<E> y) {
			// remove leaf and its prarent
			Node<E> x = null; // remaining sibling
			if (z == y.left) { // z is left child
				x = y.right;
			} else {
				x = y.left;
			}
			x.parent = y.parent;
			if (y.parent == sentinel) {
				root = x;
			} else {
				if (y == y.parent.left) {
					y.parent.left = x;
				} else {
					y.parent.right = x;
				}
			}
			return x;
		}

		/**
		 * @param x
		 *            node
		 */
		private void leftRotate(Node<E> x) {
			Node<E> y = x.right;
			x.right = y.left;
			if (y.left != sentinel) {
				y.left.parent = x;
			}
			y.parent = x.parent;
			if (x.parent == sentinel) {
				root = y;
			} else {
				if (x == x.parent.left) {
					x.parent.left = y;
				} else {
					x.parent.right = y;
				}
			}
			y.left = x;
			x.parent = y;
		}

		/**
		 * @param x
		 *            node
		 */
		private void rightRotate(Node<E> x) {
			Node<E> y = x.left;
			x.left = y.right;
			if (y.right != sentinel) {
				y.right.parent = x;
			}
			y.parent = x.parent;
			if (x.parent == sentinel) {
				root = y;
			} else {
				if (x == x.parent.right) {
					x.parent.right = y;
				} else {
					x.parent.left = y;
				}
			}
			y.right = x;
			x.parent = y;
		}
	}

	public void printLeafs() {
		printLeaf(root);
	}

	private void printLeaf(Node<E> x) {
		if (sentinel != x) {
			printLeaf(x.left);
			if (x.left == sentinel && x.right == sentinel)
				System.out.println(x.value);
			printLeaf(x.right);
		}
	}
}
