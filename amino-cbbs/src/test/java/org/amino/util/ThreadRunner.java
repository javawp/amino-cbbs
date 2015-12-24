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

package org.amino.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadRunner implements ConcurrentRunner {
	static ThreadRunner runner = new ThreadRunner(null, 0, 0);
	static Map<Integer, ExecutorService> executors = new ConcurrentHashMap<Integer, ExecutorService>();

	public static ThreadRunner getRunner(Class<?> testClass, int nthread,
			int nelement) {
		return getRunner(testClass.getCanonicalName(), nthread, nelement);
	}

	public static ThreadRunner getRunner(String className, int nthread,
			int nelement) {
		runner.className = className;
		runner.setNElement(nelement);
		if (runner.executor == null || runner.nthread != nthread) {
			runner.executor = executors.get(nthread);
			if (runner.executor == null) {
				runner.executor = (ThreadPoolExecutor) Executors
						.newFixedThreadPool(nthread);
				executors.put(nthread, runner.executor);
			}
		}

		runner.nthread = nthread;
		return runner;
	}

	ExecutorService executor;

	public static void shutdown() {
		Set<Integer> keySet = executors.keySet();
		for (Integer key : keySet) {
			executors.get(key).shutdown();
		}
		executors.clear();
	}

	StoreUncaughtExceptionHandler excpHandler = new StoreUncaughtExceptionHandler();

	private String className;

	private int nthread;

	private int nelement;

	private ThreadRunner(Class<?> setClass, int nthread, int nelement) {
		if (setClass != null)
			this.className = setClass.getCanonicalName();
		else
			className = "";
		this.nthread = nthread;
		this.setNElement(nelement);
	}

	public void runThreads(final Runnable[] tasks, String testName)
			throws InterruptedException, ExecutionException {
		System.gc();
		long start = System.nanoTime();

		Future[] futures = new Future[tasks.length];

		for (int i = 0; i < tasks.length; i++) {
			futures[i] = executor.submit(tasks[i]);
		}

		for (int i = 0; i < tasks.length; i++) {
			futures[i].get();
		}

		long end = System.nanoTime();
		Loggers.performance(className, nthread, getNelement(), testName,
				(end - start));
	}

	public void setNElement(int nelement) {
		this.nelement = nelement;
	}

	public int getNelement() {
		return nelement;
	}
}
