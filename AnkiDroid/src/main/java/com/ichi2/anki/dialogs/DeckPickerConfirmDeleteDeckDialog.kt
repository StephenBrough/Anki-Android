package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class DeckPickerConfirmDeleteDeckDialog : DialogFragment() {

    var dialogMessage: String by StringArg()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(R.string.delete_deck_title)
                .content(dialogMessage)
                .iconAttr(R.attr.dialogErrorIcon)
                .positiveText(R.string.dialog_positive_delete)
                .negativeText(android.R.string.cancel)
                .cancelable(true)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        (activity as DeckPicker).deleteContextMenuDeck()
                        (activity as DeckPicker).dismissAllDialogFragments()
                    }

                    override fun onNegative(dialog: MaterialDialog?) {
                        (activity as DeckPicker).dismissAllDialogFragments()
                    }
                })
                .build()
    }

    companion object {
        fun newInstance(dialogMessage: String): DeckPickerConfirmDeleteDeckDialog {
            val f = DeckPickerConfirmDeleteDeckDialog()
            val args = Bundle()
            args.putString("dialogMessage", dialogMessage)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogMessage: String) {
            fm.dismissExisting<DeckPickerConfirmDeleteDeckDialog>()
            DeckPickerConfirmDeleteDeckDialog().apply {
                this.dialogMessage = dialogMessage
            }.show(fm, DeckPickerConfirmDeleteDeckDialog::class.java.simpleName)
        }
    }
}
