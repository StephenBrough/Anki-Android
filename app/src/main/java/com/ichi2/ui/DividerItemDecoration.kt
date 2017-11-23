package com.ichi2.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View

import com.ichi2.anki.R

/**
 * Copied and modified from http://stackoverflow.com/questions/24618829/27037230#27037230
 */

class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val mDivider: Drawable?

    init {
        val attrs = intArrayOf(R.attr.deckDivider)
        val ta = context.obtainStyledAttributes(attrs)
        mDivider = ta.getDrawable(0)
        ta.recycle()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + mDivider!!.intrinsicHeight

            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }
    }
}