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

import android.graphics.Rect

class RectangleWrap(rect: Rect) {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    constructor(width: Int, heigth: Int) : this(Rect(0, 0, width, heigth)) {}

    init {

        this.x = rect.left
        this.y = rect.top
        this.height = rect.height()
        this.width = rect.width()
    }

    fun width(): Int = width

    fun height(): Int = height

    fun getRect(): Rect = Rect(x, y, x + width, y + height)

}
