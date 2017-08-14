/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha></bibekshrestha>@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial></qutorial>@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul></nicolas.raoul>@gmail.com>                           *
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

package com.ichi2.anki.multimediacard.impl

import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.fields.IField

import java.util.ArrayList

/**
 * Implementation of the editable note.
 *
 *
 * Has to be translate to and from anki db format.
 */

class MultimediaEditableNote : IMultimediaEditableNote {
    internal var mIsModified = false

    internal var mFields: ArrayList<IField> = ArrayList()
    var modelId: Long = 0

    override fun circularSwap() {
        if (mFields.size <= 1) {
            return
        }

        val newFields = ArrayList<IField>()
        newFields.add(mFields[mFields.size - 1])
        newFields.addAll(mFields)
        newFields.removeAt(mFields.size)

        mFields = newFields
    }


    internal fun setThisModified() {
        mIsModified = true
    }


    override val isModified: Boolean
        get() = mIsModified


    // package
    fun setNumFields(numberOfFields: Int) {
        mFields.clear()
//        for (i in 0 until numberOfFields) {
//            mFields.add(null)
//        }
    }


    override val numberOfFields: Int
        get() = mFields.size

    override fun getField(index: Int): IField? {
        return if (index in 0..(numberOfFields - 1)) {
            mFields[index]
        } else null
    }


    override fun setField(index: Int, field: IField): Boolean {
        if (index in 0..(numberOfFields - 1)) {
            // If the same unchanged field is set.
            if (getField(index) === field) {
                if (field.isModified) {
                    setThisModified()
                }
            } else {
                setThisModified()
            }

            mFields[index] = field

            return true
        }
        return false
    }

    companion object {
        private val serialVersionUID = -6161821367135636659L
    }

}
