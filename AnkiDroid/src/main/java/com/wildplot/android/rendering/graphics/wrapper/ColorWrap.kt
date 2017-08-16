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

class ColorWrap {
    //android.graphics.Color
    var colorValue: Int = 0
        private set

    constructor(colorValue: Int) : super() {
        this.colorValue = colorValue
    }

    constructor(colorValue: Int, af: Float) : super() {
        val a = Math.round(af * 255)
        val r = android.graphics.Color.red(colorValue)
        val g = android.graphics.Color.green(colorValue)
        val b = android.graphics.Color.blue(colorValue)
        this.colorValue = android.graphics.Color.argb(a, r, g, b)
    }

    constructor(r: Int, g: Int, b: Int) {
        this.colorValue = android.graphics.Color.rgb(r, g, b)
    }

    constructor(r: Int, g: Int, b: Int, a: Int) {
        this.colorValue = android.graphics.Color.argb(a, r, g, b)
    }

    constructor(r: Float, g: Float, b: Float, a: Float) {
        this.colorValue = android.graphics.Color.argb((a * 255).toInt(), (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    constructor(r: Float, g: Float, b: Float) {
        this.colorValue = android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    fun getRed(): Int = android.graphics.Color.red(colorValue)

    fun getGreen(): Int = android.graphics.Color.green(colorValue)

    fun getBlue(): Int = android.graphics.Color.blue(colorValue)

    fun brighter(): ColorWrap {

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorValue, hsv)
        hsv[2] *= 1.6f // value component
        return ColorWrap(android.graphics.Color.HSVToColor(hsv))
    }

    fun darker(): ColorWrap {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorValue, hsv)
        hsv[2] *= 0.8f // value component
        return ColorWrap(android.graphics.Color.HSVToColor(hsv))
    }

    companion object {
        val red = ColorWrap(android.graphics.Color.RED)
        val RED = ColorWrap(android.graphics.Color.RED)

        val BLACK = ColorWrap(android.graphics.Color.BLACK)
        val black = ColorWrap(android.graphics.Color.BLACK)

        val BLUE = ColorWrap(android.graphics.Color.BLUE)
        val blue = ColorWrap(android.graphics.Color.BLUE)

        val CYAN = ColorWrap(android.graphics.Color.CYAN)
        val cyan = ColorWrap(android.graphics.Color.CYAN)

        val DARK_GRAY = ColorWrap(android.graphics.Color.DKGRAY)
        val darkgray = ColorWrap(android.graphics.Color.DKGRAY)

        val GRAY = ColorWrap(android.graphics.Color.GRAY)
        val gray = ColorWrap(android.graphics.Color.GRAY)

        val GREEN = ColorWrap(android.graphics.Color.GREEN)
        val green = ColorWrap(android.graphics.Color.GREEN)

        val LIGHT_GRAY = ColorWrap(android.graphics.Color.LTGRAY)
        val lightGray = ColorWrap(android.graphics.Color.LTGRAY)

        val MAGENTA = ColorWrap(android.graphics.Color.MAGENTA)
        val magenta = ColorWrap(android.graphics.Color.MAGENTA)

        val TRANSPARENT = ColorWrap(android.graphics.Color.TRANSPARENT)

        val WHITE = ColorWrap(android.graphics.Color.WHITE)
        val white = ColorWrap(android.graphics.Color.WHITE)

        val YELLOW = ColorWrap(android.graphics.Color.YELLOW)
        val yellow = ColorWrap(android.graphics.Color.YELLOW)
    }

}
