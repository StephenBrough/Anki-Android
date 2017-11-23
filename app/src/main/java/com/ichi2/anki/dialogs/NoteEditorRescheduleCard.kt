package com.ichi2.anki.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.NoteEditor
import com.ichi2.anki.R
import com.ichi2.utils.dismissExisting

class NoteEditorRescheduleCard : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        return MaterialDialog.Builder(activity)
                .title(R.string.reschedule_card_dialog_title)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .inputRange(1, 4) // max 4 characters (i.e., 9999)
                .input(R.string.reschedule_card_dialog_message, R.string.empty_string) { _, text ->
                    val days = Integer.parseInt(text.toString())
                    (activity as NoteEditor).onRescheduleCard(days)
                }
                .show()
    }

    companion object {
        fun newInstance(): NoteEditorRescheduleCard = NoteEditorRescheduleCard()

        fun show(fm: FragmentManager) {
            fm.dismissExisting<NoteEditorRescheduleCard>()
            NoteEditorRescheduleCard().show(fm, NoteEditorRescheduleCard::class.java.simpleName)
        }
    }
}
