package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class ModelBrowserContextMenu : DialogFragment() {

    var label: String by StringArg()
    lateinit var contextMenuListener: MaterialDialog.ListCallback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)

        val entries = arrayOfNulls<String>(3)
        entries[MODEL_TEMPLATE] = resources.getString(R.string.model_browser_template)
        entries[MODEL_RENAME] = resources.getString(R.string.model_browser_rename)
        entries[MODEL_DELETE] = resources.getString(R.string.model_browser_delete)

        return MaterialDialog.Builder(activity)
                .title(label)
                .items(*entries)
                .itemsCallback(contextMenuListener)
                .build()
    }

    companion object {

        const val MODEL_TEMPLATE = 0
        const val MODEL_RENAME = 1
        const val MODEL_DELETE = 2

        fun newInstance(label: String, contextMenuListener: MaterialDialog.ListCallback): ModelBrowserContextMenu {

            val n = ModelBrowserContextMenu()
            val b = Bundle()
            b.putString("label", label)
            n.arguments = b
            n.contextMenuListener = contextMenuListener
            return n
        }

        fun show(fm: FragmentManager, label: String, contextMenuListener: MaterialDialog.ListCallback) {
            fm.dismissExisting<ModelBrowserContextMenu>()
            ModelBrowserContextMenu().apply {
                this.label = label
                this.contextMenuListener = contextMenuListener
            }.show(fm, ModelBrowserContextMenu::class.java.simpleName)
        }
    }
}
