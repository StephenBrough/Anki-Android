/***************************************************************************************
 * Copyright (c) 2016 Houssam Salem <houssam.salem.au></houssam.salem.au>@gmail.com>                        *
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

package com.ichi2.libanki.importer


import android.content.Context
import android.content.res.Resources
import com.ichi2.anki.deckpicker.model.TaskData

import com.ichi2.utils.async.DeckTask
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Utils

import java.util.ArrayList

abstract class Importer(protected var mCol: Collection, protected var mFile: String) {

    protected var mNeedMapper = false
    protected var mNeedDelimiter = false
    var log: List<String>
        protected set
    protected var mTotal: Int = 0

    private var mTs: Long = 0
    protected var mDst: Collection? = null
    protected var mSrc: Collection? = null

    protected var mContext: Context
    protected var mProgress: DeckTask.ProgressCallback? = null

    protected lateinit var progressUpdate: (TaskData) -> Unit


    protected val res: Resources
        get() = mContext.resources

    init {
        log = ArrayList()
        mTotal = 0
        mContext = mCol.context
    }

    abstract fun runImport()

    /**
     * Timestamps
     * ***********************************************************
     * It's too inefficient to check for existing ids on every object,
     * and a previous import may have created timestamps in the future, so we
     * need to make sure our starting point is safe.
     */

    protected fun _prepareTS() {
        mTs = Utils.maxID(mDst!!.db)
    }


    protected fun ts(): Long {
        mTs++
        return mTs
    }


    /**
     * The methods below are not in LibAnki.
     * ***********************************************************
     */

    fun setProgressCallback(progressCallback: DeckTask.ProgressCallback) {
        mProgress = progressCallback
    }

    fun setProgressCallback(progressCallback: (TaskData) -> Unit ) {
        progressUpdate = progressCallback
    }
}
