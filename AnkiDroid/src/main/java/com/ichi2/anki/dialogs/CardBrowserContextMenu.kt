package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.BooleanArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class CardBrowserContextMenu : DialogFragment() {

    var title: String by StringArg()
    var isMarked: Boolean by BooleanArg()
    var isSuspended: Boolean by BooleanArg()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        val res = resources
        val entries = arrayOfNulls<String>(4)
        entries[CONTEXT_MENU_DELETE] = res.getString(R.string.card_browser_delete_card)
        entries[CONTEXT_MENU_DETAILS] = res.getString(R.string.card_editor_preview_card)
        entries[CONTEXT_MENU_MARK] = res.getString(
                if (arguments.getBoolean("isMarked"))
                    R.string.card_browser_unmark_card
                else
                    R.string.card_browser_mark_card)
        entries[CONTEXT_MENU_SUSPEND] = res.getString(
                if (arguments.getBoolean("isSuspended"))
                    R.string.card_browser_unsuspend_card
                else
                    R.string.card_browser_suspend_card)
        // Ellipsize the title if it's obscenely long
        var title = arguments.getString("dialogTitle")
        if (title != null && title.length > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH) + "…"
        }
        return MaterialDialog.Builder(activity)
                .title(title!!)
                .items(*entries)
                .itemsCallback(mContextMenuListener!!)
                .build()
    }

    companion object {
        /**
         * Context Menu items
         */
        const val CONTEXT_MENU_MARK = 0
        const val CONTEXT_MENU_SUSPEND = 1
        const val CONTEXT_MENU_DELETE = 2
        const val CONTEXT_MENU_DETAILS = 3
        private val MAX_TITLE_LENGTH = 75

        private var mContextMenuListener: MaterialDialog.ListCallback? = null


        fun newInstance(dialogTitle: String, isMarked: Boolean,
                        isSuspended: Boolean, contextMenuListener: MaterialDialog.ListCallback): CardBrowserContextMenu {
            val f = CardBrowserContextMenu()
            val args = Bundle()
            args.putString("dialogTitle", dialogTitle)
            args.putBoolean("isMarked", isMarked)
            args.putBoolean("isSuspended", isSuspended)
            mContextMenuListener = contextMenuListener
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, title: String, isMarked: Boolean, isSuspended: Boolean, contextMenuListener: MaterialDialog.ListCallback) {
            fm.dismissExisting<CardBrowserContextMenu>()
            CardBrowserContextMenu().apply {
                this.title = title
                this.isMarked = isMarked
                this.isSuspended = isSuspended
                mContextMenuListener = contextMenuListener
            }.show(fm, CardBrowserContextMenu::class.java.simpleName)
        }
    }
}
