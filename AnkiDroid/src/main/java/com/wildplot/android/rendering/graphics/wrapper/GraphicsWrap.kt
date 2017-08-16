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
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Xfermode

/**
 * Wrapper of swing/awt graphics class for android use
 * @author Michael Goldbach
 */
class GraphicsWrap(private val canvas: Canvas, val paint: Paint) {

    var stroke: StrokeWrap
        get() = StrokeWrap(paint.strokeWidth)
        set(stroke) {
            paint.strokeWidth = stroke.strokeSize
        }

    val clipBounds: RectangleWrap
        get() = RectangleWrap(canvas.clipBounds)

    var color: ColorWrap
        get() = ColorWrap(paint.color)
        set(color) {
            paint.color = color.colorValue
        }

    // TODO Auto-generated method stub
    val font: Any?
        get() = null

    val fontMetrics: FontMetricsWrap
        get() = FontMetricsWrap(this)

    var fontSize: Float
        get() = paint.textSize
        set(size) {
            paint.textSize = size
        }
    var typeface: Typeface
        get() = paint.typeface
        set(typeface) {
            paint.typeface = typeface
        }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        val oldStyle = paint.style
        paint.style = Style.FILL_AND_STROKE
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.style = oldStyle
    }

    fun drawRect(x: Float, y: Float, width: Float, height: Float) {
        val oldStyle = paint.style
        paint.style = Style.STROKE
        canvas.drawRect(x, y, x + width, y + height, paint)
        paint.style = oldStyle
    }

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        //        boolean isAntiAlias = paint.isAntiAlias();
        //
        //        paint.setAntiAlias(true);
        val oldStyle = paint.style
        paint.style = Style.FILL
        canvas.drawRect(x, y, x + width, y + height, paint)
        paint.style = oldStyle
        //        paint.setAntiAlias(isAntiAlias);
    }

    fun setClip(rectangle: RectangleWrap) {
        //seems to be not necessary
    }

    fun drawArc(x: Float, y: Float, width: Float, height: Float, startAngle: Float, arcAngle: Float) {
        if (arcAngle == 0f) {
            return
        }
        val oldStyle = paint.style
        paint.style = Style.STROKE
        val rectF = RectF(x, y, x + width, y + height)
        canvas.drawArc(rectF, startAngle, arcAngle, true, paint)
        paint.style = oldStyle
    }

    fun fillArc(x: Float, y: Float, width: Float, height: Float, startAngle: Float, arcAngle: Float) {
        if (arcAngle == 0f) {
            return
        }
        val oldStyle = paint.style
        paint.style = Style.FILL
        val rectF = RectF(x, y, x + width, y + height)
        canvas.drawArc(rectF, startAngle, arcAngle, true, paint)
        paint.style = oldStyle
    }

    fun drawImage(image: BufferedImageWrap, tmp: String, x: Float, y: Float) {
        //System.err.println("drawImage: " + image.getBitmap().getWidth() + " : "+ image.getBitmap().getHeight());
        val mode = paint.xfermode
        paint.xfermode = PorterDuffXfermode(Mode.SRC_OVER)
        //canvas.drawBitmap(image.getBitmap(), x, y, paint);
        val bitmap = image.bitmap
        bitmap.prepareToDraw()
        canvas.drawBitmap(bitmap, canvas.clipBounds, canvas.clipBounds, paint)
        paint.xfermode = mode
    }

    fun drawString(text: String, x: Float, y: Float) {
        val oldStyle = paint.style
        paint.style = Style.FILL
        canvas.drawText(text, x, y, paint)
        paint.style = oldStyle

    }

    fun getFontMetrics(font: Any): FontMetricsWrap = FontMetricsWrap(this)

    fun dispose() {
        //TODO: search if there is something to do with it
    }

    fun save(): Int = canvas.save()

    fun restore() {
        canvas.restore()
    }

    fun rotate(degree: Float, x: Float, y: Float) {
        canvas.rotate(degree, x, y)
    }

    fun setShadow(radius: Float, dx: Float, dy: Float, color: ColorWrap) {
        val colorVal = color.colorValue
        paint.setShadowLayer(radius, dx, dy, colorVal)
    }

    fun unsetShadow() {
        paint.clearShadowLayer()
    }

}
