/***************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
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

package com.ichi2.preferences

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences.Editor
import android.preference.DialogPreference
import android.util.AttributeSet
import android.widget.Toast

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.MetaDB
import com.ichi2.anki.R

class CustomDialogPreference(private val mContext: Context, attrs: AttributeSet) : DialogPreference(mContext, attrs), DialogInterface.OnClickListener {


    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (this.title == mContext.resources.getString(R.string.deck_conf_reset)) {
                // Deck Options :: Restore Defaults for Options Group
                val editor = AnkiDroidApp.getSharedPrefs(mContext).edit()
                editor.putBoolean("confReset", true)
                editor.commit()
            } else if (this.title == mContext.resources.getString(R.string.deck_conf_remove)) {
                // Deck Options :: Remove Options Group
                val editor = AnkiDroidApp.getSharedPrefs(mContext).edit()
                editor.putBoolean("confRemove", true)
                editor.commit()
            } else if (this.title == mContext.resources.getString(R.string.deck_conf_set_subdecks)) {
                // Deck Options :: Set Options Group for all Sub-decks
                val editor = AnkiDroidApp.getSharedPrefs(mContext).edit()
                editor.putBoolean("confSetSubdecks", true)
                editor.commit()
            } else {
                // Main Preferences :: Reset Languages
                if (MetaDB.resetLanguages(mContext)) {
                    val successReport = Toast.makeText(this.context,
                            AnkiDroidApp.getAppResources().getString(R.string.reset_confirmation), Toast.LENGTH_SHORT)
                    successReport.show()
                }
            }
        }
    }

}
