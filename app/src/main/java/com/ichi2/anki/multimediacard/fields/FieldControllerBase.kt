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

package com.ichi2.anki.multimediacard.fields

import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.activity.MultimediaEditFieldActivity

abstract class FieldControllerBase : IFieldController {

    protected var mActivity: MultimediaEditFieldActivity? = null
    protected var mField: IField? = null
    protected var mNote: IMultimediaEditableNote? = null
    protected var mIndex: Int = 0


    override fun setField(field: IField) {
        mField = field
    }


    override fun setNote(note: IMultimediaEditableNote) {
        mNote = note
    }


    override fun setFieldIndex(index: Int) {
        mIndex = index
    }


    override fun setEditingActivity(activity: MultimediaEditFieldActivity) {
        mActivity = activity
    }
}
