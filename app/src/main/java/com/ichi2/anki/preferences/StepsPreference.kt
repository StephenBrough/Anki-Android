/****************************************************************************************
 * Copyright (c) 2013 Houssam Salem <houssam.salem.au></houssam.salem.au>@gmail.com>                        *
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

package com.ichi2.anki.preferences

import android.content.Context
import android.preference.EditTextPreference
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils

import org.json.JSONArray
import org.json.JSONException

class StepsPreference : EditTextPreference {

    private val mAllowEmpty: Boolean


    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        mAllowEmpty = getAllowEmptyFromAttributes(attrs)
        updateSettings()
    }


    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mAllowEmpty = getAllowEmptyFromAttributes(attrs)
        updateSettings()
    }


    constructor(context: Context) : super(context) {
        mAllowEmpty = getAllowEmptyFromAttributes(null)
        updateSettings()
    }


    /**
     * Update settings to show a numeric keyboard instead of the default keyboard.
     *
     *
     * This method should only be called once from the constructor.
     */
    private fun updateSettings() {
        // Use the number pad but still allow normal text for spaces and decimals.
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_CLASS_TEXT
    }


    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val validated = getValidatedStepsInput(editText.text.toString())
            if (validated == null) {
                UIUtils.showThemedToast(context, context.resources.getString(R.string.steps_error), false)
            } else if (TextUtils.isEmpty(validated) && !mAllowEmpty) {
                UIUtils.showThemedToast(context, context.resources.getString(R.string.steps_min_error),
                        false)
            } else {
                text = validated
            }
        }
    }


    /**
     * Check if the string is a valid format for steps and return that string, reformatted for better usability if
     * needed.
     *
     * @param steps User input in text editor.
     * @return The correctly formatted string or null if the input is not valid.
     */
    private fun getValidatedStepsInput(steps: String): String? {
        val ja = convertToJSON(steps)
        if (ja == null) {
            return null
        } else {
            val sb = StringBuilder()
            try {
                for (i in 0..ja.length() - 1) {
                    sb.append(ja.getString(i)).append(" ")
                }
                return sb.toString().trim { it <= ' ' }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

        }
    }


    private fun getAllowEmptyFromAttributes(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeBooleanValue(AnkiDroidApp.XML_CUSTOM_NAMESPACE, "allowEmpty", true) ?: true
    }

    companion object {


        /**
         * Convert steps format.
         *
         * @param a JSONArray representation of steps.
         * @return The steps as a space-separated string.
         */
        fun convertFromJSON(a: JSONArray): String {
            val sb = StringBuilder()
            try {
                for (i in 0..a.length() - 1) {
                    sb.append(a.getString(i)).append(" ")
                }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            return sb.toString().trim { it <= ' ' }
        }


        /**
         * Convert steps format. For better usability, rounded floats are converted to integers (e.g., 1.0 is converted to
         * 1).
         *
         * @param steps String representation of steps.
         * @return The steps as a JSONArray or null if the steps are not valid.
         */
        fun convertToJSON(steps: String): JSONArray? {
            var steps = steps
            val ja = JSONArray()
            steps = steps.trim { it <= ' ' }
            if (TextUtils.isEmpty(steps)) {
                return ja
            }
            try {
                for (s in steps.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    val f = java.lang.Float.parseFloat(s)
                    // 0 or less is not a valid step.
                    if (f <= 0) {
                        return null
                    }
                    // Use whole numbers if we can (but still allow decimals)
                    val i = f.toInt()
                    if (i.toFloat() == f) {
                        ja.put(i)
                    } else {
                        ja.put(f.toDouble())
                    }
                }
            } catch (e: NumberFormatException) {
                return null
            } catch (e: JSONException) {
                // Can't serialize float. Value likely too big/small.
                return null
            }

            return ja
        }
    }
}
