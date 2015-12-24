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

//import org.amino.ds.tree.LockFreeBSTreeTest;
//import org.amino.ds.tree.ParallelRBTreeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Zhi Gan
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { IteratorTest.class, GraphTest.class, StackTest.class,
		QueueTest.class, DequeTest.class,
		ListTest.class, VectorTest.class,
		MapTest.class, SetTest.class/*, PQueueTest.class, ParallelRBTreeTest.class, LockFreeBSTreeTest.class */ })
public class DSTests {

}
