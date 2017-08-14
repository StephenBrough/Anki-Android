package com.ichi2.anki

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView

import com.ichi2.themes.Themes


class FieldEditText : EditText {

    var name: String? = null
        private set
    var ord: Int = 0
        private set
    private var mOrigBackground: Drawable? = null


    val label: TextView
        get() {
            val label = TextView(this.context)
            label.text = name
            return label
        }


    constructor(context: Context) : super(context) {}


    constructor(context: Context, attr: AttributeSet) : super(context, attr) {}


    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}


    constructor(context: Context, ord: Int, name: String, content: String) : super(context) {
        init(ord, name, content)
    }


    fun init(ord: Int, name: String, content: String?) {
        var content = content
        this.ord = ord
        this.name = name

        if (content == null) {
            content = ""
        } else {
            content = content.replace("<br(\\s*\\/*)>".toRegex(), NEW_LINE)
        }
        setText(content)
        contentDescription = name
        minimumWidth = 400
        mOrigBackground = background
        // Fixes bug where new instances of this object have wrong colors, probably
        // from some reuse mechanic in Android.
        setDefaultStyle()
    }

    /**
     * Modify the style of this view to represent a duplicate field.
     */
    fun setDupeStyle() {
        setBackgroundColor(Themes.getColorFromAttr(context, R.attr.duplicateColor))
    }


    /**
     * Restore the default style of this view.
     */
    fun setDefaultStyle() {
        setBackgroundDrawable(mOrigBackground)
    }

    companion object {

        val NEW_LINE = System.getProperty("line.separator")
        val NL_MARK = "newLineMark"
    }
}
