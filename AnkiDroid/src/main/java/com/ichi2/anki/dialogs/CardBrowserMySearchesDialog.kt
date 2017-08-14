package com.ichi2.anki.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.utils.IntArg
import com.ichi2.utils.SerializableArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting
import java.util.*

class CardBrowserMySearchesDialog : DialogFragment() {

    private var mSearchesAdapter: MySearchesArrayAdapter? = null

    var type: Int by IntArg()
    var savedFilters: HashMap<String, String> by SerializableArg(default = HashMap())
    var currentSearchTerms: String by StringArg()

    var mySearchesDialogListener: MySearchesDialogListener? = null

    interface MySearchesDialogListener {
        fun OnSelection(searchName: String?)
        fun OnRemoveSearch(searchName: String?)
        fun OnSaveSearch(searchName: String, searchTerms: String?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)

        val builder = MaterialDialog.Builder(activity)

        //adjust padding to use dp as seen here: http://stackoverflow.com/a/9685690/1332026
        when (type) {
            CARD_BROWSER_MY_SEARCHES_TYPE_LIST -> {
                mSearchesAdapter = MySearchesArrayAdapter(activity, ArrayList(savedFilters.keys))
                mSearchesAdapter!!.notifyDataSetChanged() //so the values are sorted.
                builder.title(resources.getString(R.string.card_browser_list_my_searches_title))
                        .adapter(mSearchesAdapter!!) { dialog, _, which, _ ->
                            mySearchesDialogListener!!.OnSelection(mSearchesAdapter!!.getItem(which))
                            dialog.dismiss()
                        }
            }
            CARD_BROWSER_MY_SEARCHES_TYPE_SAVE -> {
                builder.title(getString(R.string.card_browser_list_my_searches_save))
                        .positiveText(getString(android.R.string.ok))
                        .negativeText(getString(android.R.string.cancel))
                        .input(R.string.card_browser_list_my_searches_new_name, R.string.empty_string) {
                            _, text -> mySearchesDialogListener!!.OnSaveSearch(text.toString(), currentSearchTerms)
                        }
            }
        }
        val dialog = builder.build()
        dialog.listView?.let {
            it.divider = ColorDrawable(ContextCompat.getColor(activity, R.color.material_grey_600))
            it.dividerHeight = 1
            //adjust padding to use dp as seen here: http://stackoverflow.com/a/9685690/1332026
            val scale = resources.displayMetrics.density
            val dpAsPixels = (5 * scale + 0.5f).toInt()
            dialog.view.setPadding(dpAsPixels, 0, dpAsPixels, dpAsPixels)
        }

        return dialog
    }

    private fun removeSearch(searchName: String?) {
        if (mSearchesAdapter!!.getPosition(searchName) >= 0) {
            MaterialDialog.Builder(activity)
                    .content(resources.getString(R.string.card_browser_list_my_searches_remove_content, searchName))
                    .positiveText(resources.getString(android.R.string.ok))
                    .negativeText(resources.getString(R.string.cancel))
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog?) {
                            mySearchesDialogListener!!.OnRemoveSearch(searchName)
                            savedFilters.remove(searchName)
                            mSearchesAdapter!!.remove(searchName)
                            mSearchesAdapter!!.notifyDataSetChanged()
                            dialog!!.dismiss()
                            if (savedFilters.size == 0) {
                                getDialog().dismiss()
                            }
                        }
                    }).show()
        }
    }

    //using View Holder pattern for faster ListView scrolling.
    internal class ViewHolder {
        lateinit var mSearchName: TextView
        lateinit var mSearchTerms: TextView
        lateinit var mRemoveButton: ImageButton
    }

    inner class MySearchesArrayAdapter(private val mContext: Context, private val mSavedFiltersNames: ArrayList<String>) : ArrayAdapter<String>(mContext, R.layout.card_browser_item_my_searches_dialog, mSavedFiltersNames) {

        override fun getView(position: Int, conView: View?, parent: ViewGroup): View {
            var convertView = conView
            val viewHolder: ViewHolder?
            if (convertView == null) {
                viewHolder = ViewHolder()
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = inflater.inflate(R.layout.card_browser_item_my_searches_dialog, parent, false)
                viewHolder.mSearchName = convertView.findViewById(R.id.card_browser_my_search_name_textview)
                viewHolder.mSearchTerms = convertView.findViewById(R.id.card_browser_my_search_terms_textview)
                viewHolder.mRemoveButton = convertView.findViewById(R.id.card_browser_my_search_remove_button)

                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }

            viewHolder.mSearchName.text = getItem(position)
            viewHolder.mSearchTerms.text = savedFilters[getItem(position)]
            viewHolder.mRemoveButton.setOnClickListener { this@CardBrowserMySearchesDialog.removeSearch(getItem(position)) }
            return convertView!!
        }

        override fun notifyDataSetChanged() {
            Collections.sort(mSavedFiltersNames) { lhs, rhs -> lhs.compareTo(rhs, ignoreCase = true) }
            super.notifyDataSetChanged()
        }

        override fun areAllItemsEnabled(): Boolean = true
    }

    companion object {

        const val CARD_BROWSER_MY_SEARCHES_TYPE_LIST = 0 //list searches dialog
        const val CARD_BROWSER_MY_SEARCHES_TYPE_SAVE = 1 //save searches dialog

        fun newInstance(savedFilters: HashMap<String, String>,
                        mySearchesDialogListener: MySearchesDialogListener,
                        currentSearchTerms: String, type: Int): CardBrowserMySearchesDialog {
            val m = CardBrowserMySearchesDialog()
            val args = Bundle()
            args.putSerializable("savedFilters", savedFilters)
            args.putInt("type", type)
            args.putString("currentSearchTerms", currentSearchTerms)
            m.arguments = args
            m.mySearchesDialogListener = mySearchesDialogListener
            return m
        }

        fun show(fm: FragmentManager, savedFilters: HashMap<String, String>, currentSearchTerms: String, type: Int, mySearchesDialogListener: MySearchesDialogListener) {
            fm.dismissExisting<CardBrowserMySearchesDialog>()
            CardBrowserMySearchesDialog().apply {
                this.savedFilters= savedFilters
                this.currentSearchTerms = currentSearchTerms
                this.type = type
                this.mySearchesDialogListener = mySearchesDialogListener
            }.show(fm, CardBrowserMySearchesDialog::class.java.simpleName)
        }
    }
}
