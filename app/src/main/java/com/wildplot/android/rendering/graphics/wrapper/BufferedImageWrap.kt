/****************************************************************************************
 * Copyright (c) 2014 Michael Goldbach <michael></michael>@wildplot.com>                           *
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
package com.wildplot.android.rendering.graphics.wrapper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PorterDuff.Mode

class BufferedImageWrap(width: Int, height: Int, bitmapConfig: Bitmap.Config) {
    val bitmap: Bitmap

    val graphics: GraphicsWrap
        get() = createGraphics()

    init {
        bitmap = Bitmap.createBitmap(width, height, bitmapConfig)
    }

    fun createGraphics(): GraphicsWrap {
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, Mode.CLEAR)
        val paint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.style = Style.STROKE
        //        System.err.println("XFERMODE: "+paint.getXfermode().toString());
        //        Paint transPainter = new Paint();
        //        transPainter.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        //
        //        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), transPainter);
        return GraphicsWrap(canvas, paint)
    }

    companion object {

        val TYPE_INT_ARGB: Bitmap.Config = Bitmap.Config.ARGB_8888
    }


}
