package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.cardbrowser.CardBrowser
import com.ichi2.anki.R
import com.ichi2.utils.BooleanArg
import com.ichi2.utils.IntArg
import com.ichi2.utils.dismissExisting

class CardBrowserOrderDialog : DialogFragment() {


    lateinit var orderDialogListener: MaterialDialog.ListCallbackSingleChoice

    var order: Int by IntArg()
    var isOrderAsc: Boolean by BooleanArg()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        val items = resources.getStringArray(R.array.card_browser_order_labels)
        // Set sort order arrow
        items.indices
                .filter { it != CardBrowser.CARD_ORDER_NONE && it == order }
                .forEach {
                    if (isOrderAsc) {
                        items[it] = items[it] + " (\u25b2)"
                    } else {
                        items[it] = items[it] + " (\u25bc)"

                    }
                }
        return MaterialDialog.Builder(activity!!)
                .title(resources.getString(R.string.card_browser_change_display_order_title))
                .content(resources.getString(R.string.card_browser_change_display_order_reverse))
                .items(*items)
                .itemsCallbackSingleChoice(order, orderDialogListener)
                .build()
    }

    companion object {

        fun newInstance(order: Int, isOrderAsc: Boolean,
                        orderDialogListener: MaterialDialog.ListCallbackSingleChoice): CardBrowserOrderDialog {
            val f = CardBrowserOrderDialog()
            val args = Bundle()
            args.putInt("order", order)
            args.putBoolean("isOrderAsc", isOrderAsc)
            f.orderDialogListener = orderDialogListener
            f.arguments = args
            return f
        }
    }

    fun show(fm: FragmentManager, order: Int, isOrderAsc: Boolean, orderDialogListener: MaterialDialog.ListCallbackSingleChoice) {
        fm.dismissExisting<CardBrowserOrderDialog>()
        CardBrowserOrderDialog().apply {
            this.order = order
            this.isOrderAsc = isOrderAsc
            this.orderDialogListener = orderDialogListener
        }.show(fm, CardBrowserOrderDialog::class.java.simpleName)
    }
}