package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class ModelEditorContextMenu : DialogFragment() {

    var label: String by StringArg()
    lateinit var contextMenuListener: MaterialDialog.ListCallback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)

        val entries = arrayOfNulls<String>(4)
        entries[FIELD_REPOSITION] = resources.getString(R.string.model_field_editor_reposition_menu)
        entries[SORT_FIELD] = resources.getString(R.string.model_field_editor_sort_field)
        entries[FIELD_RENAME] = resources.getString(R.string.model_field_editor_rename)
        entries[FIELD_DELETE] = resources.getString(R.string.model_field_editor_delete)

        return MaterialDialog.Builder(activity)
                .title(label)
                .items(*entries)
                .itemsCallback(contextMenuListener)
                .build()
    }

    companion object {

        const val FIELD_REPOSITION = 0
        const val SORT_FIELD = 1
        const val FIELD_RENAME = 2
        const val FIELD_DELETE = 3

        fun newInstance(label: String, contextMenuListener: MaterialDialog.ListCallback): ModelEditorContextMenu {
            val n = ModelEditorContextMenu()
            val b = Bundle()
            b.putString("label", label)
            n.contextMenuListener = contextMenuListener
            n.arguments = b
            return n
        }

        fun show(fm: FragmentManager, label: String, contextMenuListener: MaterialDialog.ListCallback) {
            fm.dismissExisting<ModelEditorContextMenu>()
            ModelEditorContextMenu().apply {
                this.label = label
                this.contextMenuListener = contextMenuListener
            }.show(fm, ModelEditorContextMenu::class.java.simpleName)
        }
    }
}
