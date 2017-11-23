package com.ichi2.anki.dialogs

import android.content.res.Resources
import android.os.Message
import android.support.v4.app.DialogFragment

import com.ichi2.anki.AnkiDroidApp

abstract class AsyncDialogFragment : DialogFragment() {
    /* provide methods for text to show in notification bar when the DialogFragment
       can't be shown due to the host activity being in stopped state.
       This can happen when the DialogFragment is shown from
       the onPostExecute() method of an AsyncTask */

    abstract val notificationMessage: String
    abstract val notificationTitle: String

    open fun getDialogHandlerMessage(): Message? = null

    protected fun res(): Resources = AnkiDroidApp.getAppResources()
} 