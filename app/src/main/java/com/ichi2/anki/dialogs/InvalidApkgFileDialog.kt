package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class InvalidApkgFileDialog : DialogFragment() {

    var title: String by StringArg()
    var msg: String by StringArg()
    var callback: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok) { _, _ -> callback() }
                .create()
    }


    companion object {
        fun show(fm: FragmentManager, title: String, msg: String, callback: () -> Unit) {
            fm.dismissExisting<InvalidApkgFileDialog>()
            InvalidApkgFileDialog().apply {
                this.title = title
                this.msg = msg
                this.callback = callback
            }.show(fm, InvalidApkgFileDialog::class.java.simpleName)
        }
    }

}