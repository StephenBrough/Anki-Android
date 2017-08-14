package com.ichi2.anim

import android.annotation.TargetApi
import android.app.Activity

import com.ichi2.anki.R

object ActivityTransitionAnimation {
    const val LEFT = 0
    const val RIGHT = 1
    const val FADE = 2
    const val UP = 3
    const val DOWN = 4
    const val DIALOG_EXIT = 5
    const val NONE = 6


    @TargetApi(5)
    fun slide(activity: Activity, direction: Int) {
        when (direction) {
            LEFT -> activity.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out)
            RIGHT -> activity.overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out)
            FADE -> activity.overridePendingTransition(R.anim.fade_out, R.anim.fade_in)
            UP -> activity.overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out)
            DOWN -> {
                // this is the default animation, we shouldn't try to override it
            }
            DIALOG_EXIT -> activity.overridePendingTransition(R.anim.none, R.anim.dialog_exit)
            NONE -> activity.overridePendingTransition(R.anim.none, R.anim.none)
        }
    }
}
