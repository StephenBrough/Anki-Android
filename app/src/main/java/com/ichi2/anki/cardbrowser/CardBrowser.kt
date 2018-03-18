/****************************************************************************************
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana></inigo.aldana>@gmail.com>                       *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */

package com.ichi2.anki.cardbrowser

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.NavigationDrawerActivity
import com.ichi2.anki.NoteEditor
import com.ichi2.anki.flashcardviewer.Previewer
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.deckpicker.model.TaskData
import com.ichi2.anki.dialogs.CardBrowserContextMenu
import com.ichi2.anki.dialogs.CardBrowserMySearchesDialog
import com.ichi2.anki.dialogs.CardBrowserOrderDialog
import com.ichi2.anki.dialogs.TagsDialog
import com.ichi2.anki.dialogs.TagsDialog.TagsDialogListener
import com.ichi2.anki.junkdrawer.AnkiFont
import com.ichi2.anki.junkdrawer.receiver.SdCardReceiver
import com.ichi2.anki.junkdrawer.widgets.DeckDropDownAdapter
import com.ichi2.utils.async.DeckTask
import com.ichi2.libanki.Card
import com.ichi2.libanki.Collection
import com.ichi2.utils.themes.Themes
import com.ichi2.utils.upgrade.Upgrade
import com.ichi2.widget.WidgetStatus

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.regex.Pattern

import timber.log.Timber

class CardBrowser : NavigationDrawerActivity(), DeckDropDownAdapter.SubtitleListener {

    private var mDeckNames: HashMap<String, String>? = null
    private var mDropDownDecks: ArrayList<JSONObject>? = null
    private var mCardsListView: ListView? = null
    private var mSearchView: SearchView? = null
    private var mCardsAdapter: MultiColumnListAdapter? = null
    private var mSearchTerms: String? = null
    private var mRestrictOnDeck: String? = null

    private var mSearchItem: MenuItem? = null
    private var mSaveSearchItem: MenuItem? = null
    private var mMySearchesItem: MenuItem? = null

    private var mPositionInCardsList: Int = 0

    private var mOrder: Int = 0
    private var mOrderAsc: Boolean = false
    private var mColumn1Index: Int = 0
    private var mColumn2Index: Int = 0
    private var mLastRenderStart: Long = 0
    private var mDropDownAdapter: DeckDropDownAdapter? = null
    private var mActionBarSpinner: Spinner? = null
    private var mReloadRequired = false

    private var mCards: List<Map<String, String>>? = null
    get () {
        if (mCards == null) {
            mCards = ArrayList()
        }
        return mCards as MutableList<MutableMap<String, String>>
    }

//    private val cards: MutableList<MutableMap<String, String>>
//        get() {
//            if (mCards == null) {
//                mCards = ArrayList()
//            }
//            return mCards as MutableList<MutableMap<String, String>>
//        }

    private val cardIds: LongArray
        get() {
            val l = LongArray(mCards!!.size)
            for (i in mCards!!.indices) {
                l[i] = java.lang.Long.parseLong(mCards!![i]["id"])
            }
            return l
        }

    /**
     * Broadcast that informs us when the sd card is about to be unmounted
     */
    private var mUnmountReceiver: BroadcastReceiver? = null

    private val mContextMenuListener = MaterialDialog.ListCallback { _, _, which, _ ->
        if (mCards!!.size == 0) {
            // Don't do anything if mCards empty
            searchCards()
            return@ListCallback
        }
        val card = col!!.getCard(java.lang.Long.parseLong(mCards!![mPositionInCardsList]["id"]))
        when (which) {
            CardBrowserContextMenu.CONTEXT_MENU_MARK -> {
                onMark(card)
                updateCardInList(card, null)
                return@ListCallback
            }

            CardBrowserContextMenu.CONTEXT_MENU_SUSPEND -> {
                if (currentCardInUseByReviewer()) {
                    mReloadRequired = true
                }
                DeckTask.launchDeckTask(
                        DeckTask.TASK_TYPE_DISMISS,
                        mSuspendCardHandler,
                        TaskData(arrayOf<Any?>(card, Collection.DismissType.SUSPEND_CARD)))
                return@ListCallback
            }

            CardBrowserContextMenu.CONTEXT_MENU_DELETE -> {
                MaterialDialog.Builder(this@CardBrowser)
                        .title(getString(R.string.delete_card_title))
                        .iconAttr(R.attr.dialogErrorIcon)
                        .content(getString(R.string.delete_card_message, mCards!![mPositionInCardsList]["sfld"]))
                        .positiveText(getString(R.string.dialog_positive_delete))
                        .negativeText(getString(R.string.dialog_cancel))
                        .onPositive { _, _ ->
                            deleteNote(card)
                            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_DISMISS,
                                    mDeleteNoteHandler,
                                    TaskData(arrayOf<Any?>(card, Collection.DismissType.DELETE_NOTE)))
                        }
                        .build().show()
                return@ListCallback
            }

            CardBrowserContextMenu.CONTEXT_MENU_DETAILS -> {
//                val cardId = java.lang.Long.parseLong(cards[mPositionInCardsList]["id"])
                val previewer = Previewer.createIntent(this, mPositionInCardsList, cardIds)
                startActivity(previewer)
            }
        }
    }


    private val mOrderDialogListener = MaterialDialog.ListCallbackSingleChoice { _, _, which, _ ->
        if (which != mOrder) {
            mOrder = which
            mOrderAsc = false
            try {
                if (mOrder == 0) {
                    col!!.conf.put("sortType", fSortTypes[1])
                    AnkiDroidApp.getSharedPrefs(baseContext).edit()
                            .putBoolean("cardBrowserNoSorting", true)
                            .commit()
                } else {
                    col!!.conf.put("sortType", fSortTypes[mOrder])
                    AnkiDroidApp.getSharedPrefs(baseContext).edit()
                            .putBoolean("cardBrowserNoSorting", false)
                            .commit()
                }
                // default to descending for non-text fields
                if (fSortTypes[mOrder] == "noteFld") {
                    mOrderAsc = true
                }
                col!!.conf.put("sortBackwards", mOrderAsc)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            searchCards()
        } else if (which != CARD_ORDER_NONE) {
            mOrderAsc = !mOrderAsc
            try {
                col!!.conf.put("sortBackwards", mOrderAsc)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            Collections.reverse(mCards!!)
            updateList()
        }
        true
    }

    private val mMySearchesDialogListener = object : CardBrowserMySearchesDialog.MySearchesDialogListener {
        override fun onSelection(searchName: String?) {
            val savedFiltersObj = col!!.conf.optJSONObject("savedFilters")
            if (savedFiltersObj != null) {
                mSearchTerms = savedFiltersObj.optString(searchName)
                mSearchView!!.setQuery(mSearchTerms, false)
                mSearchItem!!.expandActionView()
                searchCards()
            }
        }

        override fun onRemoveSearch(searchName: String?) {
            try {
                val savedFiltersObj = col!!.conf.optJSONObject("savedFilters")

                if (savedFiltersObj != null && savedFiltersObj.has(searchName)) {
                    savedFiltersObj.remove(searchName)
                    col!!.conf.put("savedFilters", savedFiltersObj)
                    col!!.flush()

                    if (savedFiltersObj.length() == 0) {
                        mMySearchesItem!!.isVisible = false
                    }
                }

            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }

        override fun onSaveSearch(searchName: String, searchTerms: String?) {
            if (searchName.isEmpty()) {
                UIUtils.showThemedToast(this@CardBrowser,
                        getString(R.string.card_browser_list_my_searches_new_search_error_empty_name), true)
                return
            }
            try {
                var savedFiltersObj: JSONObject? = col!!.conf.optJSONObject("savedFilters")
                var shouldSave = false
                if (savedFiltersObj == null) {
                    savedFiltersObj = JSONObject()
                    savedFiltersObj.put(searchName, searchTerms)
                    shouldSave = true
                } else if (!savedFiltersObj.has(searchName)) {
                    savedFiltersObj.put(searchName, searchTerms)
                    shouldSave = true
                } else {
                    UIUtils.showThemedToast(this@CardBrowser,
                            getString(R.string.card_browser_list_my_searches_new_search_error_dup), true)
                }
                if (shouldSave) {
                    col!!.conf.put("savedFilters", savedFiltersObj)
                    col!!.flush()
                    mSearchView!!.setQuery("", false)
                    mMySearchesItem!!.isVisible = true
                }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }

    /**
     * @return text to be used in the subtitle of the drop-down deck selector
     */
    override val subtitleText: String
        get() {
            val count = mCards!!.size
            return resources.getQuantityString(R.plurals.card_browser_subtitle, count, count)
        }

    private val mUpdateCardHandler = object : DeckTask.TaskListener() {
        override fun onPreExecute() {
            showProgressBar()
        }

        override fun onProgressUpdate(vararg values: TaskData) {
            updateCardInList(values[0].card!!, values[0].string)
        }

        override fun onPostExecute(result: TaskData) {
            Timber.d("Card Browser - mUpdateCardHandler.onPostExecute()")
            if (!result.boolean) {
                closeCardBrowser(DeckPickerActivity.RESULT_DB_ERROR)
            }

            hideProgressBar()
        }

        override fun onCancelled() {}
    }


    private val mSuspendCardHandler = object : DeckTask.TaskListener() {
        override fun onPreExecute() {
            showProgressBar()
        }

        override fun onProgressUpdate(vararg values: TaskData) {}

        override fun onPostExecute(result: TaskData) {
            if (result.boolean) {
                updateCardInList(col!!.getCard(java.lang.Long.parseLong(mCards!![mPositionInCardsList]["id"])), null)
            } else {
                closeCardBrowser(DeckPickerActivity.RESULT_DB_ERROR)
            }
            hideProgressBar()
        }

        override fun onCancelled() {}
    }

    private val mDeleteNoteHandler = object : DeckTask.TaskListener() {
        override fun onPreExecute() {
            showProgressBar()
        }

        override fun onProgressUpdate(vararg values: TaskData) {}

        override fun onPostExecute(result: TaskData) {
            hideProgressBar()
        }

        override fun onCancelled() {}
    }

    private val mSearchCardsHandler = object : DeckTask.TaskListener() {
        override fun onProgressUpdate(vararg values: TaskData) {
            if (values[0] != null) {
                mCards = values[0].cards
                updateList()
            }
        }

        override fun onPreExecute() {
            showProgressBar()
        }

        override fun onPostExecute(result: TaskData?) {
            if (result != null && mCards != null) {
                Timber.i("CardBrowser:: Completed doInBackgroundSearchCards Successfuly")
                updateList()
                if (!mSearchView!!.isIconified) {
                    UIUtils.showSimpleSnackbar(this@CardBrowser, subtitleText, false)
                }
            }
            hideProgressBar()
        }

        override fun onCancelled() {
            Timber.d("doInBackgroundSearchCards onCancelled() called")
        }
    }

    private val mRenderQAHandler = object : DeckTask.TaskListener() {
        override fun onProgressUpdate(vararg values: TaskData) {
            // Note: This is called every time a card is rendered.
            // It blocks the long-click callback while the task is running, so usage of the task should be minimized
            mCardsAdapter!!.notifyDataSetChanged()
        }

        override fun onPreExecute() {
            Timber.d("Starting Q&A background rendering")
        }

        override fun onPostExecute(result: TaskData?) {
            if (result != null) {
                hideProgressBar()
                mCardsAdapter!!.notifyDataSetChanged()
                Timber.d("Completed doInBackgroundRenderBrowserQA Successfuly")
            } else {
                // Might want to do something more proactive here like show a message box?
                Timber.e("doInBackgroundRenderBrowserQA was not successful... continuing anyway")
            }
        }

        override fun onCancelled() {
            hideProgressBar()
        }
    }


    private fun onSearch() {
        mSearchTerms = mSearchView!!.query.toString()
        if (mSearchTerms!!.isEmpty()) {
            mSearchView!!.queryHint = resources.getString(R.string.downloaddeck_search)
        }
        searchCards()
    }

    private fun onMark(card: Card) {
        val note = card.note()
        if (note.hasTag("marked")) {
            note.delTag("marked")
        } else {
            note.addTag("marked")
        }
        note.flush()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        setContentView(R.layout.activity_card_browser)
        initNavigationDrawer(findViewById(android.R.id.content))
        startLoadingCollection()
    }


    // Finish initializing the activity after the collection has been correctly loaded
    override fun onCollectionLoaded(col: Collection) {
        super.onCollectionLoaded(col)
        Timber.d("onCollectionLoaded()")
        mDeckNames = HashMap()
        for (did in col.decks.allIds()) {
            mDeckNames!![did.toString()] = col.decks.name(did)
        }
        registerExternalStorageListener()

        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)

        // Add drop-down menu to select deck to action bar.
        mDropDownDecks = col.decks.allSorted()
        mDropDownAdapter = DeckDropDownAdapter(this, mDropDownDecks!!)
        val mActionBar = supportActionBar
        mActionBar?.setDisplayShowTitleEnabled(false)
        mActionBarSpinner = findViewById<View>(R.id.toolbarSpinner) as Spinner
        mActionBarSpinner!!.adapter = mDropDownAdapter
        mActionBarSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectDropDownItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        mActionBarSpinner!!.visibility = View.VISIBLE

        try {
            mOrder = CARD_ORDER_NONE
            val colOrder = col.conf.getString("sortType")
            for (c in fSortTypes.indices) {
                if (fSortTypes[c] == colOrder) {
                    mOrder = c
                    break
                }
            }
            if (mOrder == 1 && preferences.getBoolean("cardBrowserNoSorting", false)) {
                mOrder = 0
            }
            mOrderAsc = Upgrade.upgradeJSONIfNecessary(col, col.conf, "sortBackwards", false)
            // default to descending for non-text fields
            if (fSortTypes[mOrder] == "noteFld") {
                mOrderAsc = !mOrderAsc
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        mCards = ArrayList()
        mCardsListView = findViewById<View>(R.id.card_browser_list) as ListView
        // Create a spinner for column1
        val cardsColumn1Spinner = findViewById<View>(R.id.browser_column1_spinner) as Spinner
        val column1Adapter = ArrayAdapter.createFromResource(this,
                R.array.browser_column1_headings, android.R.layout.simple_spinner_item)
        column1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cardsColumn1Spinner.adapter = column1Adapter
        mColumn1Index = AnkiDroidApp.getSharedPrefs(baseContext).getInt("cardBrowserColumn1", 0)
        cardsColumn1Spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                // If a new column was selected then change the key used to map from mCards to the column TextView
                if (pos != mColumn1Index) {
                    mColumn1Index = pos
                    AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().baseContext).edit()
                            .putInt("cardBrowserColumn1", mColumn1Index).commit()
                    val fromMap = mCardsAdapter!!.fromMapping
                    fromMap!![0] = COLUMN1_KEYS[mColumn1Index]
                    mCardsAdapter!!.fromMapping = fromMap
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do Nothing
            }
        }
        // Load default value for column2 selection
        mColumn2Index = AnkiDroidApp.getSharedPrefs(baseContext).getInt("cardBrowserColumn2", 0)
        // Setup the column 2 heading as a spinner so that users can easily change the column type
        val cardsColumn2Spinner = findViewById<View>(R.id.browser_column2_spinner) as Spinner
        val column2Adapter = ArrayAdapter.createFromResource(this,
                R.array.browser_column2_headings, android.R.layout.simple_spinner_item)
        column2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cardsColumn2Spinner.adapter = column2Adapter
        // Create a new list adapter with updated column map any time the user changes the column
        cardsColumn2Spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                // If a new column was selected then change the key used to map from mCards to the column TextView
                if (pos != mColumn2Index) {
                    mColumn2Index = pos
                    AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().baseContext).edit()
                            .putInt("cardBrowserColumn2", mColumn2Index).commit()
                    val fromMap = mCardsAdapter!!.fromMapping
                    fromMap!![1] = COLUMN2_KEYS[mColumn2Index]
                    mCardsAdapter!!.fromMapping = fromMap
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do Nothing
            }
        }
        // get the font and font size from the preferences
        val sflRelativeFontSize = preferences.getInt("relativeCardBrowserFontSize", DEFAULT_FONT_SIZE_RATIO)
        val sflCustomFont = preferences.getString("browserEditorFont", "")
        // make a new list adapter mapping the data in mCards to column1 and column2 of R.layout.card_item_browser
        mCardsAdapter = MultiColumnListAdapter(
                this,
                R.layout.card_item_browser,
                arrayOf(COLUMN1_KEYS[mColumn1Index], COLUMN2_KEYS[mColumn2Index]),
                intArrayOf(R.id.card_sfld, R.id.card_column2),
                "flags",
                sflRelativeFontSize,
                sflCustomFont!!)
        // link the adapter to the main mCardsListView
        mCardsListView!!.adapter = mCardsAdapter
        // make the items (e.g. question & answer) render dynamically when scrolling
        mCardsListView!!.setOnScrollListener(RenderOnScroll())
        // set the spinner index
        cardsColumn1Spinner.setSelection(mColumn1Index)
        cardsColumn2Spinner.setSelection(mColumn2Index)


        mCardsListView!!.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            // load up the card selected on the list
            mPositionInCardsList = position
            val cardId = java.lang.Long.parseLong(mCards!![mPositionInCardsList]["id"])
            sCardBrowserCard = col.getCard(cardId)
            // start note editor using the card we just loaded
            val editCard = Intent(this@CardBrowser, NoteEditor::class.java)
            editCard.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CARDBROWSER_EDIT)
            editCard.putExtra(NoteEditor.EXTRA_CARD_ID, sCardBrowserCard!!.id)
            startActivityForResult(editCard, EDIT_CARD)
        }

        mCardsListView!!.onItemLongClickListener = OnItemLongClickListener { _, _, position, _ ->
            mPositionInCardsList = position
            val card = mCards!![mPositionInCardsList]
            val flags = Integer.parseInt(card["flags"])
            val cardName = card["sfld"]
            val isMarked = flags == 2 || flags == 3
            val isSuspended = flags == 1 || flags == 3
            showDialogFragment(CardBrowserContextMenu
                    .newInstance(cardName!!, isMarked, isSuspended, mContextMenuListener))
            true
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        // initialize mSearchTerms to a default value
        mSearchTerms = ""

        // set the currently selected deck
        selectDropDownItem(getDeckPositionFromDeckId(intent.getLongExtra("defaultDeckId", -1)))
    }


    override fun onStop() {
        Timber.d("onStop()")
        // cancel rendering the question and answer, which has shared access to mCards
        DeckTask.cancelTask(DeckTask.TASK_TYPE_SEARCH_CARDS)
        DeckTask.cancelTask(DeckTask.TASK_TYPE_RENDER_BROWSER_QA)
        super.onStop()
        if (!isFinishing) {
            WidgetStatus.update(this)
            UIUtils.saveCollectionInBackground()
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        super.onDestroy()
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver)
        }
    }

    override fun onBackPressed() {
        if (isDrawerOpen) {
            super.onBackPressed()
        } else {
            Timber.i("Back key pressed")
            val data = Intent()
            if (mReloadRequired) {
                // Add reload flag to result intent so that schedule reset when returning to note editor
                data.putExtra("reloadRequired", true)
            }
            closeCardBrowser(Activity.RESULT_OK, data)
        }
    }

    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
        selectNavigationItem(R.id.nav_browser)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.card_browser, menu)
        mSaveSearchItem = menu.findItem(R.id.action_save_search)
        mSaveSearchItem!!.isVisible = false // The searchview's query always starts empty.
        mMySearchesItem = menu.findItem(R.id.action_list_my_searches)
        val savedFiltersObj = col!!.conf.optJSONObject("savedFilters")
        mMySearchesItem!!.isVisible = savedFiltersObj != null && savedFiltersObj.length() > 0
        mSearchItem = menu.findItem(R.id.action_search)
        MenuItemCompat.setOnActionExpandListener(mSearchItem!!, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // SearchView doesn't support empty queries so we always reset the search when collapsing
                mSearchTerms = ""
                mSearchView!!.setQuery(mSearchTerms, false)
                searchCards()
                // invalidate options menu so that disappeared icons would appear again
                supportInvalidateOptionsMenu()
                return true
            }
        })

        mSearchView = MenuItemCompat.getActionView(mSearchItem!!) as SearchView
        mSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                mSaveSearchItem!!.isVisible = !TextUtils.isEmpty(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                onSearch()
                mSearchView!!.clearFocus()
                return true
            }
        })

        mSearchView!!.setOnSearchClickListener {
            // Provide SearchView with the previous search terms
            mSearchView!!.setQuery(mSearchTerms, false)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        when (item.itemId) {
            R.id.action_add_card_from_card_browser -> {
                val intent = Intent(this@CardBrowser, NoteEditor::class.java)
                intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CARDBROWSER_ADD)
                startActivityForResult(intent, ADD_NOTE)
                return true
            }

            R.id.action_save_search -> {
                val searchTerms = mSearchView!!.query.toString()
                showDialogFragment(CardBrowserMySearchesDialog.newInstance(null!!, mMySearchesDialogListener,
                        searchTerms, CardBrowserMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_SAVE))
                return true
            }

            R.id.action_list_my_searches -> {
                val savedFiltersObj = col!!.conf.optJSONObject("savedFilters")
                val savedFilters = HashMap<String, String>()
                if (savedFiltersObj != null) {
                    val it = savedFiltersObj.keys()
                    while (it.hasNext()) {
                        val searchName = it.next()
                        savedFilters[searchName] = savedFiltersObj.optString(searchName)
                    }
                }
                showDialogFragment(CardBrowserMySearchesDialog.newInstance(savedFilters, mMySearchesDialogListener,
                        "", CardBrowserMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_LIST))
                return true
            }

            R.id.action_sort_by_size -> {
                showDialogFragment(CardBrowserOrderDialog
                        .newInstance(mOrder, mOrderAsc, mOrderDialogListener))
                return true
            }

            R.id.action_show_marked -> {
                mSearchTerms = "tag:marked"
                mSearchView!!.setQuery("", false)
                mSearchView!!.queryHint = resources.getString(R.string.card_browser_show_marked)
                searchCards()
                return true
            }

            R.id.action_show_suspended -> {
                mSearchTerms = "is:suspended"
                mSearchView!!.setQuery("", false)
                mSearchView!!.queryHint = resources.getString(R.string.card_browser_show_suspended)
                searchCards()
                return true
            }

            R.id.action_search_by_tag -> {
                showTagsDialog()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // FIXME:
        Timber.d("onActivityResult(requestCode=%d, resultCode=%d)", requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == DeckPickerActivity.RESULT_DB_ERROR) {
            closeCardBrowser(DeckPickerActivity.RESULT_DB_ERROR)
        }

        if (requestCode == EDIT_CARD && resultCode != Activity.RESULT_CANCELED) {
            Timber.i("CardBrowser:: CardBrowser: Saving card...")
            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_UPDATE_FACT, mUpdateCardHandler,
                    TaskData(sCardBrowserCard!!, false))
        } else if (requestCode == ADD_NOTE && resultCode == Activity.RESULT_OK) {
            if (mSearchView != null) {
                mSearchTerms = mSearchView!!.query.toString()
                searchCards()
            } else {
                Timber.w("Note was added from browser and on return mSearchView == null")
            }

        }

        if (requestCode == EDIT_CARD && data != null && data.hasExtra("reloadRequired")) {
            // if reloadRequired flag was sent from note editor then reload card list
            searchCards()
            // keep track of changes for reviewer
            if (currentCardInUseByReviewer()) {
                mReloadRequired = true
            }
        }
    }

    private fun currentCardInUseByReviewer(): Boolean {
        if (intent.hasExtra("currentCard") && mCards!!.size > mPositionInCardsList
                && mCards!![mPositionInCardsList] != null) {
            val reviewerCard = intent.extras!!.getLong("currentCard")
            val selectedCard = java.lang.Long.parseLong(mCards!![mPositionInCardsList]["id"])
            return selectedCard == reviewerCard
        }
        return false
    }

    private fun showTagsDialog() {
        val dialog = TagsDialog.newInstance(
                TagsDialog.TYPE_FILTER_BY_TAG, ArrayList(), ArrayList(col!!.tags.all()))
        dialog.setTagsDialogListener(object : TagsDialogListener {
            override fun onPositive(selectedTags: List<String>, option: Int) {
                mSearchView!!.setQuery("", false)
                val tags = selectedTags.toString()
                mSearchView!!.queryHint = resources.getString(R.string.card_browser_tags_shown,
                        tags.substring(1, tags.length - 1))
                val sb = StringBuilder()
                when (option) {
                    1 -> sb.append("is:new ")
                    2 -> sb.append("is:due ")
                    else -> {
                    }
                }// Logging here might be appropriate : )
                var i = 0
                for (tag in selectedTags) {
                    if (i != 0) {
                        sb.append("or ")
                    } else {
                        sb.append("(") // Only if we really have selected tags
                    }
                    sb.append("tag:").append(tag).append(" ")
                    i++
                }
                if (i > 0) {
                    sb.append(")") // Only if we added anything to the tag list
                }
                mSearchTerms = sb.toString()
                searchCards()
            }
        })
        showDialogFragment(dialog)
    }

    fun selectDropDownItem(position: Int) {
        mActionBarSpinner!!.setSelection(position)
        if (position == 0) {
            mRestrictOnDeck = ""
        } else {
            val deck = mDropDownDecks!![position - 1]
            val deckName: String
            try {
                deckName = deck.getString("name")
            } catch (e: JSONException) {
                throw RuntimeException()
            }

            try {
                col!!.decks.select(deck.getLong("id"))
            } catch (e: JSONException) {
                Timber.e(e, "Could not get ID from deck")
            }

            mRestrictOnDeck = "deck:\"$deckName\" "
        }
        searchCards()
    }

    private fun searchCards() {
        // cancel the previous search & render tasks if still running
        DeckTask.cancelTask(DeckTask.TASK_TYPE_SEARCH_CARDS)
        DeckTask.cancelTask(DeckTask.TASK_TYPE_RENDER_BROWSER_QA)
        val searchText: String
        if (mSearchTerms!!.contains("deck:")) {
            searchText = mSearchTerms!!
        } else {
            searchText = mRestrictOnDeck!! + mSearchTerms!!
        }
        if (colIsOpen() && mCardsAdapter != null) {
            // clear the existing card list
            (mCards as MutableList).clear()
            mCardsAdapter!!.notifyDataSetChanged()
            //  estimate maximum number of cards that could be visible (assuming worst-case minimum row height of 20dp)
            val numCardsToRender = Math.ceil((mCardsListView!!.height / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)).toDouble()).toInt() + 5
            // Perform database query to get all card ids
            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_SEARCH_CARDS, mSearchCardsHandler, TaskData(
                    arrayOf<Any?>(mDeckNames!!, searchText, mOrder != CARD_ORDER_NONE, numCardsToRender)))
        }
    }

    private fun updateList() {
        mCardsAdapter!!.notifyDataSetChanged()
        mDropDownAdapter!!.notifyDataSetChanged()
    }

    private fun getPosition(list: List<Map<String, String>>, cId: Long): Int {
        val cardId = cId.toString()
        for (i in list.indices) {
            if (list[i]["id"] == cardId) {
                return i
            }
        }
        return -1
    }

    /**
     * Get the index in the deck spinner for a given deck ID
     * @param did the id of a deck
     * @return the corresponding index in the deck spinner, or 0 if not found
     */
    private fun getDeckPositionFromDeckId(did: Long): Int {
        for (dropDownDeckIdx in mDropDownDecks!!.indices) {
            val deck = mDropDownDecks!![dropDownDeckIdx]
            val cdid: Long
            try {
                cdid = deck.getLong("id")
            } catch (e: JSONException) {
                throw RuntimeException()
            }

            if (cdid == did) {
                // NOTE: mDropDownDecks.get(0) is the first deck, whereas index 0 in mActionBarSpinner is "All Decks"
                return dropDownDeckIdx + 1
            }
        }
        // Fall back on "All Decks" if did wasn't found
        return 0
    }

    private fun updateCardInList(card: Card, updatedCardTags: String?) {
        val note = card.note()
        var pos: Int
        for (c in note.cards()) {
            // get position in the mCards search results HashMap
            pos = getPosition(mCards!!, c.id)
            if (pos < 0 || pos >= mCards!!.size) {
                continue
            }
            // update tags
            if (updatedCardTags != null) {

                (mCards!![pos] as MutableMap).put("tags", updatedCardTags)
            }
            // update sfld
            val sfld = note.sFld
            (mCards!![pos] as MutableMap).put("sfld", sfld)
            // update Q & A etc
            updateSearchItemQA((mCards!![pos] as MutableMap), c)
            // update deck
            val deckName: String
            try {
                deckName = col!!.decks.get(card.did).getString("name")
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            (mCards!![pos] as MutableMap).put("deck", deckName)
            // update flags (marked / suspended / etc) which determine color
            val flags = Integer.toString((if (c.queue == -1) 1 else 0) + if (note.hasTag("marked")) 2 else 0)
            (mCards!![pos] as MutableMap).put("flags", flags)
        }
        updateList()
    }


    private fun deleteNote(card: Card) {
        if (currentCardInUseByReviewer()) {
            mReloadRequired = true
        }
        val cards = card.note().cards()
        var pos: Int
        for (c in cards) {
            pos = getPosition(mCards!!, c.id)
            if (pos >= 0 && pos < mCards!!.size) {
                (mCards as MutableList).removeAt(pos)
            }
        }
        // Delete itself if not deleted
        pos = getPosition(mCards!!, card.id)
        if (pos >= 0 && pos < mCards!!.size) {
            (mCards as MutableList).removeAt(pos)
        }
        updateList()
    }

    private fun closeCardBrowser(result: Int, data: Intent? = null) {
        // Pass the originally selected deck back so that the calling Activity can switch back to it
        if (intent.hasExtra("selectedDeck")) {
            data!!.putExtra("originalDeck", intent.getLongExtra("selectedDeck", 0L))
        }
        // Pass a flag to say whether "All Decks" was selected so that the calling Activity can remember it
        data!!.putExtra("allDecksSelected", mActionBarSpinner!!.selectedItemPosition == 0)
        // Set result and finish
        setResult(result, data)
        finish()
    }

    /**
     * Render the second column whenever the user stops scrolling
     */
    private inner class RenderOnScroll : AbsListView.OnScrollListener {
        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            // Show the progress bar if scrolling to given position requires rendering of the question / answer
            val lastVisibleItem = firstVisibleItem + visibleItemCount
            val size = mCards!!.size
            if (size > 0 && firstVisibleItem < size && lastVisibleItem - 1 < size) {
                val firstAns = mCards!![firstVisibleItem]["answer"]
                // Note: max value of lastVisibleItem is totalItemCount, so need to subtract 1
                val lastAns = mCards!![lastVisibleItem - 1]["answer"]
                if (firstAns != null && firstAns == "" || lastAns != null && lastAns == "") {
                    showProgressBar()
                    // Also start rendering the items on the screen every 300ms while scrolling
                    val currentTime = SystemClock.elapsedRealtime()
                    if (currentTime - mLastRenderStart > 300 || lastVisibleItem >= totalItemCount) {
                        mLastRenderStart = currentTime
                        DeckTask.cancelTask(DeckTask.TASK_TYPE_RENDER_BROWSER_QA)
                        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RENDER_BROWSER_QA, mRenderQAHandler,
                                TaskData(arrayOf(mCards, firstVisibleItem, visibleItemCount)))
                    }
                }
            }
        }

        override fun onScrollStateChanged(listView: AbsListView, scrollState: Int) {
            // TODO: Try change to RecyclerView as currently gets stuck a lot when using scrollbar on right of ListView
            // Start rendering the question & answer every time the user stops scrolling
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                val startIdx = listView.firstVisiblePosition
                val numVisible = listView.lastVisiblePosition - startIdx
                DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RENDER_BROWSER_QA, mRenderQAHandler,
                        TaskData(arrayOf(mCards, startIdx - 5, 2 * numVisible + 5)))
            }
        }
    }

    private inner class MultiColumnListAdapter(context: Context, private val mResource: Int, private var mFromKeys: Array<String>?, private val mToIds: IntArray, private val mColorFlagKey: String,
                                               private val mFontSizeScalePcent: Int, customFont: String) : BaseAdapter() {
        private var mOriginalTextSize = -1.0f
        private var mCustomTypeface: Typeface? = null
        private val mInflater: LayoutInflater


        var fromMapping: Array<String>?
            get() = mFromKeys
            set(from) {
                mFromKeys = from
                notifyDataSetChanged()
            }


        init {
            if (customFont != "") {
                mCustomTypeface = AnkiFont.getTypeface(context, customFont)
            }
            mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }


        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Get the main container view if it doesn't already exist, and call bindView
            val v: View
            if (convertView == null) {
                v = mInflater.inflate(mResource, parent, false)
                val count = mToIds.size
                val columns = arrayOfNulls<View>(count)
                for (i in 0 until count) {
                    columns[i] = v.findViewById(mToIds[i])
                }
                v.tag = columns
            } else {
                v = convertView
            }
            bindView(position, v)
            return v
        }


        private fun bindView(position: Int, v: View) {
            // Draw the content in the columns
            val columns = v.tag as Array<View>
            val dataSet = mCards!![position]
            val colorIdx = getColor(dataSet[mColorFlagKey])
            val colors = Themes.getColorFromAttr(this@CardBrowser, intArrayOf(android.R.attr.colorBackground, R.attr.markedColor, R.attr.suspendedColor, R.attr.markedColor))
            for (i in mToIds.indices) {
                val col = columns[i] as TextView
                // set font for column
                setFont(col)
                // set background color for column
                col.setBackgroundColor(colors[colorIdx])
                // set text for column
                col.text = dataSet[mFromKeys!![i]]
            }
        }


        private fun setFont(v: TextView) {
            // Set the font and font size for a TextView v
            val currentSize = v.textSize
            if (mOriginalTextSize < 0) {
                mOriginalTextSize = v.textSize
            }
            // do nothing when pref is 100% and apply scaling only once
            if (mFontSizeScalePcent != 100 && Math.abs(mOriginalTextSize - currentSize) < 0.1) {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, mOriginalTextSize * (mFontSizeScalePcent / 100.0f))
            }

            if (mCustomTypeface != null) {
                v.typeface = mCustomTypeface
            }
        }

        /**
         * Get the index that specifies the background color of items in the card list based on the String tag
         * @param flag a string flag
         * @return index into TypedArray specifying the background color
         */
        private fun getColor(flag: String?): Int {
            if (flag == null) {
                return BACKGROUND_NORMAL
            }
            when (flag) {
                "1" -> return BACKGROUND_SUSPENDED
                "2" -> return BACKGROUND_MARKED
                "3" -> return BACKGROUND_MARKED_SUSPENDED
                else -> return BACKGROUND_NORMAL
            }
        }


        override fun getCount(): Int {
            return mCards!!.size
        }


        override fun getItem(position: Int): Any {
            return mCards!![position]
        }


        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }


    /**
     * Show/dismiss dialog when sd card is ejected/remounted (collection is saved by SdCardReceiver)
     */
    private fun registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == SdCardReceiver.MEDIA_EJECT) {
                        finish()
                    }
                }
            }
            val iFilter = IntentFilter()
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT)
            registerReceiver(mUnmountReceiver, iFilter)
        }
    }

    companion object {

        var sCardBrowserCard: Card? = null

        private val BACKGROUND_NORMAL = 0
        private val BACKGROUND_MARKED = 1
        private val BACKGROUND_SUSPENDED = 2
        private val BACKGROUND_MARKED_SUSPENDED = 3

        private val EDIT_CARD = 0
        private val ADD_NOTE = 1
        private val DEFAULT_FONT_SIZE_RATIO = 100
        // Should match order of R.array.card_browser_order_labels
        val CARD_ORDER_NONE = 0
        private val fSortTypes = arrayOf("", "noteFld", "noteCrt", "noteMod", "cardMod", "cardDue", "cardIvl", "cardEase", "cardReps", "cardLapses")
        // list of available keys in mCards corresponding to the column names in R.array.browser_column2_headings.
        // Note: the last 6 are currently hidden
        private val COLUMN1_KEYS = arrayOf("question", "sfld")
        private val COLUMN2_KEYS = arrayOf("answer", "card", "deck", "note", "question", "tags", "lapses", "reviews", "changed", "created", "due", "ease", "edited", "interval")

        fun updateSearchItemQA(item: MutableMap<String, String>, c: Card) {
            // render question and answer
            val qa = c._getQA(true, true)
            // Render full question / answer if the bafmt (i.e. "browser appearance") setting forced blank result
            if (qa["q"] == "" || qa["a"] == "") {
                val qaFull = c._getQA(true, false)
                if (qa["q"] == "") {
                    qa["q"] = qaFull["q"]
                }
                if (qa["a"] == "") {
                    qa["a"] = qaFull["a"]
                }
            }
            // update the original hash map to include rendered question & answer
            val q = qa["q"]
            var a = qa["a"]
            // remove the question from the start of the answer if it exists
            if (a!!.startsWith(q!!)) {
                a = a.replaceFirst(Pattern.quote(q).toRegex(), "")
            }
            // put all of the fields in except for those that have already been pulled out straight from the
            // database
            item["answer"] = formatQA(a)
            item["card"] = c.template().optString("name")
            // item.put("changed",strftime("%Y-%m-%d", localtime(c.getMod())));
            // item.put("created",strftime("%Y-%m-%d", localtime(c.note().getId()/1000)));
            // item.put("due",getDueString(c));
            // item.put("ease","");
            // item.put("edited",strftime("%Y-%m-%d", localtime(c.note().getMod())));
            // item.put("interval","");
            item["lapses"] = Integer.toString(c.lapses)
            item["note"] = c.model().optString("name")
            item["question"] = formatQA(q)
            item["reviews"] = Integer.toString(c.reps)
        }
    }
}
