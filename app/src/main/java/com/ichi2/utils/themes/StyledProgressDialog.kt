/****************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
 * *
 * based on custom Dialog windows by antoine vianey                                     *
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

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatDialogFragment
import com.ichi2.utils.BooleanArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class StyledProgressDialog : AppCompatDialogFragment() {

    var title: String by StringArg()
    var message: String by StringArg()
    var canCancel: Boolean by BooleanArg()

    var theDialog: StyledProgressDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(canCancel)
                .show()
    }

//    fun setMax(max: Int) {
//        // TODO
//    }
//
//
//    fun setProgress(progress: Int) {
//        // TODO
//    }
//
//
//    fun setProgressStyle(style: Int) {
//        // TODO
//    }

    companion object {
        @JvmOverloads
        fun show(context: Context, title: String, message: String,
                 cancelable: Boolean = false, cancelListener: DialogInterface.OnCancelListener? = null): ProgressDialog =
                ProgressDialog.show(context, title,message, true, cancelable, cancelListener)

        fun show(fm: FragmentManager, title: String, message: String, cancelable: Boolean = false) {
            fm.dismissExisting<StyledProgressDialog>()
            StyledProgressDialog().apply {
                this.title = title
                this.message = message
                this.canCancel = cancelable
                theDialog = this
            }.show(fm, StyledProgressDialog::class.java.simpleName)
        }
    }
}