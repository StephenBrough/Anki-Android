package com.ichi2.utils.anim

import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object ViewAnimation {

    val FADE_IN = 0
    val FADE_OUT = 1

    fun fade(type: Int, duration: Int, offset: Int): Animation {
        val startValue = type.toFloat()
        val animation = AlphaAnimation(startValue, 1.0f - startValue)
        animation.duration = duration.toLong()
        if (type == FADE_IN) {
            animation.zAdjustment = Animation.ZORDER_TOP
        }
        animation.startOffset = offset.toLong()
        return animation
    }
}
