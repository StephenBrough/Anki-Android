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

package com.ichi2.anki.multimediacard.activity

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import com.ichi2.utils.StringArg
import com.ichi2.utils.StringArrayListArg
import com.ichi2.utils.dismissExisting

import java.util.ArrayList

/**
 * This dialog fragment support a choice from a list of strings.
 */
class PickStringDialogFragment : DialogFragment() {
    var possibleChoices: ArrayList<String> by StringArrayListArg()

    var clickListener: DialogInterface.OnClickListener? = null

    var title: String by StringArg()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)

        val adapter = ArrayAdapter(this.activity,
                android.R.layout.simple_list_item_1, possibleChoices)

        builder.setAdapter(adapter, clickListener)

        return builder.create()
    }

    companion object {
        fun show(fm: FragmentManager, title: String, choices: ArrayList<String>, clickListener: DialogInterface.OnClickListener) {
            fm.dismissExisting<PickStringDialogFragment>()
            PickStringDialogFragment().apply {
                this.title = title
                this.clickListener = clickListener
                this.possibleChoices = choices
            }.show(fm, PickStringDialogFragment::class.java.simpleName)
        }
    }
}
