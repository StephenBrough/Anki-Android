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
import java.io.File
import java.util.regex.Pattern

/**
 * Field with an image.
 */
class ImageField : FieldBase(), IField {
    private var mImagePath: String? = null
    private var mHasTemporaryMedia = false
    override var name: String? = null

    override val type: EFieldType = EFieldType.IMAGE

    override var isModified: Boolean = thisModified
        get() = thisModified


    override var html: String? = null
        get() = null


    override var imagePath: String?
        get() = mImagePath
        set(pathToImage) {
            mImagePath = pathToImage
            thisModified = true
        }


    override var audioPath: String?
        get() = null
        set(s) {

        }

    override var text: String? = null

    override val formattedValue: String
        get() {
            val file = File(imagePath!!)
            return if (file.exists()) {
                String.format("<img src='%s'/>", file.name)
            } else {
                ""
            }
        }


    override fun setHasTemporaryMedia(hasTemporaryMedia: Boolean) {
        mHasTemporaryMedia = hasTemporaryMedia
    }

    override fun hasTemporaryMedia(): Boolean = mHasTemporaryMedia


    override fun setFormattedString(col: Collection, value: String) {
        val p = Pattern.compile(PATH_REGEX)
        val m = p.matcher(value)
        var res = ""
        if (m.find()) {
            res = m.group(1)
        }
        val mediaDir = col.media.dir() + "/"
        imagePath = mediaDir + res
    }

    companion object {
        private val serialVersionUID = 4431611060655809687L

        private val PATH_REGEX = "<img.*src=[\"'](.*)[\"'].*/?>"
    }
}
