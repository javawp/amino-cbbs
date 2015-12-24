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

package org.amino.utility;

/**
 * A global elimination array interface.
 *
 * <pre>
 * A Scalable Lock-free Stack Algorithm
 * Danny Hendler                Nir Shavit            Lena Yerushalmi
 * School of Computer Science Tel-Aviv University &amp; School of Computer Science
 *  Tel-Aviv University     Sun Microsystems           Tel-Aviv University
 *  Tel Aviv, Israel 69978      Laboratories          Tel Aviv, Israel 69978
 *  hendlerd@post.tau.ac.il    shanir@sun.com          lenay@post.tau.ac.il
 * </pre>
 *
 * @author Zhi Gan (ganzhi@gmail.com)
 */
public interface IEliminationArray {

	/**
	 * Try to add element to the central data structure.
	 *
	 * @param obj
	 *            the adding object
	 * @param backOff
	 *            time in millisecond for waiting for matching
	 * @return true if match happened between this method and tryRemove(int)
	 *
	 * @throws InterruptedException throw exception if interrupted
	 */
	public abstract boolean tryAdd(Object obj, int backOff)
			throws InterruptedException;

	/**
	 * Try to remove element from central data structure.
	 *
	 * @param backOff
	 *            time in millisecond for waiting for matching
	 * @return null if no match. Argument to tryAdd() method if successful match
	 *
	 * @throws InterruptedException throw exception if be interrupted
	 */
	public abstract Object tryRemove(int backOff) throws InterruptedException;

}