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

package com.ichi2.anki.reviewer

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.AnkiFont
import com.ichi2.libanki.Utils

import java.util.HashMap

class CustomFontsReviewerExt(context: Context) : ReviewerExt {

    private val mCustomStyle: String
    private var mDefaultFontStyle: String? = null
    private var mOverrideFontStyle: String? = null
    private var mThemeFontStyle: String? = null
    private var mDominantFontStyle: String? = null


    /**
     * Returns the CSS used to set the theme font.
     *
     * @return the font style, or the empty string if no font is set
     */
    private val themeFontStyle: String
        get() {
            if (mThemeFontStyle == null) {
                val themeFontName = "OpenSans"
                if (TextUtils.isEmpty(themeFontName)) {
                    mThemeFontStyle = ""
                } else {
                    mThemeFontStyle = String.format(
                            "BODY {"
                                    + "font-family: '%s';"
                                    + "font-weight: normal;"
                                    + "font-style: normal;"
                                    + "font-stretch: normal;"
                                    + "}\n", themeFontName)
                }
            }
            return mThemeFontStyle!!
        }

    init {
        val customFontsMap = getCustomFontsMap(context)
        mCustomStyle = getCustomFontsStyle(customFontsMap) + getDominantFontStyle(context, customFontsMap)
    }


    override fun updateCssStyle(cssStyle: StringBuilder) {
        cssStyle.append(mCustomStyle)
    }


    /**
     * Returns the CSS used to handle custom fonts.
     *
     *
     * Custom fonts live in fonts directory in the directory used to store decks.
     *
     *
     * Each font is mapped to the font family by the same name as the name of the font without the extension.
     */
    private fun getCustomFontsStyle(customFontsMap: Map<String, AnkiFont>): String {
        val builder = StringBuilder()
        for (font in customFontsMap.values) {
            builder.append(font.declaration)
            builder.append('\n')
        }
        return builder.toString()
    }


    /**
     * Returns the CSS used to set the default font.
     *
     * @return the default font style, or the empty string if no default font is set
     */
    private fun getDefaultFontStyle(context: Context, customFontsMap: Map<String, AnkiFont>): String {
        if (mDefaultFontStyle == null) {
            val preferences = AnkiDroidApp.getSharedPrefs(context)
            val defaultFont = customFontsMap[preferences.getString("defaultFont", null)]
            mDefaultFontStyle = if (defaultFont != null) {
                "BODY { " + defaultFont.getCSS(false) + " }\n"
            } else {
                ""
            }
        }
        return mDefaultFontStyle!!
    }


    /**
     * Returns the CSS used to set the override font.
     *
     * @return the override font style, or the empty string if no override font is set
     */
    private fun getOverrideFontStyle(context: Context, customFontsMap: Map<String, AnkiFont>): String {
        if (mOverrideFontStyle == null) {
            val preferences = AnkiDroidApp.getSharedPrefs(context)
            val defaultFont = customFontsMap[preferences.getString("defaultFont", null)]
            val overrideFont = preferences.getString("overrideFontBehavior", "0") == "1"
            mOverrideFontStyle = if (defaultFont != null && overrideFont) {
                "BODY, .card, * { " + defaultFont.getCSS(true) + " }\n"
            } else {
                ""
            }
        }
        return mOverrideFontStyle!!
    }


    /**
     * Returns the CSS that determines font choice in a global fashion.
     *
     * @return the font style, or the empty string if none applies
     */
    private fun getDominantFontStyle(context: Context, customFontsMap: Map<String, AnkiFont>): String {
        if (mDominantFontStyle == null) {
            mDominantFontStyle = getOverrideFontStyle(context, customFontsMap)
            if (TextUtils.isEmpty(mDominantFontStyle)) {
                mDominantFontStyle = getDefaultFontStyle(context, customFontsMap)
                if (TextUtils.isEmpty(mDominantFontStyle)) {
                    mDominantFontStyle = themeFontStyle
                }
            }
        }
        return mDominantFontStyle as String
    }


    /**
     * Returns a map from custom fonts names to the corresponding [AnkiFont] object.
     *
     *
     * The list of constructed lazily the first time is needed.
     */
    private fun getCustomFontsMap(context: Context): Map<String, AnkiFont> {
        val fonts = Utils.getCustomFonts(context)
        val customFontsMap = HashMap<String, AnkiFont>()
        for (f in fonts) {
            customFontsMap.put(f.name, f)
        }
        return customFontsMap
    }

}
