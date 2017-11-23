package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.R
import com.ichi2.utils.dismissExisting

class DeckPickerNoSpaceLeftDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(R.string.sd_card_full_title)
                .content(R.string.backup_deck_no_space_left)
                .cancelable(true)
                .positiveText(R.string.ok)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        (activity as DeckPickerActivity).startLoadingCollection()
                    }
                })
                .cancelListener { (activity as DeckPickerActivity).startLoadingCollection() }
                .show()
    }

    companion object {
        fun newInstance(): DeckPickerNoSpaceLeftDialog = DeckPickerNoSpaceLeftDialog()

        fun show(fm: FragmentManager) {
            fm.dismissExisting<DeckPickerNoSpaceLeftDialog>()
            DeckPickerNoSpaceLeftDialog().show(fm, DeckPickerNoSpaceLeftDialog::class.java.simpleName)
        }
    }
}