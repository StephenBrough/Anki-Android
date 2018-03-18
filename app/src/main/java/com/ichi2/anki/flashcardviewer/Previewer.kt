/***************************************************************************************
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana></inigo.aldana>@gmail.com>                       *
 * Copyright (c) 2013 Jolta Technologies                                                *
 * Copyright (c) 2014 Bruno Romero de Azevedo <brunodea></brunodea>@inf.ufsm.br>                    *
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

package com.ichi2.anki.flashcardviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ichi2.anki.R

import com.ichi2.libanki.Collection
import com.ichi2.utils.themes.Themes

import timber.log.Timber

/**
 * The previewer intent must supply an array of cards to show and the index in the list from where
 * to begin showing them. Special rules are applied if the list size is 1 (i.e., no scrolling
 * buttons will be shown).
 */
class Previewer : AbstractFlashcardViewer() {
    private var mCardList: LongArray? = null
    private var mIndex: Int = 0
    private var mShowingAnswer: Boolean = false

    private val mSelectScrollHandler = View.OnClickListener { view ->
        if (!mShowingAnswer) {
            displayCardAnswer()
        } else {
            if (view.id == R.id.flashcard_layout_ease1) {
                mIndex--
            } else if (view.id == R.id.flashcard_layout_ease2) {
                mIndex++
            }
            mCurrentCard = col!!.getCard(mCardList!![mIndex])
            displayCardQuestion()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")
        mCardList = intent.getLongArrayExtra("cardList")
        mIndex = intent.getIntExtra("index", -1)
        if (mCardList!!.size == 0 || mIndex < 0 || mIndex > mCardList!!.size - 1) {
            Timber.e("Previewer started with empty card list or invalid index")
            finish();
        }
        super.onCreate(savedInstanceState)
        showBackIcon()
        // Ensure navigation drawer can't be opened. Various actions in the drawer cause crashes.
        disableDrawerSwipe()
    }

    override fun onCollectionLoaded(col: Collection) {
        super.onCollectionLoaded(col)
        mCurrentCard = col.getCard(mCardList!![mIndex])
        displayCardQuestion()
        showBackIcon()
    }


    override fun setTitle() {
        supportActionBar!!.setTitle(R.string.preview_title)
    }


    override fun initLayout() {
        super.initLayout()
        mTopBarLayout.visibility = View.GONE
    }


    override fun displayCardQuestion() {
        super.displayCardQuestion()
        mShowingAnswer = false
        updateButtonState()
    }


    // Called via mFlipCardListener in parent class when answer button pressed
    override fun displayCardAnswer() {
        super.displayCardAnswer()
        mShowingAnswer = true
        updateButtonState()
    }


    // we don't want the Activity title to be changed.
    override fun updateScreenCounts() {}


    // No Gestures!
    override fun executeCommand(which: Int) {}

    private fun updateButtonState() {
        // If we are in single-card mode, we show the "Show Answer" button on the question side
        // and hide all the button s on the answer side.
        if (mCardList!!.size == 1) {
            if (!mShowingAnswer) {
                mFlipCardLayout.visibility = View.VISIBLE
            } else {
                findViewById<View>(R.id.answer_options_layout).visibility = View.GONE
                mFlipCardLayout.visibility = View.GONE
                hideEaseButtons()
            }
            return
        }

        mFlipCardLayout.visibility = View.GONE
        mEase1Layout.visibility = View.VISIBLE
        mEase2Layout.visibility = View.VISIBLE
        mEase3Layout.visibility = View.GONE
        mEase4Layout.visibility = View.GONE

        val background = Themes.getResFromAttr(this, intArrayOf(R.attr.hardButtonRef))
        val textColor = Themes.getColorFromAttr(this, intArrayOf(R.attr.hardButtonTextColor))

        mNext1.textSize = 30f
        mEase1.visibility = View.GONE
        mNext1.setTextColor(textColor[0])
        mEase1Layout.setOnClickListener(mSelectScrollHandler)
        mEase1Layout.setBackgroundResource(background[0])

        mNext2.textSize = 30f
        mEase2.visibility = View.GONE
        mNext2.setTextColor(textColor[0])
        mEase2Layout.setOnClickListener(mSelectScrollHandler)
        mEase2Layout.setBackgroundResource(background[0])


        if (mIndex == 0 && mShowingAnswer) {
            mEase1Layout.isEnabled = false
            mNext1.text = "-"
        } else {
            mEase1Layout.isEnabled = true
            mNext1.text = "<"
        }

        if (mIndex == mCardList!!.size - 1 && mShowingAnswer) {
            mEase2Layout.isEnabled = false
            mNext2.text = "-"
        } else {
            mEase2Layout.isEnabled = true
            mNext2.text = ">"
        }
    }

    companion object {
        const val EXTRA_INDEX = "index"
        const val EXTRA_CARDLIST = "cardList"

        fun createIntent(context: Context, positionInCardList: Int, cardIds: LongArray): Intent = Intent(context, Previewer::class.java).apply {
            putExtra(EXTRA_INDEX, positionInCardList)
            putExtra(EXTRA_CARDLIST, cardIds)
        }
    }
}
