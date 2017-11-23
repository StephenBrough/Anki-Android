/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha></bibekshrestha>@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial></qutorial>@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul></nicolas.raoul>@gmail.com>                           *
 * Copyright (c) 2013 Flavio Lerda <flerda></flerda>@gmail.com>                                   *
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

package com.ichi2.anki.multimediacard.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.ichi2.anki.R
import com.ichi2.anki.multimediacard.beolingus.parsing.BeolingusParser
import com.ichi2.anki.multimediacard.language.LanguageListerBeolingus
import com.ichi2.anki.junkdrawer.web.HttpFetcher
import com.ichi2.utils.async.Connection
import com.ichi2.utils.toast
import com.ichi2.utils.toastLong
import kotlinx.android.synthetic.main.activity_load_pronounciation.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

/**
 * Activity to load pronunciation files from Beolingus.
 *
 *
 * User picks a source language and the source is passed as extra.
 *
 *
 * When activity finished, it passes the filepath as another extra to the caller.
 */
class LoadPronounciationActivity : AppCompatActivity(), OnCancelListener {

    private var progressDialog: ProgressDialog? = null

    private var mTranslation: String? = null
    private var mTranslationAddress: String? = null
    private var mPronunciationAddress: String? = null
    private var mSource: String? = null
    private var mPronunciationPage: String? = null
    private var mMp3Address: String? = null

    private var mActivity: LoadPronounciationActivity? = null
    private var mLanguageLister: LanguageListerBeolingus? = null
    lateinit private var mSpinnerFrom: Spinner
    private var mSaveButton: Button? = null

    private var mStopped: Boolean = false

    private var downloadFileJob: Job? = null
    private var backgroundPostJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(savedInstanceState?.getBoolean(BUNDLE_KEY_SHUT_OFF, false) == true) {
            finishCancel()
            return
        }

        setContentView(R.layout.activity_load_pronounciation)
        mSource = intent.extras!!.getString(EXTRA_SOURCE)

        mLanguageLister = LanguageListerBeolingus()

        mSpinnerFrom = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                mLanguageLister!!.languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mSpinnerFrom.adapter = adapter
        layoutInLoadPronActivity.addView(mSpinnerFrom)

        val buttonLoadPronunciation = Button(this)
        buttonLoadPronunciation.text = getText(R.string.multimedia_editor_pron_load)
        layoutInLoadPronActivity.addView(buttonLoadPronunciation)
        buttonLoadPronunciation.setOnClickListener { v -> onLoadPronunciation() }

        mSaveButton = Button(this)
        mSaveButton!!.text = resources.getString(R.string.multimedia_editor_pron_save)
        mSaveButton!!.setOnClickListener { }
        mActivity = this

        mStopped = false

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_load_pronounciation, menu)
        return true
    }

    private fun onLoadPronunciation() {
        if (!Connection.isOnline()) {
            toast(getText(R.string.network_no_connection))
            return
        }

        val message = getText(R.string.multimedia_editor_searching_word)

        showProgressDialog(message)

        mTranslationAddress = computeAddressOfTranslationPage()

        startBackgroundPostJob(mTranslationAddress!!)

    }

    private fun startBackgroundPostJob(address: String) {
        backgroundPostJob = launch(UI) {
            // TMP CODE for quick testing
            // if (mAddress.contentEquals(mTranslationAddress))
            // {
            // return MockTranslationFetcher.get();
            // }
            // else if (mAddress.contentEquals(mPronunciationAddress))
            // {
            // return MockPronounciationPageFetcher.get();
            // }

            //doInBackground
            // Result here is the whole HTML of the page
            // this is passed to ask for address and differentiate, which of the
            // post has finished.
            val resultString = HttpFetcher.fetchThroughHttp(address, "ISO-8859-1")

            //onPostExecute
            processPostFinished(address, resultString)

            // if something went wrong...
            // progressDialog!!.dismiss()
        }
    }


    private fun showProgressDialog(message: CharSequence) {

        dismissCarefullyProgressDialog()

        progressDialog = ProgressDialog.show(this, getText(R.string.multimedia_editor_progress_wait_title), message, true,
                false)
        progressDialog!!.setCancelable(true)
        progressDialog!!.setOnCancelListener(this)
    }

    private fun processPostFinished(address: String, result: String) {

        if (mStopped) {
            return
        }

        // First call returned
        // Means we get the page with the word translation,
        // And we have to start fetching the page with pronunciation
        if (address.contentEquals(mTranslationAddress!!)) {
            mTranslation = result

            if (mTranslation!!.startsWith("FAILED")) {

                failNoPronunciation()

                return
            }

            mPronunciationAddress = BeolingusParser.getPronounciationAddressFromTranslation(mTranslation!!, mSource!!)

            if (mPronunciationAddress!!.contentEquals("no")) {

                failNoPronunciation()

                if (!mSource!!.toLowerCase(Locale.getDefault()).contentEquals(mSource!!)) {
                    toastLong(getText(R.string.multimedia_editor_word_search_try_lower_case))
                }

                return
            }

            showProgressDialog(getText(R.string.multimedia_editor_pron_looking_up))
            startBackgroundPostJob(mPronunciationAddress!!)

            return
        }

        // Else
        // second call returned
        // This is a call when pronunciation page has been fetched.
        // We chekc if mp3 file could be downloaded and download it.
        if (address.contentEquals(mPronunciationAddress!!)) {
            // else here = pronunciation post returned;

            mPronunciationPage = result

            mMp3Address = BeolingusParser.getMp3AddressFromPronounciation(mPronunciationPage!!)

            if (mMp3Address!!.contentEquals("no")) {
                failNoPronunciation()
                return
            }

            // Download MP3 file
            showProgressDialog(getText(R.string.multimedia_editor_general_downloading))
            downloadFileJob = launch(UI) {
                val mAddress = mMp3Address

                //doInBackground
                val downloadResult = com.ichi2.anki.junkdrawer.web.HttpFetcher.downloadFileToSdCard(mAddress!!, mActivity!!, "pronunc")

                // If something went wrong...
//                    progressDialog!!.dismiss()

                //onPostExecute
                receiveMp3File(downloadResult)
            }

            return
        }
    }

    // This is called when MP3 Download is finished.
    fun receiveMp3File(result: String?) {

        if (result == null || result.startsWith("FAIL")) {
            failNoPronunciation()
            return
        }

        progressDialog!!.dismiss()

        toast(getText(R.string.multimedia_editor_general_done))

        val resultData = Intent().apply {
            putExtra(EXTRA_PRONUNCIATION_FILE_PATH, result)
        }
        setResult(Activity.RESULT_OK, resultData)

        finish();
    }

    private fun finishCancel() {
        val resultData = Intent()
        setResult(Activity.RESULT_CANCELED, resultData)
        finish();
    }

    private fun failNoPronunciation() {
        stop(getText(R.string.multimedia_editor_error_word_not_found))
        mPronunciationAddress = "no"
        mMp3Address = "no"
    }

    private fun stop(string: CharSequence) {
        progressDialog!!.dismiss()
        toast(string)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_KEY_SHUT_OFF, true)
    }

    private fun computeAddressOfTranslationPage(): String {
        // Service name has to be replaced from the language lister.
        var address = "http://dict.tu-chemnitz.de/dings.cgi?lang=en&service=SERVICE&opterrors=0&optpro=0&query=Welt"

        val strFrom = mSpinnerFrom.selectedItem.toString()
        val langCodeFrom = mLanguageLister!!.getCodeFor(strFrom)

        val query: String = try {
            URLEncoder.encode(mSource, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            mSource!!.replace(" ", "%20")
        }

        address = address.replace("SERVICE".toRegex(), langCodeFrom).replace("Welt".toRegex(), query)

        return address
    }

    // If the loading and dialog are cancelled
    override fun onCancel(dialog: DialogInterface) {
        mStopped = true

        dismissCarefullyProgressDialog()

        stopAllTasks()

        val resultData = Intent()

        setResult(Activity.RESULT_CANCELED, resultData)

        finish();
    }

    private fun dismissCarefullyProgressDialog() {
        try {
            if (progressDialog != null) {
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
            }
        } catch (e: Exception) {
            // nothing is done intentionally
        }

    }

    private fun stopAllTasks() {
        downloadFileJob?.cancel()
        backgroundPostJob?.cancel()
    }

    override fun onPause() {
        super.onPause()
        dismissCarefullyProgressDialog()
        stopAllTasks()
    }

    companion object {
        private val BUNDLE_KEY_SHUT_OFF = "key.multimedia.shut.off"
        // Must be passed in
        var EXTRA_SOURCE = "com.ichi2.anki.LoadPronounciationActivity.extra.source"
        // Passed out as a result
        var EXTRA_PRONUNCIATION_FILE_PATH = "com.ichi2.anki.LoadPronounciationActivity.extra.pronun.file.path"
    }
}
