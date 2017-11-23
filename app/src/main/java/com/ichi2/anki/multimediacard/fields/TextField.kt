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

import com.ichi2.libanki.Collection

/**
 * Text Field implementation.
 */
class TextField : FieldBase(), IField {
    override var text: String? = ""
        get() = field
        set(value) {
            field = value
            thisModified = true
        }

    override var name: String? = null

    override var type: EFieldType = EFieldType.TEXT

    override var isModified: Boolean = thisModified

    override var html: String? = null

    override var imagePath: String?
        get() = null
        set(s) {

        }

    override var audioPath: String?
        get() = null
        set(s) {

        }


    override val formattedValue: String?
        get() = text

    override fun setHasTemporaryMedia(hasTemporaryMedia: Boolean) {}

    override fun hasTemporaryMedia(): Boolean = false

    override fun setFormattedString(col: Collection, value: String) {
        text = value
    }

    companion object {
        private val serialVersionUID = -6508967905716947525L
    }
}
