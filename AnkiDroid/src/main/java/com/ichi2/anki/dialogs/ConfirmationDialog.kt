package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

/**
 * This is a reusable convenience class which makes it easy to show a confirmation dialog as a DialogFragment.
 * Create a new instance, call setArgs(...), setConfirm(), and setCancel() then show it via the fragment manager as usual.
 */
class ConfirmationDialog : DialogFragment() {

    var title: String by StringArg(default = "")
    var message: String by StringArg()

    private var confirm: Runnable = Runnable {
        // Do nothing by default
    }
    private var cancel: Runnable = Runnable {
        // Do nothing by default
    }

    // TODO: Remove setters
    fun setArgs(message: String) {
        setArgs("", message)
    }

    fun setArgs(title: String, message: String) {
        val args = Bundle()
        args.putString("message", message)
        args.putString("title", title)
        arguments = args
    }

    fun setConfirm(confirm: Runnable) {
        this.confirm = confirm
    }

    fun setCancel(cancel: Runnable) {
        this.cancel = cancel
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(if (title.isBlank()) resources.getString(R.string.app_name) else title)
                .content(message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) = confirm.run()
                    override fun onNegative(dialog: MaterialDialog?) = cancel.run()
                })
                .show()
    }

    companion object {
        fun show(fm: FragmentManager, title: String, message: String, confirm: Runnable, cancel: Runnable) {
            // TODO: Check if this will cause problems if multiple confirmation dialogs are shown
            fm.dismissExisting<ConfirmationDialog>()
            ConfirmationDialog().apply {
                this.title = title
                this.message = message
                this.confirm = confirm
                this.cancel = cancel
            }.show(fm, ConfirmationDialog::class.java.simpleName)
        }
    }
}
