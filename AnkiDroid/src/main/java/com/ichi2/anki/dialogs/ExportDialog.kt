package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.LongArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class ExportDialog : DialogFragment() {

    var deckId: Long by LongArg(default = -1L)
    var dialogMessage: String by StringArg()

    private val INCLUDE_SCHED = 0
    private val INCLUDE_MEDIA = 1
    private var mIncludeSched = false
    private var mIncludeMedia = false

    interface ExportDialogListener {
        fun exportApkg(path: String?, did: Long?, includeSched: Boolean, includeMedia: Boolean)
        fun dismissAllDialogFragments()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val checked: Array<Int>
        if (deckId != -1L) {
            mIncludeSched = false
            checked = arrayOf()
        } else {
            mIncludeSched = true
            checked = arrayOf(INCLUDE_SCHED)
        }
        val items = arrayOf(resources.getString(R.string.export_include_schedule), resources.getString(R.string.export_include_media))

        val builder = MaterialDialog.Builder(activity)
                .title(R.string.export)
                .content(dialogMessage)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .cancelable(true)
                .items(*items)
                .alwaysCallMultiChoiceCallback()
                .itemsCallbackMultiChoice(checked
                ) { _, integers, _ ->
                    mIncludeMedia = false
                    mIncludeSched = false
                    for (integer in integers) {
                        when (integer) {
                            INCLUDE_SCHED -> mIncludeSched = true
                            INCLUDE_MEDIA -> mIncludeMedia = true
                        }
                    }
                    true
                }
                .onPositive { _, _ ->
                    (activity as ExportDialogListener)
                            .exportApkg(null, if (deckId != -1L) deckId else null, mIncludeSched, mIncludeMedia)
                    dismissAllDialogFragments()
                }
                .onNegative { _, _ -> dismissAllDialogFragments() }
        return builder.show()
    }


    fun dismissAllDialogFragments() = (activity as ExportDialogListener).dismissAllDialogFragments()

    companion object {
        /**
         * A set of dialogs which deal with importing a file
         *
         * @param did An integer which specifies which of the sub-dialogs to show
         * @param dialogMessage An optional string which can be used to show a custom message or specify import path
         */
        fun newInstance(dialogMessage: String, did: Long?): ExportDialog {
            val f = ExportDialog()
            val args = Bundle()
            args.putLong("did", did!!)
            args.putString("dialogMessage", dialogMessage)
            f.arguments = args
            return f
        }

        fun newInstance(dialogMessage: String): ExportDialog {
            val f = ExportDialog()
            val args = Bundle()
            args.putString("dialogMessage", dialogMessage)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogMessage: String, deckId: Long = -1L) {
            fm.dismissExisting<ExportDialog>()
            ExportDialog().apply {
                this.dialogMessage = dialogMessage
                this.deckId = deckId
            }.show(fm, ExportDialog::class.java.simpleName)
        }
    }

}
