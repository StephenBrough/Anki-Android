/****************************************************************************************
 * Copyright (c) 2015 Houssam Salem <houssam.salem.au></houssam.salem.au>@gmail.com>                        *
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

package com.ichi2.anki.junkdrawer.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import com.ichi2.anki.R
import com.ichi2.utils.compat.CompatHelper
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Sched

import java.util.ArrayList

class DeckAdapter(private val mLayoutInflater: LayoutInflater, context: Context) : RecyclerView.Adapter<DeckAdapter.ViewHolder>() {
    private val mDeckList: MutableList<Sched.DeckDueTreeNode>
    private val mZeroCountColor: Int
    private val mNewCountColor: Int
    private val mLearnCountColor: Int
    private val mReviewCountColor: Int
    private val mRowCurrentDrawable: Int
    private val mDeckNameDefaultColor: Int
    private val mDeckNameDynColor: Int
    private val mExpandImage: Drawable?
    private val mCollapseImage: Drawable?
    private val mNoExpander = ColorDrawable(Color.TRANSPARENT)

    // Listeners
    private var mDeckClickListener: View.OnClickListener? = null
    private var mDeckExpanderClickListener: View.OnClickListener? = null
    private var mDeckLongClickListener: View.OnLongClickListener? = null
    private var mCountsClickListener: View.OnClickListener? = null

    private var mCol: Collection? = null

    // Totals accumulated as each deck is processed
    private var mNew: Int = 0
    private var mLrn: Int = 0
    private var mRev: Int = 0

    // Flags
    private var mHasSubdecks: Boolean = false


    val eta: Int
        get() = mCol!!.sched.eta(intArrayOf(mNew, mLrn, mRev))

    val due: Int
        get() = mNew + mLrn + mRev

    val deckList: List<Sched.DeckDueTreeNode>
        get() = mDeckList

    // ViewHolder class to save inflated views for recycling
    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var deckLayout: RelativeLayout
        var countsLayout: LinearLayout
        var deckExpander: ImageButton
        var indentView: ImageButton
        var deckName: TextView
        var deckNew: TextView
        var deckLearn: TextView
        var deckRev: TextView

        init {
            deckLayout = v.findViewById<View>(R.id.DeckPickerHoriz) as RelativeLayout
            countsLayout = v.findViewById<View>(R.id.counts_layout) as LinearLayout
            deckExpander = v.findViewById<View>(R.id.deckpicker_expander) as ImageButton
            indentView = v.findViewById<View>(R.id.deckpicker_indent) as ImageButton
            deckName = v.findViewById<View>(R.id.deckpicker_name) as TextView
            deckNew = v.findViewById<View>(R.id.deckpicker_new) as TextView
            deckLearn = v.findViewById<View>(R.id.deckpicker_lrn) as TextView
            deckRev = v.findViewById<View>(R.id.deckpicker_rev) as TextView
        }
    }

    init {
        mDeckList = ArrayList<Sched.DeckDueTreeNode>()
        // Get the colors from the theme attributes
        val attrs = intArrayOf(R.attr.zeroCountColor,
                R.attr.newCountColor,
                R.attr.learnCountColor,
                R.attr.reviewCountColor,
                R.attr.currentDeckBackground,
                android.R.attr.textColor,
                R.attr.dynDeckColor,
                R.attr.expandRef,
                R.attr.collapseRef)
        val ta = context.obtainStyledAttributes(attrs)
        mZeroCountColor = ta.getColor(0, ContextCompat.getColor(context, R.color.black))
        mNewCountColor = ta.getColor(1, ContextCompat.getColor(context, R.color.black))
        mLearnCountColor = ta.getColor(2, ContextCompat.getColor(context, R.color.black))
        mReviewCountColor = ta.getColor(3, ContextCompat.getColor(context, R.color.black))
        mRowCurrentDrawable = ta.getResourceId(4, 0)
        mDeckNameDefaultColor = ta.getColor(5, ContextCompat.getColor(context, R.color.black))
        mDeckNameDynColor = ta.getColor(6, ContextCompat.getColor(context, R.color.material_blue_A700))
        mExpandImage = ta.getDrawable(7)
        mCollapseImage = ta.getDrawable(8)
        ta.recycle()
    }

    fun setDeckClickListener(listener: View.OnClickListener) {
        mDeckClickListener = listener
    }

    fun setCountsClickListener(listener: View.OnClickListener) {
        mCountsClickListener = listener
    }

    fun setDeckExpanderClickListener(listener: View.OnClickListener) {
        mDeckExpanderClickListener = listener
    }

    fun setDeckLongClickListener(listener: View.OnLongClickListener) {
        mDeckLongClickListener = listener
    }


    /**
     * Consume a list of [Sched.DeckDueTreeNode]s to render a new deck list.
     */
    fun buildDeckList(nodes: List<Sched.DeckDueTreeNode>, col: Collection) {
        mCol = col
        mDeckList.clear()
        mRev = 0
        mLrn = mRev
        mNew = mLrn
        mHasSubdecks = false
        processNodes(nodes)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckAdapter.ViewHolder {
        val v = mLayoutInflater.inflate(R.layout.deck_item, parent, false)
        return ViewHolder(v)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Update views for this node
        val node = mDeckList[position]
        // Set the expander icon and padding according to whether or not there are any subdecks
        val deckLayout = holder.deckLayout
        val rightPadding = deckLayout.resources.getDimension(R.dimen.deck_picker_right_padding).toInt()
        if (mHasSubdecks) {
            val smallPadding = deckLayout.resources.getDimension(R.dimen.deck_picker_left_padding_small).toInt()
            deckLayout.setPadding(smallPadding, 0, rightPadding, 0)
            holder.deckExpander.visibility = View.VISIBLE
            // Create the correct expander for this deck
            setDeckExpander(holder.deckExpander, holder.indentView, node)
        } else {
            holder.deckExpander.visibility = View.GONE
            val normalPadding = deckLayout.resources.getDimension(R.dimen.deck_picker_left_padding).toInt()
            deckLayout.setPadding(normalPadding, 0, rightPadding, 0)
        }

        if (node.children.size > 0) {
            holder.deckExpander.tag = node.did
            holder.deckExpander.setOnClickListener(mDeckExpanderClickListener)
        } else {
            holder.deckExpander.setOnClickListener(null)
        }
        holder.deckLayout.setBackgroundResource(mRowCurrentDrawable)
        // Set background colour. The current deck has its own color
        if (node.did == mCol!!.decks.current().optLong("id")) {
            holder.deckLayout.setBackgroundResource(mRowCurrentDrawable)
        } else {
            CompatHelper.compat.setSelectableBackground(holder.deckLayout)
        }
        // Set deck name and colour. Filtered decks have their own colour
        holder.deckName.text = node.names[0]
        if (mCol!!.decks.isDyn(node.did)) {
            holder.deckName.setTextColor(mDeckNameDynColor)
        } else {
            holder.deckName.setTextColor(mDeckNameDefaultColor)
        }

        // Set the card counts and their colors
        holder.deckNew.text = node.newCount.toString()
        holder.deckNew.setTextColor(if (node.newCount == 0) mZeroCountColor else mNewCountColor)
        holder.deckLearn.text = node.lrnCount.toString()
        holder.deckLearn.setTextColor(if (node.lrnCount == 0) mZeroCountColor else mLearnCountColor)
        holder.deckRev.text = node.revCount.toString()
        holder.deckRev.setTextColor(if (node.revCount == 0) mZeroCountColor else mReviewCountColor)

        // Store deck ID in layout's tag for easy retrieval in our click listeners
        holder.deckLayout.tag = node.did
        holder.countsLayout.tag = node.did

        // Set click listeners
        holder.deckLayout.setOnClickListener(mDeckClickListener)
        holder.deckLayout.setOnLongClickListener(mDeckLongClickListener)
        holder.countsLayout.setOnClickListener(mCountsClickListener)
    }

    override fun getItemCount(): Int = mDeckList.size


    private fun setDeckExpander(expander: ImageButton, indent: ImageButton, node: Sched.DeckDueTreeNode) {
        val collapsed = mCol!!.decks.get(node.did).optBoolean("collapsed", false)
        // Apply the correct expand/collapse drawable
        if (collapsed) {
            expander.setImageDrawable(mExpandImage)
            expander.contentDescription = expander.context.getString(R.string.expand)
        } else if (node.children.size > 0) {
            expander.setImageDrawable(mCollapseImage)
            expander.contentDescription = expander.context.getString(R.string.collapse)
        } else {
            expander.setImageDrawable(mNoExpander)
        }
        // Add some indenting for each nested level
        val width = indent.resources.getDimension(R.dimen.keyline_1).toInt() * node.depth
        indent.minimumWidth = width
    }


    private fun processNodes(nodes: List<Sched.DeckDueTreeNode>, depth: Int = 0) {
        for (node in nodes) {
            // If the default deck is empty, hide it by not adding it to the deck list.
            // We don't hide it if it's the only deck or if it has sub-decks.
            if (node.did == 1L && nodes.size > 1 && node.children.size == 0) {
                if (mCol!!.db.queryScalar("select 1 from cards where did = 1") == 0) {
                    continue
                }
            }
            // If any of this node's parents are collapsed, don't add it to the deck list
            for (parent in mCol!!.decks.parents(node.did)) {
                mHasSubdecks = true    // If a deck has a parent it means it's a subdeck so set a flag
                if (parent.optBoolean("collapsed")) {
                    return
                }
            }
            mDeckList.add(node)
            // Keep track of the depth. It's used to determine visual properties like indenting later
            node.depth = depth

            // Add this node's counts to the totals if it's a parent deck
            if (depth == 0) {
                mNew += node.newCount
                mLrn += node.lrnCount
                mRev += node.revCount
            }
            // Process sub-decks
            processNodes(node.children, depth + 1)
        }
    }


    /**
     * Return the position of the deck in the deck list. If the deck is a child of a collapsed deck
     * (i.e., not visible in the deck list), then the position of the parent deck is returned instead.
     *
     * An invalid deck ID will return position 0.
     */
    fun findDeckPosition(did: Long): Int {
        mDeckList.indices
                .filter { mDeckList[it].did == did }
                .forEach { return it }
        // If the deck is not in our list, we search again using the immediate parent
        val parents = mCol!!.decks.parents(did)
        return if (parents.size == 0) {
            0
        } else {
            findDeckPosition(parents[parents.size - 1].optLong("id", 0))
        }
    }
}