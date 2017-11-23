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

import java.io.Serializable

/**
 * General interface for a field of any type.
 */
interface IField : Serializable {
    val type: EFieldType


    var isModified: Boolean


    // For mixed type
    var html: String?


    // For image type. Resets type.
    // Makes no sense to call when type is not image.
    // the same for other groups below.
    var imagePath: String?


    var audioPath: String?


    // For Text type
    var text: String?


    var name: String?


    /**
     * Returns the formatted value for this field. Each implementation of IField should return in a format which will be
     * used to store in the database
     *
     * @return
     */
    val formattedValue: String?


    /**
     * Mark if the current media path is temporary and if it should be deleted once the media has been processed.
     *
     * @param hasTemporaryMedia True if the media is temporary, False if it is existing media.
     * @return
     */
    fun setHasTemporaryMedia(hasTemporaryMedia: Boolean)


    fun hasTemporaryMedia(): Boolean


    fun setFormattedString(col: Collection, value: String)
}
