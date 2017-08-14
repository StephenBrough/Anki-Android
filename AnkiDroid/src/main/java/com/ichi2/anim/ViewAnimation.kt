package com.ichi2.anim

import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object ViewAnimation {

//    val SLIDE_IN_FROM_RIGHT = 0
//    val SLIDE_OUT_TO_RIGHT = 1
//    val SLIDE_IN_FROM_LEFT = 2
//    val SLIDE_OUT_TO_LEFT = 3
//    val SLIDE_IN_FROM_BOTTOM = 4
//    val SLIDE_IN_FROM_TOP = 5

    val FADE_IN = 0
    val FADE_OUT = 1


//    fun slide(type: Int, duration: Int, offset: Int): Animation {
//        val animation: Animation?
//        when (type) {
//            SLIDE_IN_FROM_RIGHT -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, +1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = DecelerateInterpolator()
//            }
//            SLIDE_OUT_TO_RIGHT -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, +1.0f,
//                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = AccelerateInterpolator()
//            }
//            SLIDE_IN_FROM_LEFT -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = DecelerateInterpolator()
//            }
//            SLIDE_OUT_TO_LEFT -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
//                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = AccelerateInterpolator()
//            }
//            SLIDE_IN_FROM_BOTTOM -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                        Animation.RELATIVE_TO_SELF, +1.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = DecelerateInterpolator()
//            }
//            SLIDE_IN_FROM_TOP -> {
//                animation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f)
//                animation.interpolator = DecelerateInterpolator()
//            }
//            else -> animation = null
//        }
//        animation!!.duration = duration.toLong()
//        animation.startOffset = offset.toLong()
//        return animation
//    }


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
