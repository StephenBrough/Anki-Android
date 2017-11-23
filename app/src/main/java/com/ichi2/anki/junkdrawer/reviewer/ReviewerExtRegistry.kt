/****************************************************************************************
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

package com.ichi2.anki.junkdrawer.reviewer

import android.content.Context

/**
 * Keeps track of all the [ReviewerExt] implementations and delegates to them.
 */
class ReviewerExtRegistry
/**
 * Creates the list of extensions.
 *
 *
 * Must be called at the beginning of onCreate().
 */
(context: Context) : ReviewerExt {

    /**
     * The registered extensions.
     */
    private val mReviewerExts: Array<ReviewerExt> = arrayOf(CustomFontsReviewerExt(context))


    override fun updateCssStyle(cssStyle: StringBuilder) {
        for (ext in mReviewerExts) {
            ext.updateCssStyle(cssStyle)
        }
    }

}
