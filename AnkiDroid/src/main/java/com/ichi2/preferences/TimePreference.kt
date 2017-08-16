package com.ichi2.preferences

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker


class TimePreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var timePicker: TimePicker? = null
    private var hours: Int = 0
    private var minutes: Int = 0

    init {

        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    override fun onCreateDialogView(): View {
        timePicker = TimePicker(context)

        timePicker!!.setIs24HourView(true)

        return timePicker as TimePicker
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        val time: String

        if (restorePersistedValue) {
            if (null == defaultValue) {
                time = getPersistedString(DEFAULT_VALUE)
            } else {
                time = getPersistedString(defaultValue.toString())
            }
        } else {
            time = defaultValue!!.toString()
        }

        hours = parseHours(time)
        minutes = parseMinutes(time)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        timePicker!!.currentHour = hours
        timePicker!!.currentMinute = minutes
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (positiveResult) {
            hours = timePicker!!.currentHour
            minutes = timePicker!!.currentMinute

            val time = String.format("%1$02d:%2$02d", hours, minutes)

            if (callChangeListener(time)) {
                persistString(time)
            }
        }
    }

    companion object {
        val DEFAULT_VALUE = "00:00"

        fun parseHours(time: String): Int {
            return Integer.parseInt(time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        }

        fun parseMinutes(time: String): Int {
            return Integer.parseInt(time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
        }
    }
}
