package com.ichi2.anki.dialogs

import android.os.Bundle
import android.os.Message
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.R
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting
import java.io.File

class DeckPickerExportCompleteDialog : AsyncDialogFragment() {

    var exportPath: String by StringArg()

    override val notificationTitle: String
        get() = resources.getString(R.string.export_successful_title)

    override// Show a relative path if the collection is stored in the default location
            // Show the absolute path if the user has messed with the AnkiDroid directory
    val notificationMessage: String
        get() {
            return if (CollectionHelper.getCurrentAnkiDroidDirectory(context) == CollectionHelper.getDefaultAnkiDroidDirectory()) {
                val exportFile = File("AnkiDroid/export/", File(exportPath).name)
                resources.getString(R.string.export_successful, exportFile.path)
            } else {
                resources.getString(R.string.export_successful, exportPath)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(R.string.export_successful_title)
                .content(notificationMessage)
                .iconAttr(R.attr.dialogSendIcon)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        (activity as DeckPickerActivity).dismissAllDialogFragments()
//                        (activity as DeckPickerActivity).emailFile(exportPath)
                        (activity as DeckPickerActivity).viewModel.emailFile(exportPath)

                    }

                    override fun onNegative(dialog: MaterialDialog?) {
                        (activity as DeckPickerActivity).dismissAllDialogFragments()
                    }
                })
                .show()
    }


    override fun getDialogHandlerMessage(): Message? {
        val msg = Message.obtain()
        msg.what = DialogHandler.MSG_SHOW_EXPORT_COMPLETE_DIALOG
        val b = Bundle()
        b.putString("exportPath", exportPath)
        msg.data = b
        return msg
    }

    companion object {

        fun newInstance(exportPath: String): DeckPickerExportCompleteDialog {
            val f = DeckPickerExportCompleteDialog()
            val args = Bundle()
            args.putString("exportPath", exportPath)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, exportPath: String) {
            fm.dismissExisting<DeckPickerExportCompleteDialog>()
            DeckPickerExportCompleteDialog().apply {
                this.exportPath = exportPath
            }.show(fm, DeckPickerExportCompleteDialog::class.java.simpleName)
        }
    }
}
