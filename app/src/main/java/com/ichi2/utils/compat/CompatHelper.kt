/***************************************************************************************
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
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

package com.ichi2.utils.compat


import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.view.KeyCharacterMap

class CompatHelper private constructor() {
    private var mCompat: Compat? = null

    private val isNookHdOrHdPlus: Boolean
        get() = isNookHd || isNookHdPlus

    private val isNookHdPlus: Boolean
        get() = android.os.Build.BRAND == "NOOK" && android.os.Build.PRODUCT == "HDplus"
                && android.os.Build.DEVICE == "ovation"

    private val isNookHd: Boolean
        get() = android.os.Build.MODEL.equals("bntv400", ignoreCase = true) && android.os.Build.BRAND == "NOOK"


    init {

        if (isNookHdOrHdPlus && sdkVersion == 15) {
            mCompat = CompatV15NookHdOrHdPlus()
        } else if (sdkVersion >= 21) {
            mCompat = CompatV21()
        } else if (sdkVersion >= 19) {
            mCompat = CompatV19()
        } else if (sdkVersion >= 17) {
            mCompat = CompatV17()
        } else if (sdkVersion >= 16) {
            mCompat = CompatV16()
        } else if (sdkVersion >= 15) {
            mCompat = CompatV15()
        } else if (sdkVersion >= 11) {
            mCompat = CompatV11()
        } else if (sdkVersion >= 12) {
            mCompat = CompatV12()
        } else {
            mCompat = CompatV10()
        }
    }

    companion object {
        private var sInstance: CompatHelper? = null

        /** Get the current Android API level.  */
        val sdkVersion: Int
            get() = Build.VERSION.SDK_INT


        /** Determine if the device is running API level 11 or higher.  */
        val isHoneycomb: Boolean
            get() = sdkVersion >= Build.VERSION_CODES.HONEYCOMB
        /** Determine if the device is running API level 21 or higher.  */
        val isLollipop: Boolean
            get() = sdkVersion >= Build.VERSION_CODES.LOLLIPOP
        /** Determine if the device is running API level 23 or higher.  */
        val isMarshmallow: Boolean
            get() = sdkVersion >= Build.VERSION_CODES.M

        /**
         * Main public method to get the compatibility class
         */
        val compat: Compat
            get() = instance.mCompat!!

        val instance: CompatHelper
            @Synchronized get() {
                if (sInstance == null) {
                    sInstance = CompatHelper()
                }
                return sInstance!!
            }


        val isNook: Boolean
            get() = android.os.Build.MODEL.equals("nook", ignoreCase = true) || android.os.Build.DEVICE.equals("nook", ignoreCase = true)


        val isChromebook: Boolean
            get() = android.os.Build.BRAND.equals("chromium", ignoreCase = true) || android.os.Build.MANUFACTURER.equals("chromium", ignoreCase = true)

        val isKindle: Boolean
            get() = Build.BRAND.equals("amazon", ignoreCase = true) || Build.MANUFACTURER.equals("amazon", ignoreCase = true)

        fun hasKanaAndEmojiKeys(): Boolean =
                KeyCharacterMap.deviceHasKey(94) && KeyCharacterMap.deviceHasKey(95)

        fun hasScrollKeys(): Boolean =
                KeyCharacterMap.deviceHasKey(92) || KeyCharacterMap.deviceHasKey(93)

        fun removeHiddenPreferences(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (isHoneycomb) {
                preferences.edit().remove("longclickWorkaround").apply()
            }
            if (sdkVersion >= 13) {
                preferences.edit().remove("safeDisplay").apply()
            }
            if (sdkVersion >= 15) {
                preferences.edit().remove("inputWorkaround").apply()
            }
            if (sdkVersion >= 16) {
                preferences.edit().remove("fixHebrewText").apply()
            }
        }
    }
}
