/****************************************************************************************
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

import android.text.TextUtils

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.preferences.Preferences

import java.util.Locale

/**
 * Utility call for proving language related functionality.
 */
object LanguageUtil {

    /** A list of all languages supported by AnkiDroid  */
    val APP_LANGUAGES = arrayOf("ar", "bg", "ca", "cs", "de", "el", "es-AR", "es-ES", "et", "fa", "fi", "fr", "got", "gl", "hi", "hu", "id", "it", "ja", "ko", "lt", "nl", "no", "pl", "pt_PT", "pt_BR", "ro", "ru", "sk", "sl", "sr", "sv", "th", "tr", "uk", "vi", "zh_CN", "zh_TW", "en")


    /**
     * Returns the [Locale] for the given code or the default locale, if no code is given.
     *
     * @param localeCode The locale code of the language
     * @return The [Locale] for the given code
     */
    val locale: Locale
        get() = getLocale("")

    fun getLocale(localeCode: String?): Locale {
        var localeCode = localeCode
        var locale: Locale
        if (localeCode == null || TextUtils.isEmpty(localeCode)) {

            localeCode = AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().baseContext).getString(
                    Preferences.LANGUAGE, "")
            // If no code provided use the app language.
        }
        locale = if (TextUtils.isEmpty(localeCode)) {
            Locale.getDefault()
            // Fall back to (system) default only if that fails.
        } else if (localeCode!!.length > 2) {
            try {
                Locale(localeCode.substring(0, 2), localeCode.substring(3, 5))
            } catch (e: StringIndexOutOfBoundsException) {
                Locale(localeCode)
            }

        } else {
            Locale(localeCode)
        }
        return locale
    }

}
