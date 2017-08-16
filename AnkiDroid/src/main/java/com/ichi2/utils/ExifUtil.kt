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

package com.ichi2.utils

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface

import java.io.File

@TargetApi(5)
object ExifUtil {
    fun rotateFromCamera(theFile: File, bmp: Bitmap): Bitmap {
        var bmp = bmp
        try {
            val exif = ExifInterface(theFile.path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            var angle = 0

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270
            }

            val mat = Matrix()
            mat.postRotate(angle.toFloat())

            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mat, true)
            return bmp
        } catch (e: Exception) {
            return bmp
        }

    }
}
