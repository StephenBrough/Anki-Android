/***************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
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

package com.ichi2.utils.themes


import android.content.Context
import android.support.v4.content.ContextCompat

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R

object Themes {
    val ALPHA_ICON_ENABLED_LIGHT = 255 // 100%
    val ALPHA_ICON_DISABLED_LIGHT = 76 // 31%
    val ALPHA_ICON_ENABLED_DARK = 138 // 54%

    // Day themes
    private val THEME_DAY_LIGHT = 0
    private val THEME_DAY_PLAIN = 1
    // Night themes
    private val THEME_NIGHT_BLACK = 0
    private val THEME_NIGHT_DARK = 1


    fun setTheme(context: Context) {
        val prefs = AnkiDroidApp.getSharedPrefs(context.applicationContext)
        if (prefs.getBoolean("invertedColors", false)) {
            val theme = Integer.parseInt(prefs.getString("nightTheme", "0"))
            when (theme) {
                THEME_NIGHT_DARK -> context.setTheme(R.style.Theme_Dark_Compat)
                THEME_NIGHT_BLACK -> context.setTheme(R.style.Theme_Black_Compat)
            }
        } else {
            val theme = Integer.parseInt(prefs.getString("dayTheme", "0"))
            when (theme) {
                THEME_DAY_LIGHT -> context.setTheme(R.style.Theme_Light_Compat)
                THEME_DAY_PLAIN -> context.setTheme(R.style.Theme_Plain_Compat)
            }
        }
    }

    fun setThemeLegacy(context: Context) {
        val prefs = AnkiDroidApp.getSharedPrefs(context.applicationContext)
        if (prefs.getBoolean("invertedColors", false)) {
            val theme = Integer.parseInt(prefs.getString("nightTheme", "0"))
            when (theme) {
                THEME_NIGHT_DARK -> context.setTheme(R.style.LegacyActionBarDark)
                THEME_NIGHT_BLACK -> context.setTheme(R.style.LegacyActionBarBlack)
            }
        } else {
            val theme = Integer.parseInt(prefs.getString("dayTheme", "0"))
            when (theme) {
                THEME_DAY_LIGHT -> context.setTheme(R.style.LegacyActionBarLight)
                THEME_DAY_PLAIN -> context.setTheme(R.style.LegacyActionBarPlain)
            }
        }
    }


    fun getResFromAttr(context: Context, resAttr: Int): Int {
        val attrs = intArrayOf(resAttr)
        return getResFromAttr(context, attrs)[0]
    }

    fun getResFromAttr(context: Context, attrs: IntArray): IntArray {
        val ta = context.obtainStyledAttributes(attrs)
        for (i in attrs.indices) {
            attrs[i] = ta.getResourceId(i, 0)
        }
        ta.recycle()
        return attrs
    }

    fun getColorFromAttr(context: Context, colorAttr: Int): Int {
        val attrs = intArrayOf(colorAttr)
        return getColorFromAttr(context, attrs)[0]
    }


    fun getColorFromAttr(context: Context, attrs: IntArray): IntArray {
        val ta = context.obtainStyledAttributes(attrs)
        for (i in attrs.indices) {
            attrs[i] = ta.getColor(i, ContextCompat.getColor(context, R.color.white))
        }
        ta.recycle()
        return attrs
    }

    /**
     * Return the current integer code of the theme being used, taking into account
     * whether we are in day mode or night mode.
     */
    fun getCurrentTheme(context: Context): Int {
        val prefs = AnkiDroidApp.getSharedPrefs(context)
        return if (prefs.getBoolean("invertedColors", false)) {
            Integer.parseInt(prefs.getString("nightTheme", "0"))
        } else {
            Integer.parseInt(prefs.getString("dayTheme", "0"))
        }
    }
}
