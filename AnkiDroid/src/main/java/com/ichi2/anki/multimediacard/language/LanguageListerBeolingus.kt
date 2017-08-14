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

import android.content.Context

import java.util.Locale

/**
 * This one listers services in beolingus.
 *
 *
 * It is used to load pronunciation.
 */
class LanguageListerBeolingus : LanguageListerBase() {

    init {

        addLanguage(Locale("eng").displayLanguage, "en-de")
        addLanguage(Locale("deu").displayLanguage, "deen")
        addLanguage(Locale("spa").displayLanguage, "es-de")
        // Seems to have no pronunciation yet
        // addLanguage(context.getString(R.string.multimedia_editor_languages_portuguese), "pt-de");
    }

}
