package com.ichi2.anki.dialogs

import android.os.Bundle
import android.os.Message
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.DialogHandler
import com.ichi2.anki.R
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArrayListArg
import com.ichi2.utils.dismissExisting
import java.util.*

class MediaCheckDialog : AsyncDialogFragment() {

    var dialogType: Int by IntArg()
    var nohave: ArrayList<String> by StringArrayListArg()
    var unused: ArrayList<String> by StringArrayListArg()
    var invalid: ArrayList<String> by StringArrayListArg()


    override val notificationMessage: String
        get() = when (dialogType) {
            DIALOG_CONFIRM_MEDIA_CHECK -> resources.getString(R.string.check_media_warning)
            else -> resources.getString(R.string.app_name)
        }


    override val notificationTitle: String
        get() = when (dialogType) {
            DIALOG_CONFIRM_MEDIA_CHECK -> resources.getString(R.string.check_media_title)
            DIALOG_MEDIA_CHECK_RESULTS -> resources.getString(R.string.check_media_acknowledge)
            else -> resources.getString(R.string.app_name)
        }

    interface MediaCheckDialogListener {
        fun showMediaCheckDialog(dialogType: Int)
        fun showMediaCheckDialog(dialogType: Int, checkList: List<List<String>>)
        fun mediaCheck()
        fun deleteUnused(unused: List<String>?)
        fun dismissAllDialogFragments()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val builder = MaterialDialog.Builder(activity!!)
        builder.title(notificationTitle)

        when (dialogType) {
            DIALOG_CONFIRM_MEDIA_CHECK -> return builder.content(notificationMessage)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .cancelable(true)
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog?) {
                            (activity as MediaCheckDialogListener).mediaCheck()
                            (activity as MediaCheckDialogListener)
                                    .dismissAllDialogFragments()
                        }

                        override fun onNegative(dialog: MaterialDialog?) {
                            (activity as MediaCheckDialogListener)
                                    .dismissAllDialogFragments()
                        }
                    })
                    .show()
            DIALOG_MEDIA_CHECK_RESULTS -> {
                // Generate report
                var report = ""
                if (invalid.isNotEmpty()) {
                    report += String.format(resources.getString(R.string.check_media_invalid), invalid.size)
                }
                if (unused.isNotEmpty()) {
                    if (report.isNotEmpty()) {
                        report += "\n"
                    }
                    report += String.format(resources.getString(R.string.check_media_unused), unused.size)
                }
                if (nohave.isNotEmpty()) {
                    if (report.isNotEmpty()) {
                        report += "\n"
                    }
                    report += String.format(resources.getString(R.string.check_media_nohave), nohave.size)
                }

                if (report.isEmpty()) {
                    report = resources.getString(R.string.check_media_no_unused_missing)
                }

                // We also prefix the report with a message about the media db being rebuilt, since
                // we do a full media scan and update the db on each media check on AnkiDroid.
                report = resources.getString(R.string.check_media_db_updated) + "\n\n" + report
                builder.content(report)
                        .cancelable(true)

                // If we have unused files, show a dialog with a "delete" button. Otherwise, the user only
                // needs to acknowledge the results, so show only an OK dialog.
                if (unused.size > 0) {
                    builder.positiveText(resources.getString(R.string.dialog_ok))
                            .negativeText(resources.getString(R.string.check_media_delete_unused))
                            .callback(object : MaterialDialog.ButtonCallback() {
                                override fun onPositive(dialog: MaterialDialog?) {
                                    (activity as MediaCheckDialogListener)
                                            .dismissAllDialogFragments()
                                }

                                override fun onNegative(dialog: MaterialDialog?) {
                                    (activity as MediaCheckDialogListener).deleteUnused(unused)
                                    dismissAllDialogFragments()
                                }
                            })
                } else {
                    builder.positiveText(resources.getString(R.string.dialog_ok))
                            .callback(object : MaterialDialog.ButtonCallback() {
                                override fun onPositive(dialog: MaterialDialog?) {
                                    (activity as MediaCheckDialogListener).dismissAllDialogFragments()
                                }
                            })
                }
                return builder.show()
            }
            else -> return builder.show()
        }
    }

    fun dismissAllDialogFragments() = (activity as MediaCheckDialogListener).dismissAllDialogFragments()

    override fun getDialogHandlerMessage(): Message? {
        val msg = Message.obtain()
        msg.what = DialogHandler.MSG_SHOW_MEDIA_CHECK_COMPLETE_DIALOG
        val b = Bundle()
        b.putStringArrayList("nohave", nohave)
        b.putStringArrayList("unused", unused)
        b.putStringArrayList("invalid", invalid)
        b.putInt("dialogType", dialogType)
        msg.data = b
        return msg
    }

    companion object {
        const val DIALOG_CONFIRM_MEDIA_CHECK = 0
        const val DIALOG_MEDIA_CHECK_RESULTS = 1


        fun newInstance(dialogType: Int): MediaCheckDialog {
            val f = MediaCheckDialog()
            val args = Bundle()
            args.putInt("dialogType", dialogType)
            f.arguments = args
            return f
        }


        fun newInstance(dialogType: Int, checkList: List<List<String>>): MediaCheckDialog {
            val f = MediaCheckDialog()
            val args = Bundle()
            args.putStringArrayList("nohave", ArrayList(checkList[0]))
            args.putStringArrayList("unused", ArrayList(checkList[1]))
            args.putStringArrayList("invalid", ArrayList(checkList[2]))
            args.putInt("dialogType", dialogType)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogType: Int, checkList: List<List<String>>) {
            fm.dismissExisting<MediaCheckDialog>()
            MediaCheckDialog().apply {
                this.dialogType = dialogType
                this.nohave = ArrayList(checkList[0])
                this.unused = ArrayList(checkList[1])
                this.invalid = ArrayList(checkList[2])
            }
        }
    }
}
