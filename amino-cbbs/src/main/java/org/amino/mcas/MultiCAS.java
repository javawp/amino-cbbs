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

package org.amino.mcas;

import org.amino.mcas.UnsafeWrapper;

import sun.misc.Unsafe;

/**
 * @author Xiao Jun Dai
 *
 * Status of MCAS processing
 */
enum Status {
    UNDECIDED, SUCCESSFUL, FAILED
}

/**
 * Multi CAS operation. It is based on Practical lock-freedom by Keir Fraser.
 *
 * @author Xiao Jun Dai
 *
 */
class MultiCAS {

    /**
     * MCAS descriptor installed in the memory.
     *
     */
    private static class MCASDesc {
        int N;
        Object[] obj;
        long[] offset;
        Object[] e;
        Object[] n;
        volatile Status status;

        static final long STATUS_OFFSET;

        static {
            try {
                STATUS_OFFSET = unsafe.objectFieldOffset(MCASDesc.class
                        .getDeclaredField("status"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public MCASDesc(int N, Object[] obj, long[] offset, Object[] e,
                Object[] n) {
            this.N = N;
            this.obj = obj;
            this.offset = offset;
            this.e = e;
            this.n = n;
            this.status = Status.UNDECIDED;
        }
    }

    /**
     * CCAS descriptor install in hte memory.
     *
     */
    private static class CCASDesc {
        /**
         * object to CAS.
         */
        Object obj;
        /**
         * offset of field in object.
         */
        long offset;
        /**
         * expected value.
         */
        Object e;
        /**
         * new value.
         */
        Object n;
        /**
         * condition.
         */
        Status cond;

        public CCASDesc(Object obj, long offset, Object e, Object n, Status cond) {
            this.obj = obj;
            this.offset = offset;
            this.e = e;
            this.n = n;
            this.cond = cond;
        }
    }

    /**
     * unsafe object to use sun.mics.Unsafe.
     */
    private static final Unsafe unsafe = UnsafeWrapper.getUnsafe();

    /**
     * @param N
     *            number of memory field to be compare and set
     * @param obj
     *            array of object address
     * @param offset
     *            array of offset for every object
     * @param e
     *            array of expected value
     * @param n
     *            array of new value
     * @return true if multi compare and swap is successful, otherwise false
     */
    static boolean mcas(int N, Object[] obj, long[] offset, Object[] e,
            Object[] n) {
        MCASDesc d = new MCASDesc(N, obj, offset, e, n);
        // System.out.println("before sort address");
        // for (int i = 0; i < N; i++) {
        // System.out.println(obj[i] + ":" + ((ObjectID) obj[i]).getID());
        // }

        addressSort(d, N); // Memory locations must be sorted into address
        // order;

        // System.out.println("after sort address");
        // for (int i = 0; i < N; i++) {
        // System.out.println(obj[i] + ":" + ((ObjectID) obj[i]).getID());
        // }

        return mcasHelp(d);
    }

    private static void addressSort(MCASDesc d, int N) {
        Object[] obj = d.obj;
        long[] offset = d.offset;
        Object[] e = d.e;
        Object[] n = d.n;
        int i;

        int key;
        Object tempO;
        long tempOff;
        Object tempE;
        Object tempN;

        for (int j = 1; j < N; j++) {
            tempO = obj[j];
            tempOff = offset[j];
            tempE = e[j];
            tempN = n[j];

            key = ((ObjectID) tempO).id;
            i = j - 1;

            while (i >= 0 && ((ObjectID) obj[i]).id > key) {
                obj[i + 1] = obj[i];
                offset[i + 1] = offset[i];
                e[i + 1] = e[i];
                n[i + 1] = n[i];

                i--;
            }

            obj[i + 1] = tempO;
            offset[i + 1] = tempOff;
            e[i + 1] = tempE;
            n[i + 1] = tempN;
        }
    }

    /**
     * @param obj
     *            Object be read
     * @param offset
     *            offset of field in object
     * @return read value
     */
    static Object mcasRead(Object obj, long offset) {
        Object v;
        for (v = ccasRead(obj, offset); isMCASDesc(v); v = ccasRead(obj, offset)) {
            mcasHelp((MCASDesc) v);
        }
        return v;
    }

    private static boolean mcasHelp(MCASDesc d) {
        // System.out.println(d);
        Object v;
        Status desired = Status.FAILED;
        boolean success;
        int N = d.N;

        /* PHASE 1: Attempt to acquire each location in turn */
        decision_point: while (true) {
            for (int i = 0; i < N; i++) {
                while (true) {
                    ccas(d.obj[i], d.offset[i], d.e[i], d, d.status);
                    // if CCAS fails
                    v = unsafe.getObject(d.obj[i], d.offset[i]);
                    // try {
                    // Thread.sleep(ran.nextInt(100));
                    // } catch (InterruptedException e) {
                    // e.printStackTrace();
                    // }
                    if ((v == d.e[i]) && (d.status == Status.UNDECIDED)) {
                        continue;
                    }
                    if (v == d) {/* Move to next location */
                        break;
                    }
                    if (!isMCASDesc(v)) {
                        break decision_point;
                    }
                    mcasHelp((MCASDesc) v);
                }
            }
            desired = Status.SUCCESSFUL;
            break decision_point;
        }

        /* decision point */
        unsafe.compareAndSwapObject(d, MCASDesc.STATUS_OFFSET,
                Status.UNDECIDED, desired);

        /* PHASE 2: Release each location that we hold */
        success = (d.status == Status.SUCCESSFUL);
        for (int i = 0; i < N; ++i) {
            unsafe.compareAndSwapObject(d.obj[i], d.offset[i], d,
                    success ? d.n[i] : d.e[i]);
        }
        return success;
    }

    private static boolean isMCASDesc(Object v) {
        return v instanceof MCASDesc;
    }

    /**
     * Conditional CAS.
     *
     * @param obj
     *            object be ccas
     * @param offset
     *            file offset of object
     * @param e
     *            expected value
     * @param n
     *            new value
     * @param cond
     *            conditon
     */
    static void ccas(Object obj, long offset, Object e, Object n, Status cond) {
        CCASDesc d = new CCASDesc(obj, offset, e, n, cond);
        Object v;

        while (unsafe.compareAndSwapObject(d.obj, d.offset, d.e, d) != true) {
            v = unsafe.getObject(d.obj, d.offset);
            if (!isCCASDesc(v)) {
                return;
            }
            ccasHelp((CCASDesc) v);
        }
        ccasHelp(d);
    }

    /**
     * @param obj
     *            obejct read
     * @param offset
     *            field offset of object
     * @return object read
     */
    static Object ccasRead(Object obj, long offset) {
        Object v;
        for (v = unsafe.getObject(obj, offset); isCCASDesc(v); v = unsafe
                .getObject(obj, offset)) {
            ccasHelp((CCASDesc) v);
        }
        return v;
    }

    private static void ccasHelp(CCASDesc d) {
        boolean success = (d.cond == Status.UNDECIDED);
        unsafe.compareAndSwapObject(d.obj, d.offset, d, success ? d.n : d.e);
    }

    private static boolean isCCASDesc(Object v) {
        return v instanceof CCASDesc;
    }
}