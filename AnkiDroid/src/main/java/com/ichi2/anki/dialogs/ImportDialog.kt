package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.CollectionHelper
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.libanki.Utils
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting
import java.io.File

class ImportDialog : DialogFragment() {
    var dialogType: Int by IntArg(default = 0)
    var dialogMessage: String by StringArg()

    interface ImportDialogListener {
        fun showImportDialog(id: Int, message: String)

        fun showImportDialog(id: Int)

        fun importAdd(importPath: String?)

        fun importReplace(importPath: String?)

        fun dismissAllDialogFragments()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val builder = MaterialDialog.Builder(activity)
        builder.cancelable(true)

        when (dialogType) {
            DIALOG_IMPORT_HINT ->
                // Instruct the user that they need to put their APKG files into the AnkiDroid directory
                return builder.title(R.string.import_title)
                        .content(resources.getString(R.string.import_hint, CollectionHelper.getCurrentAnkiDroidDirectory(activity)))
                        .positiveText(android.R.string.ok)
                        .negativeText(resources.getString(R.string.dialog_cancel))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as ImportDialogListener).showImportDialog(DIALOG_IMPORT_SELECT)
                            }

                            override fun onNegative(dialog: MaterialDialog?) {
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_IMPORT_SELECT -> {
                // Allow user to choose from the list of available APKG files
                val fileList = Utils.getImportableDecks(activity)
                if (fileList.isEmpty()) {
                    UIUtils.showThemedToast(activity,
                            resources.getString(R.string.upgrade_import_no_file_found, "'.apkg'"), false)
                    return builder.showListener { dialog -> dialog.cancel() }.show()
                } else {
                    val tts = fileList.map { it.name.replace(".apkg", "") }
                    val importValues = fileList.map(File::getAbsolutePath)

                    return builder.title(R.string.import_select_title)
                            .items(*tts.toTypedArray())
                            .itemsCallback { _, _, i, _ ->
                                val importPath = importValues[i]
                                // If the apkg file is called "collection.apkg", we assume the collection will be replaced
                                if (filenameFromPath(importPath) == "collection.apkg") {
                                    (activity as ImportDialogListener).showImportDialog(DIALOG_IMPORT_REPLACE_CONFIRM, importPath)
                                    // Otherwise we add the file since exported decks / shared decks can't be imported via replace anyway
                                } else {
                                    (activity as ImportDialogListener).showImportDialog(DIALOG_IMPORT_ADD_CONFIRM, importPath)
                                }
                            }
                            .show()
                }
            }

            DIALOG_IMPORT_ADD_CONFIRM -> return builder.title(resources.getString(R.string.import_title))
                    .content(resources.getString(R.string.import_message_add_confirm, filenameFromPath(dialogMessage)))
                    .positiveText(resources.getString(R.string.import_message_add))
                    .negativeText(android.R.string.cancel)
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog?) {
                            (activity as ImportDialogListener).importAdd(dialogMessage)
                            dismissAllDialogFragments()
                        }
                    })
                    .show()

            DIALOG_IMPORT_REPLACE_CONFIRM -> return builder.title(resources.getString(R.string.import_title))
                    .content(resources.getString(R.string.import_message_replace_confirm, dialogMessage))
                    .positiveText(resources.getString(R.string.dialog_positive_replace))
                    .negativeText(android.R.string.cancel)
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog?) {
                            (activity as ImportDialogListener).importReplace(dialogMessage)
                            dismissAllDialogFragments()
                        }
                    })
                    .show()

            else -> return builder.show()
        }
    }

    fun dismissAllDialogFragments() {
        (activity as ImportDialogListener).dismissAllDialogFragments()
    }

    companion object {

        const val DIALOG_IMPORT_HINT = 0
        const val DIALOG_IMPORT_SELECT = 1
        const val DIALOG_IMPORT_ADD_CONFIRM = 2
        const val DIALOG_IMPORT_REPLACE_CONFIRM = 3


        /**
         * A set of dialogs which deal with importing a file
         *
         * @param dialogType An integer which specifies which of the sub-dialogs to show
         * @param dialogMessage An optional string which can be used to show a custom message
         * or specify import path
         */
        fun newInstance(dialogType: Int, dialogMessage: String): ImportDialog {
            val f = ImportDialog()
            val args = Bundle()
            args.putInt("dialogType", dialogType)
            args.putString("dialogMessage", dialogMessage)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogMessage: String, dialogType: Int) {
            fm.dismissExisting<ImportDialog>()
            ImportDialog().apply {
                this.dialogMessage = dialogMessage
                this.dialogType = dialogType
            }.show(fm, ImportDialog::class.java.simpleName)
        }

        private fun filenameFromPath(path: String?): String =
                path!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1]
    }
}
