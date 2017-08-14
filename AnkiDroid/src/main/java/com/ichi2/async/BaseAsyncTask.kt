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

package com.ichi2.async

import android.os.AsyncTask

import com.ichi2.utils.MethodLogger
import com.ichi2.utils.Threads

@Suppress("ConstantConditionIf")
open class BaseAsyncTask<Params, Progress, Result> : AsyncTask<Params, Progress, Result>() {
    init {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkMainThread()
    }


    override fun onPreExecute() {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkMainThread()
        super.onPreExecute()
    }


    override fun onPostExecute(result: Result) {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkMainThread()
        super.onPostExecute(result)
    }


    override fun onProgressUpdate(vararg values: Progress) {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkMainThread()
        super.onProgressUpdate(*values)
    }


    override fun onCancelled() {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkMainThread()
        super.onCancelled()
    }


    override fun doInBackground(vararg arg0: Params): Result? {
        if (DEBUG) {
            MethodLogger.log()
        }
        Threads.checkNotMainThread()
        return null
    }

    companion object {

        /** Set this to `true` to enable detailed debugging for this class.  */
        private val DEBUG = false

        init {
            // This can actually happen if the first reference to an AsyncTask is made not from the main thread.
            //
            // In that case, the static constructor will be invoked by the class loader on the thread that is making the
            // reference.
            //
            // Unfortunately this leads to unexpected consequences, including a Handler being constructed on the wrong
            // thread.
            //
            // See https://code.google.com/p/android/issues/detail?id=20915
            if (DEBUG) {
                MethodLogger.log()
            }
            Threads.checkMainThread()
        }
    }

}
