/****************************************************************************************
 * Copyright (c) 2013 Flavio Lerda <flerda></flerda>@gmail.com>                                   *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */

package com.ichi2.utils

import android.os.Looper

/**
 * Helper class for checking for programming errors while using threads.
 */
object Threads {


    /**
     * @return true if called from the application main thread
     */
    val isOnMainThread: Boolean
        get() = Looper.getMainLooper() == Looper.myLooper()

    /**
     * An object used to check a thread-access policy.
     *
     *
     * It will verify that calls to its [.checkThread] method are done on the right thread.
     */
    interface ThreadChecker {

        /**
         * Checks that it is called from the right thread and fails otherwise.
         */
        fun checkThread()
    }


    /**
     * Creates a [ThreadChecker] that validates all access are done on the given thread.
     *
     * @param thread on which accesses should occur
     */
    fun newSingleThreadChecker(thread: Thread?): ThreadChecker {
        if (thread == null) {
            throw IllegalArgumentException("thread should not be null")
        }
        return SingleThreadChecker(thread)
    }


    /**
     * Creates a [ThreadChecker] that validates all access are done on the calling thread.
     */
    fun newCurrentThreadChecker(): ThreadChecker {
        return SingleThreadChecker(Thread.currentThread())
    }


    /**
     * Creates a [ThreadChecker] that validates all access on the same thread, without specifying which thread.
     *
     *
     * The thread will be determined by the first call to [ThreadChecker.checkThread] and enforced thereafter.
     */
    fun newLazySingleThreadChecker(): ThreadChecker = SingleThreadChecker(null)


    /**
     * Checks that it is called from the main thread and fails if it is called from another thread.
     */
    fun checkMainThread() {
        if (!isOnMainThread) {
            throw IllegalStateException("must be called on the main thread instead of " + Thread.currentThread())
        }
    }


    /**
     * Checks that it is not called from the main thread and fails if it is.
     */
    fun checkNotMainThread() {
        if (isOnMainThread) {
            throw IllegalStateException("must not be called on the main thread")
        }
    }

    /**
     * Helper class to track access from a single thread.
     *
     *
     * This class can be used to validate a single-threaded access policy to a class.
     *
     *
     * Each method that needs to be called from a single thread can simply call [.checkThread] to validate the
     * thread it is being called.
     */
    private class SingleThreadChecker
    /**
     * Creates a checker for the given thread.
     *
     *
     * If passed `null`, it will detect the first thread that calls [.checkThread] and make sure all
     * future accesses are from that thread.
     *
     * @param thread that is allowed access
     */
    (
            /** The thread that is allowed access.  */
            private var mThread: Thread?) : ThreadChecker {


        override fun checkThread() {
            // If this the first access and we have not specified a thread, record the current thread.
            if (mThread == null) {
                mThread = Thread.currentThread()
                return
            }
            if (mThread !== Thread.currentThread()) {
                throw IllegalStateException("must be called from single thread: " + mThread + " instead of "
                        + Thread.currentThread())
            }
        }

    }

}
