/****************************************************************************************
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
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
package com.ichi2.anki.widgets

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

import com.getbase.floatingactionbutton.FloatingActionsMenu

/**
 * Originally created by Paul Woitaschek (http://www.paul-woitaschek.de, woitaschek@posteo.de)
 * Defines the behavior for the floating action button. If the dependency is a Snackbar, move the
 * fab up.
 */
class FabBehavior : CoordinatorLayout.Behavior<FloatingActionsMenu> {

    private var mTranslationY: Float = 0.toFloat()

    constructor() : super() {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    private fun getFabTranslationYForSnackbar(parent: CoordinatorLayout?, fab: FloatingActionsMenu): Float {
        var minOffset = 0.0f
        val dependencies = parent!!.getDependencies(fab)
        var i = 0

        val z = dependencies.size
        while (i < z) {
            val view = dependencies[i] as View
            if (view is Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - view.getHeight().toFloat())
            }
            ++i
        }

        return minOffset
    }

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: FloatingActionsMenu?, dependency: View?): Boolean =
            dependency is Snackbar.SnackbarLayout

    override fun onDependentViewChanged(parent: CoordinatorLayout?, fab: FloatingActionsMenu?, dependency: View?): Boolean {
        if (dependency is Snackbar.SnackbarLayout && fab!!.visibility == View.VISIBLE) {
            val translationY = getFabTranslationYForSnackbar(parent, fab)
            if (translationY != this.mTranslationY) {
                ViewCompat.animate(fab).cancel()
                ViewCompat.setTranslationY(fab, translationY)
                this.mTranslationY = translationY
            }
        }
        return false
    }
}