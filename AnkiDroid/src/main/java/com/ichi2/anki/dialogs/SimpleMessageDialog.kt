package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.FragmentManager

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.utils.BooleanArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class SimpleMessageDialog : AsyncDialogFragment() {

    var title: String by StringArg()
    var message: String by StringArg()
    var reload: Boolean by BooleanArg()

    override val notificationTitle: String
        get() {
            return if (title.isNotBlank()) {
                title
            } else {
                AnkiDroidApp.getAppResources().getString(R.string.app_name)
            }
        }

    override val notificationMessage: String
        get() = message

    interface SimpleMessageDialogListener {
        fun dismissSimpleMessageDialog(reload: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(notificationTitle)
                .content(notificationMessage)
                .positiveText(android.R.string.ok)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        (activity as SimpleMessageDialogListener)
                                .dismissSimpleMessageDialog(reload)
                    }
                })
                .show()
    }

    companion object {

        fun newInstance(message: String, reload: Boolean): SimpleMessageDialog =
                newInstance("", message, reload)


        fun newInstance(title: String, message: String, reload: Boolean): SimpleMessageDialog {
            val f = SimpleMessageDialog()
            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)
            args.putBoolean("reload", reload)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, message: String, reload: Boolean, title: String = "") {
            fm.dismissExisting<SimpleMessageDialog>()
            SimpleMessageDialog().apply {
                this.message = message
                this.reload = reload
                this.title = title
            }.show(fm, SimpleMessageDialog::class.java.simpleName)
        }
    }
}
