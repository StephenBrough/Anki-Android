package com.ichi2.anki.exception

import android.content.res.Resources
import com.ichi2.anki.R

class ConfirmModSchemaException : Exception() {
    companion object {
        // TODO: Is this needed?
        val serialVersionUID = -9215098969154590797L
    }
}

class DeckRenameException(private val mErrorCode: Int) : Exception() {

    fun getLocalizedMessage(res: Resources): String = when (mErrorCode) {
        ALREADY_EXISTS -> res.getString(R.string.decks_rename_exists)
        FILTERED_NOSUBDEKCS -> res.getString(R.string.decks_rename_filtered_nosubdecks)
        else -> ""
    }

    companion object {
        const val ALREADY_EXISTS = 0
        const val FILTERED_NOSUBDEKCS = 1
    }
}

class MediaSyncException(msg: String) : Exception(msg)

class StorageAccessException(msg: String) : Exception(msg)

class UnknownHttpResponseException(message: String, private val mCode: Int) : Exception(message) {
    val responseCode: Int
        get() = mCode
}