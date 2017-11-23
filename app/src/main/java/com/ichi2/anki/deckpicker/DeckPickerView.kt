package com.ichi2.anki.deckpicker

import android.net.Uri
import android.support.v4.app.DialogFragment

/**
 * Interface between the DeckPickerActivity activity
 * and the corresponding ViewModel
 */
interface DeckPickerView {

    fun updateDeckList()
    fun dismissAllDialogFragments()
    fun collapseActionsMenu()
    fun notifyDataSetChanged()
    fun handleDeckSelection(deckId: Long, dontSkipStudyOptions: Boolean)
    fun showDialogFragment(deckId: Long)
    fun dismissProgressDialog()
    fun showSimpleMessageDialogLocal(msg: String)
    fun showSimpleMessageDialogLocal(msg: Int)
    fun requestStoragePermission()
    fun showProgress(msg: String)
    fun showProgress(msg: Int)
    fun dismissStyledProgressDialog()
    fun showExportCompleteDialog(exportPath: String)
    fun showThemedToast(msgResId: Int)
    fun setProgressDialogMessage(message: String?)
    fun dismissProgressSnackbar()
    fun resolveIntent(uri: Uri, attachmentName: String)
    fun loadStudyOptionsFragment(withDeckOptions: Boolean)
    fun showSimpleSnackbar(msg: String)
    fun showDialogFragment(newDialog: DialogFragment)
}