package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CollectionHelper
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.utils.dismissExisting

class DeckPickerBackupNoSpaceLeftDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val space = BackupManager.getFreeDiscSpace(CollectionHelper.getCollectionPath(activity))
        return MaterialDialog.Builder(activity)
                .title(resources.getString(R.string.sd_card_almost_full_title))
                .content(resources.getString(R.string.sd_space_warning, space / 1024 / 1024))
                .positiveText(android.R.string.ok)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        (activity as DeckPicker).finishWithoutAnimation()
                    }
                })
                .cancelable(true)
                .cancelListener { (activity as DeckPicker).finishWithoutAnimation() }
                .show()
    }

    companion object {
        fun newInstance(): DeckPickerBackupNoSpaceLeftDialog = DeckPickerBackupNoSpaceLeftDialog()

        fun show(fm: FragmentManager) {
            fm.dismissExisting<DeckPickerBackupNoSpaceLeftDialog>()
            DeckPickerBackupNoSpaceLeftDialog().show(fm, DeckPickerBackupNoSpaceLeftDialog::class.java.simpleName)
        }
    }
}