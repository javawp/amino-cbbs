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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class AbstractBaseTest {
	protected Class<?> classTested;
	protected Object[] params;
	protected String optionalLabel;

	protected static List<Integer> AMINO_NTHREAD;
	protected static int AMINO_NELEMENT;
	protected static int AMINO_REPEAT_COUNT;

	protected int NTHREAD;
	protected int NELEMENT;

	protected static String needLog;

	protected static Properties prop;
	private static String confFile;

	static {
		prop = new Properties();

		confFile = System.getenv("AMINO_CONF_FILE");
		if (confFile != null) {
			try {
				System.out.println("Load configure file " + confFile);
				prop.load(new FileInputStream(confFile));
			} catch (FileNotFoundException e1) {
			} catch (IOException e1) {
			}
		}
		if (prop.size() == 0) {
			System.out
					.println("Didn't find a configure file! Please setup environment AMINO_CONF_FILE pointing to your configuration.");
		}
		System.out.println();
		prop.list(System.out);
		System.out.println("-- end of listing properties --\n");

		needLog = prop.getProperty("AMINO_TEST_LOG", "false");

		try {
			AMINO_NELEMENT = Integer.valueOf(prop.getProperty("AMINO_ELEMENTS",
					"256"));
		} catch (NumberFormatException e) {

		} finally {
			if (AMINO_NELEMENT == 0) {
				AMINO_NELEMENT = 512;
			}
		}

		try {
			AMINO_NTHREAD = new ArrayList<Integer>();
			String thread_str = prop.getProperty("AMINO_THREADS",
					"8,1,2,3,4,5,6,7,8");
			if (thread_str != null) {
				String[] threads = thread_str.split(", *");
				for (int i = 0; i < threads.length; i++) {
					AMINO_NTHREAD.add(Integer.valueOf(threads[i]));
				}
			}
		} catch (NumberFormatException e) {

		} finally {
			if (AMINO_NTHREAD == null || AMINO_NTHREAD.size() == 0) {
				AMINO_NTHREAD = new ArrayList<Integer>();
				AMINO_NTHREAD.add(4);
			}
		}

		try {
			AMINO_REPEAT_COUNT = Integer.valueOf(prop.getProperty(
					"AMINO_REPEAT_COUNT", "10"));
		} catch (NumberFormatException e) {

		} finally {
			if (AMINO_REPEAT_COUNT == 0) {
				AMINO_REPEAT_COUNT = 16;
			}
		}

		System.out.println("AMINO_ELEMENTS = " + AMINO_NELEMENT);
		System.out.println("AMINO_THREADS = " + AMINO_NTHREAD);
		System.out.println("AMINO_REPEAT_COUNT = " + AMINO_REPEAT_COUNT);
	}

	public AbstractBaseTest(Object classTested, Object[] params, int nthread,
			int nelement) {
		this.classTested = (Class<?>) classTested;
		this.params = params;
		NTHREAD = nthread;
		NELEMENT = nelement;
	}

	public AbstractBaseTest(Object classTested, Object[] params, int nthread,
			int nelement, String optionalLabel) {
		this(classTested, params, nthread, nelement);
		this.optionalLabel = optionalLabel;
	}

	/**
	 * This method will generate workload which increases as thread number
	 * increases. Workload per thread remains a constant.
	 * 
	 * @param tClass
	 *            type of testing objects. It will be used by getInstance()
	 *            method.
	 * @param params
	 *            Parameter used to initialize testing objects.
	 * @return A list of parameters to initialize test case. Please refer to
	 *         Parameterized class of JUnit
	 */
	protected static Collection<Object[]> genWorkLoad1(Class<?> tClass,
			Object[] params) {
		List<Object[]> args = new ArrayList<Object[]>();

		Collection<Integer> nums = getThreadNums();
		int elementNum = getElementNum();

		for (int threadNum : nums) {
			Object[] item = new Object[] { tClass, params, threadNum,
					elementNum };
			args.add(item);
		}

		return args;
	}

	/**
	 * This method will generate workload which remains constant for any number
	 * of threads. If thread number increase, the workload per thread will
	 * decrease.
	 * 
	 * @param tClass
	 *            type of testing objects. It will be used by getInstance()
	 *            method.
	 * @param params
	 *            Parameter used to initialize testing objects.
	 * @return A list of parameters to initialize test case. Please refer to
	 *         Parameterized class of JUnit
	 */
	protected static Collection<Object[]> genWorkLoad2(Class<?> tClass,
			Object[] params) {
		List<Object[]> args = new ArrayList<Object[]>();

		Collection<Integer> nums = getThreadNums();
		int elementNum = getElementNum();

		for (int threadNum : nums) {
			Object[] item = new Object[] { tClass, params, threadNum,
					elementNum / threadNum };
			args.add(item);
		}

		return args;
	}

	protected static Collection<Object[]> genArguments(Class<?> tClass,
			Object[] params, String label) {
		List<Object[]> args = new ArrayList<Object[]>();

		Collection<Integer> nums = getThreadNums();
		int elementNum = AMINO_NELEMENT;

		for (int threadNum : nums) {
			Object[] item = new Object[] { tClass, params, threadNum,
					elementNum, label };
			args.add(item);
		}

		return args;
	}

	protected Object getInstance() throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, InvocationTargetException {
		if (params.length == 0) {
			Object instance = classTested.newInstance();
			return instance;
		} else if (params.length == 1) {
			Class<?> type = params[0].getClass();
			if (type.equals(Integer.class))
				type = int.class;
			Constructor<?> constructor = classTested.getConstructor(type);
			return constructor.newInstance(params[0]);
		} else { // FIXME: need to differ Integer and int
			Class<?>[] types = new Class[params.length];
			for (int i = 0; i < types.length; i++) {
				types[i] = params[i].getClass();
				if (types[i].equals(Integer.class)) {
					types[i] = int.class;
				} else if (types[i].equals(Double.class)) {
					types[i] = Double.TYPE;
				} else if (types[i].equals(Float.class)) {
					types[i] = Float.TYPE;
				}
			}

			Constructor<?> constructor = classTested.getConstructor(types);
			return constructor.newInstance(params);
		}
	}

	public static List<Integer> getThreadNums() {
		return AMINO_NTHREAD;
	}

	public static int getElementNum() {
		return AMINO_NELEMENT;
	}

	public static int getRepeatCount() {
		return AMINO_REPEAT_COUNT;
	}

	public static String needLogFile() {
		if (needLog != null && !needLog.equalsIgnoreCase("false"))
			return needLog;
		else
			return null;
	}
}
