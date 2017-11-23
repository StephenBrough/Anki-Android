package com.ichi2.anki.deckpicker

import android.support.design.widget.BaseTransientBottomBar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ichi2.anki.R
import kotlinx.android.synthetic.main.snackbar_progress.view.*

class CustomSnackbar(parent: ViewGroup, content: View, contentViewCallback: ContentViewCallback) : BaseTransientBottomBar<CustomSnackbar>(parent, content, contentViewCallback) {

    fun setText(msg: String): CustomSnackbar {
        view.snackbar_text.text = msg
        return this
    }

    class ContentViewCallback(val content: View) : BaseTransientBottomBar.ContentViewCallback {
        override fun animateContentOut(delay: Int, duration: Int) {
            content.scaleY = 1f
            content.animate().apply {
                scaleY(0f)
                this.duration = duration.toLong()
                this.startDelay = delay.toLong()
            }
        }

        override fun animateContentIn(delay: Int, duration: Int) {
            content.scaleY = 0f
            content.animate().scaleY(1f).apply {
                this.duration = duration.toLong()
                startDelay = delay.toLong()
            }
        }
    }

    companion object {
        fun make(parent: ViewGroup, duration: Int): CustomSnackbar {
            val snackView = LayoutInflater.from(parent.context).inflate(R.layout.snackbar_progress, parent, false)
            val contentViewCallback = ContentViewCallback(snackView)
            val snackbar = CustomSnackbar(parent, snackView, contentViewCallback)
            snackbar.duration = duration
            return snackbar
        }
    }
}