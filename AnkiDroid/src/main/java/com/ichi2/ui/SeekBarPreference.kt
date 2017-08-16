/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * adjusted by Norbert Nagold 2011 <norbert.nagold@gmail.com>
 */

package com.ichi2.ui

import android.app.AlertDialog
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

import com.ichi2.anki.AnkiDroidApp

class SeekBarPreference(private val mContext: Context, attrs: AttributeSet) : DialogPreference(mContext, attrs), SeekBar.OnSeekBarChangeListener {

    private var mSeekBar: SeekBar? = null
    private var mValueText: TextView? = null

    private val mSuffix: String?
    private val mDefault: Int
    private val mMax: Int
    private val mMin: Int
    private val mInterval: Int
    private var mValue = 0


    var value: Int
        get() = if (mValue == 0) {
            getPersistedInt(mDefault)
        } else {
            mValue
        }
        set(value) {
            mValue = value
            persistInt(value)
        }


    init {

        mSuffix = attrs.getAttributeValue(androidns, "text")
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0)
        mMax = attrs.getAttributeIntValue(androidns, "max", 100)
        mMin = attrs.getAttributeIntValue(AnkiDroidApp.XML_CUSTOM_NAMESPACE, "min", 0)
        mInterval = attrs.getAttributeIntValue(AnkiDroidApp.XML_CUSTOM_NAMESPACE, "interval", 1)
    }


    override fun onCreateDialogView(): View {
        val params: LinearLayout.LayoutParams
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(6, 6, 6, 6)

        mValueText = TextView(mContext)
        mValueText!!.gravity = Gravity.CENTER_HORIZONTAL
        mValueText!!.textSize = 32f
        params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        layout.addView(mValueText, params)

        mSeekBar = SeekBar(mContext)
        mSeekBar!!.setOnSeekBarChangeListener(this)

        layout.addView(mSeekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))

        if (shouldPersist()) {
            mValue = getPersistedInt(mDefault)
        }

        mSeekBar!!.max = (mMax - mMin) / mInterval
        mSeekBar!!.progress = (mValue - mMin) / mInterval

        val t = mValue.toString()
        mValueText!!.text = if (mSuffix == null) t else t + mSuffix
        return layout
    }


    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        mSeekBar!!.max = (mMax - mMin) / mInterval
        mSeekBar!!.progress = (mValue - mMin) / mInterval
    }


    override fun onSetInitialValue(restore: Boolean, defaultValue: Any) {
        super.onSetInitialValue(restore, defaultValue)
        mValue = getPersistedInt(mDefault)
        if (restore) {
            mValue = if (shouldPersist()) getPersistedInt(mDefault) else 0
        } else {
            mValue = defaultValue as Int
        }
    }


    override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
        if (fromTouch) {
            mValue = value * mInterval + mMin
            val t = mValue.toString()
            mValueText!!.text = if (mSuffix == null) t else t + mSuffix
        }
    }

    override fun onStartTrackingTouch(seek: SeekBar) {}


    override fun onStopTrackingTouch(seek: SeekBar) {
        if (shouldPersist()) {
            persistInt(mValue)
        }
        callChangeListener(mValue)
        this.dialog.dismiss()
    }


    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setNegativeButton(null, null)
        builder.setPositiveButton(null, null)
        builder.setTitle(null)
    }

    companion object {
        private val androidns = "http://schemas.android.com/apk/res/android"
    }
}
