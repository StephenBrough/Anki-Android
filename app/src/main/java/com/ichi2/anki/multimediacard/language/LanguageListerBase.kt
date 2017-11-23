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

package com.ichi2.anki.multimediacard.language

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

/**
 * This is some sort of tool, which translates from languages in a user readable form to a code, used to invoke some
 * service. This code depends on service, of course.
 *
 *
 * Specific language listers derive from this one.
 */
open class LanguageListerBase {

    private var mLanguageMap: HashMap<String, String> = HashMap()


    val languages: ArrayList<String>
        get() {
            val res = ArrayList<String>()
            res.addAll(mLanguageMap.keys)
            Collections.sort(res) { text1, text2 -> text1.compareTo(text2, ignoreCase = true) }
            return res
        }


    /**
     * @param name
     * @param code This one has to be used in constructor to fill the hash map.
     */
    protected fun addLanguage(name: String, code: String) {
        mLanguageMap.put(name, code)
    }


    fun getCodeFor(Language: String): String {
        return if (mLanguageMap.containsKey(Language)) {
            mLanguageMap[Language].toString()
        } else ""

    }

}
