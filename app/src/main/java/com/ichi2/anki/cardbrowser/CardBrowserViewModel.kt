package com.ichi2.anki.cardbrowser

import android.arch.lifecycle.ViewModel
import java.util.ArrayList

class CardBrowserViewModel : ViewModel() {
    private var mCards: List<Map<String, String>>? = null

    private val cards: MutableList<Map<String, String>>
        get() {
            if (mCards == null) {
                mCards = ArrayList()
            }
            return mCards as MutableList<Map<String, String>>
        }

    private val cardIds: LongArray
        get() {
            val l = LongArray(mCards!!.size)
            for (i in mCards!!.indices) {
                l[i] = java.lang.Long.parseLong(mCards!![i]["id"])
            }
            return l
        }

}