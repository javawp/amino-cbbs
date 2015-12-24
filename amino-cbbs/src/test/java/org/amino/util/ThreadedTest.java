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
package org.amino.util;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.amino.ds.lockfree.LockFreeSet;
import org.amino.util.Parallelized.CheckFor;
import org.amino.util.Parallelized.InitFor;
import org.amino.util.Parallelized.ParallelSetting;
import org.amino.util.Parallelized.Threaded;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

/**
 * @author Zhi Gan
 *
 */
@RunWith(Parallelized.class)
@ParallelSetting(threadNumber = { 1, 2, 4, 8 })
public class ThreadedTest {
	Set<String> strSet;

	@Before
	public void setUp() {
		strSet = new LockFreeSet();
	}

	@Test
	public void doNothing() {

	}

	@InitFor("testThread")
	public void putSomeData(int size){
		strSet.add("putSomeData");
	}

	@Threaded
	public void testThread(int rank, int size) {
		strSet.add("abcde" + rank);
	}

	@CheckFor("testThread")
	public void checkResult(int size) {
		assertEquals(size+1, strSet.size());
	}

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++)
			JUnitCore.runClasses(ThreadedTest.class);
	}
}
