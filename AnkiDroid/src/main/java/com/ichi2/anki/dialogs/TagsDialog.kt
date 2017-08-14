package com.ichi2.anki.dialogs

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.RadioGroup
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArrayListArg

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.TreeSet

class TagsDialog : DialogFragment() {

    private var mCurrentTags: TreeSet<String>? = null

    private var mPositiveText: String? = null
    private var mDialogTitle: String? = null
    private var mTagsDialogListener: TagsDialogListener? = null
    private var mTagsArrayAdapter: TagsArrayAdapter? = null
    private var mSelectedOption = -1

    private var mToolbar: Toolbar? = null
    private var mToolbarSearchView: SearchView? = null
    private var mToolbarSearchItem: MenuItem? = null
    private var mToolbarAddItem: MenuItem? = null

    private var mNoTagsTextView: TextView? = null
    private var mTagsListRecyclerView: RecyclerView? = null
    private var mOptionsGroup: RadioGroup? = null

    private var mDialog: MaterialDialog? = null

    var dialogType: Int by IntArg(default = TYPE_NONE)
    var currentTags: ArrayList<String> by StringArrayListArg()
    var allTags: ArrayList<String> by StringArrayListArg()

    interface TagsDialogListener {
        fun onPositive(selectedTags: List<String>, option: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mCurrentTags = TreeSet(String.CASE_INSENSITIVE_ORDER)
        mCurrentTags!!.addAll(arguments.getStringArrayList(CHECKED_TAGS_KEY))

        mCurrentTags!!
                .filterNot { allTags.contains(it) }
                .forEach { allTags.add(it) }

        isCancelable = true
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = resources

        val tagsDialogView = LayoutInflater.from(activity)
                .inflate(R.layout.tags_dialog, null, false)
        mTagsListRecyclerView = tagsDialogView.findViewById<View>(R.id.tags_dialog_tags_list) as RecyclerView
        mTagsListRecyclerView!!.requestFocus()
        mTagsListRecyclerView!!.setHasFixedSize(true)

        val tagsListLayout = LinearLayoutManager(activity)
        mTagsListRecyclerView!!.layoutManager = tagsListLayout

        mTagsArrayAdapter = TagsArrayAdapter()
        mTagsListRecyclerView!!.adapter = mTagsArrayAdapter

        mNoTagsTextView = tagsDialogView.findViewById<View>(R.id.tags_dialog_no_tags_textview) as TextView
        if (allTags!!.isEmpty()) {
            mNoTagsTextView!!.visibility = View.VISIBLE
        }
        mOptionsGroup = tagsDialogView.findViewById<View>(R.id.tags_dialog_options_radiogroup) as RadioGroup
        for (i in 0 until mOptionsGroup!!.childCount) {
            mOptionsGroup!!.getChildAt(i).id = i
        }
        mOptionsGroup!!.check(0)

        mSelectedOption = mOptionsGroup!!.checkedRadioButtonId
        mOptionsGroup!!.setOnCheckedChangeListener { radioGroup, checkedId -> mSelectedOption = checkedId }

        when (dialogType) {
            TYPE_ADD_TAG -> {
                mDialogTitle = resources.getString(R.string.card_details_tags)
                mOptionsGroup!!.visibility = View.GONE
                mPositiveText = getString(R.string.dialog_ok)
            }
            else -> {
                mDialogTitle = resources.getString(R.string.studyoptions_limit_select_tags)
                mPositiveText = getString(R.string.select)
            }
        }

        adjustToolbar(tagsDialogView)

        val builder = MaterialDialog.Builder(activity)
                .positiveText(mPositiveText!!)
                .negativeText(res.getString(R.string.dialog_cancel))
                .customView(tagsDialogView, false)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog?) {
                        mTagsDialogListener!!
                                .onPositive(ArrayList(mCurrentTags!!), mSelectedOption)
                    }
                })
        mDialog = builder.build()

        mDialog!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return mDialog!!
    }

    private fun adjustToolbar(tagsDialogView: View) {
        mToolbar = tagsDialogView.findViewById<View>(R.id.tags_dialog_toolbar) as Toolbar
        mToolbar!!.title = mDialogTitle

        mToolbar!!.inflateMenu(R.menu.tags_dialog_menu)

        val addTagFilter = InputFilter { source, start, end, _, _, _ ->
            (start until end)
                    .filter { source[it] == ' ' }
                    .forEach { return@InputFilter "" }
            null
        }
        mToolbarAddItem = mToolbar!!.menu.findItem(R.id.tags_dialog_action_add)
        mToolbarAddItem!!.setOnMenuItemClickListener {
            val query = mToolbarSearchView!!.query.toString()
            if (MenuItemCompat.isActionViewExpanded(mToolbarSearchItem!!) && !TextUtils.isEmpty(query)) {
                addTag(query)
                mToolbarSearchView!!.setQuery("", true)
            } else {
                val addTagBuilder = MaterialDialog.Builder(activity)
                        .title(R.string.add_tag)
                        .negativeText(android.R.string.cancel)
                        .positiveText(android.R.string.ok)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(R.string.tag_name, R.string.empty_string) { dialog, input -> addTag(input.toString()) }
                val addTagDialog = addTagBuilder.build()
                val inputET = addTagDialog.inputEditText
                inputET!!.filters = arrayOf(addTagFilter)
                addTagDialog.show()
            }
            true
        }

        mToolbarSearchItem = mToolbar!!.menu.findItem(R.id.tags_dialog_action_filter)
        mToolbarSearchView = MenuItemCompat.getActionView(mToolbarSearchItem!!) as SearchView

        val queryET = mToolbarSearchView!!.findViewById<View>(R.id.search_src_text) as EditText
        queryET.filters = arrayOf(addTagFilter)

        mToolbarSearchView!!.queryHint = resources.getString(R.string.filter_tags)
        mToolbarSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                mToolbarSearchView!!.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val adapter = mTagsListRecyclerView!!.adapter as TagsArrayAdapter
                adapter.filter.filter(newText)
                return true
            }
        })

        val checkAllItem = mToolbar!!.menu.findItem(R.id.tags_dialog_action_select_all)
        checkAllItem.setOnMenuItemClickListener {
            var changed = false
            if (mCurrentTags!!.containsAll(mTagsArrayAdapter!!.mTagsList)) {
                mCurrentTags!!.removeAll(mTagsArrayAdapter!!.mTagsList)
                changed = true
            } else {
                for (tag in mTagsArrayAdapter!!.mTagsList) {
                    if (!mCurrentTags!!.contains(tag)) {
                        mCurrentTags!!.add(tag)
                        changed = true
                    }
                }
            }

            if (changed) {
                mTagsArrayAdapter!!.notifyDataSetChanged()
            }
            true
        }

        when (dialogType) {
            TYPE_ADD_TAG -> mToolbarSearchView!!.queryHint = getString(R.string.add_new_filter_tags)
            else -> mToolbarAddItem!!.isVisible = false
        }
    }

    fun addTag(tag: String) {
        if (!TextUtils.isEmpty(tag)) {
            var feedbackText = ""
            if (!allTags.contains(tag)) {
                allTags.add(tag)
                if (mNoTagsTextView!!.visibility == View.VISIBLE) {
                    mNoTagsTextView!!.visibility = View.GONE
                }
                mTagsArrayAdapter!!.mTagsList.add(tag)
                mTagsArrayAdapter!!.sortData()
                feedbackText = getString(R.string.tag_editor_add_feedback, tag, mPositiveText)
            } else {
                feedbackText = getString(R.string.tag_editor_add_feedback_existing, tag)
            }
            if (!mCurrentTags!!.contains(tag)) {
                mCurrentTags!!.add(tag)
            }
            mTagsArrayAdapter!!.notifyDataSetChanged()
            // Show a snackbar to let the user know the tag was added successfully
            UIUtils.showSnackbar(activity, feedbackText, false, -1, null,
                    mDialog!!.view.findViewById(R.id.tags_dialog_snackbar), null)
        }
    }

    fun setTagsDialogListener(selectedTagsListener: TagsDialogListener) {
        mTagsDialogListener = selectedTagsListener
    }

    inner class TagsArrayAdapter : RecyclerView.Adapter<TagsArrayAdapter.ViewHolder>(), Filterable {

        var mTagsList: ArrayList<String> = ArrayList()

        inner class ViewHolder(val mTagItemCheckedTextView: CheckedTextView) : RecyclerView.ViewHolder(mTagItemCheckedTextView)

        init {
            mTagsList.addAll(allTags)
            sortData()
        }

        fun sortData() {
            Collections.sort(mTagsList) { lhs, rhs ->
                val lhs_checked = mCurrentTags!!.contains(lhs)
                val rhs_checked = mCurrentTags!!.contains(rhs)
                //priority for checked items.
                if (lhs_checked == rhs_checked) lhs.compareTo(rhs, ignoreCase = true) else if (lhs_checked) -1 else 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): TagsArrayAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.tags_item_list_dialog, parent, false)

            val vh = ViewHolder(v.findViewById<View>(R.id.tags_dialog_tag_item) as CheckedTextView)
            vh.mTagItemCheckedTextView.setOnClickListener { view ->
                val ctv = view as CheckedTextView
                ctv.toggle()
                val tag = ctv.text.toString()
                if (ctv.isChecked && !mCurrentTags!!.contains(tag)) {
                    mCurrentTags!!.add(tag)
                } else if (!ctv.isChecked && mCurrentTags!!.contains(tag)) {
                    mCurrentTags!!.remove(tag)
                }
            }
            return vh
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tag = mTagsList[position]
            holder.mTagItemCheckedTextView.text = tag
            holder.mTagItemCheckedTextView.isChecked = mCurrentTags!!.contains(tag)
        }

        override fun getItemCount(): Int {
            return mTagsList.size
        }

        override fun getFilter(): Filter = TagsFilter()

        /* Custom Filter class - as seen in http://stackoverflow.com/a/29792313/1332026 */
        private inner class TagsFilter : Filter() {
            private val mFilteredTags: ArrayList<String> = ArrayList()

            override fun performFiltering(constraint: CharSequence): Filter.FilterResults {
                mFilteredTags.clear()
                val filterResults = Filter.FilterResults()
                if (constraint.isEmpty()) {
                    mFilteredTags.addAll(allTags)
                } else {
                    val filterPattern = constraint.toString().toLowerCase().trim { it <= ' ' }
                    for (tag in allTags!!) {
                        if (tag.toLowerCase().startsWith(filterPattern)) {
                            mFilteredTags.add(tag)
                        }
                    }
                }

                filterResults.values = mFilteredTags
                filterResults.count = mFilteredTags.size
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: Filter.FilterResults) {
                mTagsList.clear()
                mTagsList.addAll(mFilteredTags)
                sortData()
                notifyDataSetChanged()
            }
        }
    }

    companion object {

        const val TYPE_NONE = -1
        const val TYPE_ADD_TAG = 0
        const val TYPE_FILTER_BY_TAG = 1
        const val TYPE_CUSTOM_STUDY_TAGS = 2

        private val DIALOG_TYPE_KEY = "dialog_type"
        private val CHECKED_TAGS_KEY = "checked_tags"
        private val ALL_TAGS_KEY = "all_tags"

        fun newInstance(type: Int, checked_tags: ArrayList<String>,
                        all_tags: ArrayList<String>): TagsDialog {
            val t = TagsDialog()

            val args = Bundle()
            args.putInt(DIALOG_TYPE_KEY, type)
            args.putStringArrayList(CHECKED_TAGS_KEY, checked_tags)
            args.putStringArrayList(ALL_TAGS_KEY, all_tags)
            t.arguments = args

            return t
        }
    }
}
