package com.ichi2.anki

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Typeface

import android.widget.Toast

import com.ichi2.libanki.Utils

import java.io.File
import java.util.ArrayList
import java.util.HashSet
import java.util.Locale

import timber.log.Timber

class AnkiFont private constructor(val name: String, private val mFamily: String, private val mAttributes: List<String>, val path: String) {
    private var mIsDefault: Boolean? = null
    private var mIsOverride: Boolean? = null


    val declaration: String
        get() = "@font-face {" + getCSS(false) + " src: url(\"file://" + path + "\");}"


    init {
        mIsDefault = false
        mIsOverride = false
    }


    fun getCSS(override: Boolean): String {
        val sb = StringBuilder("font-family: \"").append(mFamily)
        if (override) {
            sb.append("\" !important;")
        } else {
            sb.append("\";")
        }
        for (attr in mAttributes) {
            sb.append(" ").append(attr)
            if (override) {
                if (sb[sb.length - 1] == ';') {
                    sb.deleteCharAt(sb.length - 1)
                    sb.append(" !important;")
                } else {
                    Timber.d("AnkiFont.getCSS() - unable to set a font attribute important while override is set.")
                }
            }
        }
        return sb.toString()
    }


    private fun setAsDefault() {
        mIsDefault = true
        mIsOverride = false
    }


    private fun setAsOverride() {
        mIsOverride = true
        mIsDefault = false
    }

    companion object {
        private val fAssetPathPrefix = "/android_asset/fonts/"
        private val corruptFonts = HashSet<String>()


        /**
         * Factory for AnkiFont creation. Creates a typeface wrapper from a font file representing.
         *
         * @param ctx Activity context, needed to access assets
         * @param path Path to typeface file, needed when this is a custom font.
         * @param fromAssets True if the font is to be found in assets of application
         * @return A new AnkiFont object or null if the file can't be interpreted as typeface.
         */
        fun createAnkiFont(ctx: Context, path: String, fromAssets: Boolean): AnkiFont? {
            var path = path
            val fontfile = File(path)
            val name = Utils.splitFilename(fontfile.name)[0]
            var family = name
            val attributes = ArrayList<String>()

            if (fromAssets) {
                path = fAssetPathPrefix + fontfile.name
            }
            val tf = getTypeface(ctx, path) ?: // unable to create typeface
                    return null

            if (tf.isBold || name.toLowerCase(Locale.US).contains("bold")) {
                attributes.add("font-weight: bolder;")
                family = family.replaceFirst("(?i)-?Bold".toRegex(), "")
            } else if (name.toLowerCase(Locale.US).contains("light")) {
                attributes.add("font-weight: lighter;")
                family = family.replaceFirst("(?i)-?Light".toRegex(), "")
            } else {
                attributes.add("font-weight: normal;")
            }
            if (tf.isItalic || name.toLowerCase(Locale.US).contains("italic")) {
                attributes.add("font-style: italic;")
                family = family.replaceFirst("(?i)-?Italic".toRegex(), "")
            } else if (name.toLowerCase(Locale.US).contains("oblique")) {
                attributes.add("font-style: oblique;")
                family = family.replaceFirst("(?i)-?Oblique".toRegex(), "")
            } else {
                attributes.add("font-style: normal;")
            }
            if (name.toLowerCase(Locale.US).contains("condensed") || name.toLowerCase(Locale.US).contains("narrow")) {
                attributes.add("font-stretch: condensed;")
                family = family.replaceFirst("(?i)-?Condensed".toRegex(), "")
                family = family.replaceFirst("(?i)-?Narrow(er)?".toRegex(), "")
            } else if (name.toLowerCase(Locale.US).contains("expanded") || name.toLowerCase(Locale.US).contains("wide")) {
                attributes.add("font-stretch: expanded;")
                family = family.replaceFirst("(?i)-?Expanded".toRegex(), "")
                family = family.replaceFirst("(?i)-?Wide(r)?".toRegex(), "")
            }

            val createdFont = AnkiFont(name, family, attributes, path)

            // determine if override font or default font
            val preferences = AnkiDroidApp.getSharedPrefs(ctx)
            val defaultFont = preferences.getString("defaultFont", "")
            val overrideFont = preferences.getString("overrideFontBehavior", "0") == "1"
            if (defaultFont!!.equals(name, ignoreCase = true)) {
                if (overrideFont) {
                    createdFont.setAsOverride()
                } else {
                    createdFont.setAsDefault()
                }
            }
            return createdFont
        }


        fun getTypeface(ctx: Context, path: String): Typeface? {
            try {
                return if (path.startsWith(fAssetPathPrefix)) {
                    Typeface.createFromAsset(ctx.assets, path.replaceFirst("/android_asset/".toRegex(), ""))
                } else {
                    Typeface.createFromFile(path)
                }
            } catch (e: RuntimeException) {
                Timber.w(e, "Runtime error in getTypeface for File: %s", path)
                if (!corruptFonts.contains(path)) {
                    // Show warning toast
                    val name = File(path).name
                    val res = AnkiDroidApp.getAppResources()
                    val toast = Toast.makeText(ctx, res.getString(R.string.corrupt_font, name), Toast.LENGTH_LONG)
                    toast.show()
                    // Don't warn again in this session
                    corruptFonts.add(path)
                }
                return null
            }

        }
    }
}
