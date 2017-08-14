/****************************************************************************************
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
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
package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.*
import com.ichi2.utils.LongArg
import timber.log.Timber
import java.util.*

class DeckPickerContextMenu : DialogFragment() {

    var deckId: Long by LongArg()


    private val keyValueMap: HashMap<Int, String>
        get() {
            val keyValueMap = HashMap<Int, String>()
            with(keyValueMap) {
                put(CONTEXT_MENU_DELETE_DECK,          resources.getString(R.string.rename_deck))
                put(CONTEXT_MENU_DECK_OPTIONS,         resources.getString(R.string.study_options))
                put(CONTEXT_MENU_CUSTOM_STUDY,         resources.getString(R.string.custom_study))
                put(CONTEXT_MENU_DELETE_DECK,          resources.getString(R.string.contextmenu_deckpicker_delete_deck))
                put(CONTEXT_MENU_EXPORT_DECK,          resources.getString(R.string.export_deck))
                put(CONTEXT_MENU_UNBURY,               resources.getString(R.string.unbury))
                put(CONTEXT_MENU_CUSTOM_STUDY_REBUILD, resources.getString(R.string.rebuild_cram_label))
                put(CONTEXT_MENU_CUSTOM_STUDY_EMPTY,   resources.getString(R.string.empty_cram_label))
            }
            return keyValueMap
        }

    /**
     * Retrieve the list of ids to put in the context menu list
     * @return the ids of which values to show
     */
    private val listIds: IntArray
        get() {
            val col = CollectionHelper.getInstance().getCol(context)
            val itemIds = ArrayList<Int>()
            if (col!!.decks.isDyn(deckId)) {
                itemIds.add(CONTEXT_MENU_CUSTOM_STUDY_REBUILD)
                itemIds.add(CONTEXT_MENU_CUSTOM_STUDY_EMPTY)
            }
            itemIds.add(CONTEXT_MENU_RENAME_DECK)
            itemIds.add(CONTEXT_MENU_DECK_OPTIONS)
            if (!col.decks.isDyn(deckId)) {
                itemIds.add(CONTEXT_MENU_CUSTOM_STUDY)
            }
            itemIds.add(CONTEXT_MENU_DELETE_DECK)
            itemIds.add(CONTEXT_MENU_EXPORT_DECK)
            if (col.sched.haveBuried(deckId)) {
                itemIds.add(CONTEXT_MENU_UNBURY)
            }
            return ContextMenuHelper.integerListToArray(itemIds)
        }

    // Handle item selection on context menu which is shown when the user long-clicks on a deck
    private val mContextMenuListener = MaterialDialog.ListCallback { _, view, _, _ ->
        when (view.id) {
            CONTEXT_MENU_DELETE_DECK -> {
                Timber.i("Delete deck selected")
                (activity as DeckPicker).confirmDeckDeletion()
            }

            CONTEXT_MENU_DECK_OPTIONS -> {
                Timber.i("Open deck options selected")
                (activity as DeckPicker).showContextMenuDeckOptions()
                (activity as AnkiActivity).dismissAllDialogFragments()
            }
            CONTEXT_MENU_CUSTOM_STUDY -> {
                Timber.i("Custom study option selected")
                val d = CustomStudyDialog.newInstance(
                        CustomStudyDialog.CONTEXT_MENU_STANDARD, deckId)
                (activity as AnkiActivity).showDialogFragment(d)
            }
            CONTEXT_MENU_RENAME_DECK -> {
                Timber.i("Rename deck selected")
                (activity as DeckPicker).renameDeckDialog()
            }

            CONTEXT_MENU_EXPORT_DECK -> {
                Timber.i("Export deck selected")
                (activity as DeckPicker).showContextMenuExportDialog()
            }

            CONTEXT_MENU_UNBURY -> {
                Timber.i("Unbury deck selected")
                val col = CollectionHelper.getInstance().getCol(context)
                col!!.sched.unburyCardsForDeck(deckId)
                (activity as StudyOptionsFragment.StudyOptionsListener).onRequireDeckListUpdate()
                (activity as AnkiActivity).dismissAllDialogFragments()
            }
            CONTEXT_MENU_CUSTOM_STUDY_REBUILD -> {
                Timber.i("Empty deck selected")
                (activity as DeckPicker).rebuildFiltered()
                (activity as AnkiActivity).dismissAllDialogFragments()
            }
            CONTEXT_MENU_CUSTOM_STUDY_EMPTY -> {
                Timber.i("Empty deck selected")
                (activity as DeckPicker).emptyFiltered()
                (activity as AnkiActivity).dismissAllDialogFragments()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        val title = CollectionHelper.getInstance().getCol(context)!!.decks.name(deckId)
        val itemIds = listIds
        return MaterialDialog.Builder(activity)
                .title(title)
                .cancelable(true)
                .autoDismiss(false)
                .itemsIds(itemIds)
                .items(*ContextMenuHelper.getValuesFromKeys(keyValueMap, itemIds))
                .itemsCallback(mContextMenuListener)
                .build()
    }

    companion object {
        /**
         * Context Menus
         */
        private val CONTEXT_MENU_RENAME_DECK = 0
        private val CONTEXT_MENU_DECK_OPTIONS = 1
        private val CONTEXT_MENU_CUSTOM_STUDY = 2
        private val CONTEXT_MENU_DELETE_DECK = 3
        private val CONTEXT_MENU_EXPORT_DECK = 4
        private val CONTEXT_MENU_UNBURY = 5
        private val CONTEXT_MENU_CUSTOM_STUDY_REBUILD = 6
        private val CONTEXT_MENU_CUSTOM_STUDY_EMPTY = 7


        fun newInstance(did: Long): DeckPickerContextMenu {
            val f = DeckPickerContextMenu()
            val args = Bundle()
            args.putLong("deckId", did)
            f.arguments = args
            return f
        }
    }
}
