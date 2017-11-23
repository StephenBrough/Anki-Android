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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams

import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R
import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.fields.AudioField
import com.ichi2.anki.multimediacard.fields.BasicControllerFactory
import com.ichi2.anki.multimediacard.fields.EFieldType
import com.ichi2.anki.multimediacard.fields.IField
import com.ichi2.anki.multimediacard.fields.IFieldController
import com.ichi2.anki.multimediacard.fields.ImageField
import com.ichi2.anki.multimediacard.fields.TextField
import kotlinx.android.synthetic.main.multimedia_edit_field_activity.*
import kotlinx.android.synthetic.main.toolbar.view.*

import java.io.File

import timber.log.Timber

class MultimediaEditFieldActivity : AnkiActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    lateinit private var mField: IField
    lateinit private var mNote: IMultimediaEditableNote

    private var mFieldIndex: Int = 0

    private var mFieldController: IFieldController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.getBoolean(BUNDLE_KEY_SHUT_OFF, false) == true) {
            finishCancel()
            return
        }

        setContentView(R.layout.multimedia_edit_field_activity)
        val mainView = findViewById<View>(android.R.id.content)
        setSupportActionBar(mainView.toolbar)

        with(intent) {
            mField = extras.getSerializable(EXTRA_FIELD) as IField
            mNote = getSerializableExtra(EXTRA_WHOLE_NOTE) as IMultimediaEditableNote
            mFieldIndex = getIntExtra(EXTRA_FIELD_INDEX, 0)
        }

        recreateEditingUi()
    }

    /**
     * Sets the result to RESULT_CANCELED and finishes the activity
     */
    private fun finishCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish();
    }

    private fun recreateEditingUi() {
        val controllerFactory = BasicControllerFactory.getInstance()

        mFieldController = controllerFactory.createControllerForField(mField)

        if (mFieldController == null) {
            Timber.d("Field controller creation failed")
            return
        }

        // Request permission to record if audio field
        if (mField is AudioField && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_AUDIO_PERMISSION)
            return
        }

        linearLayoutInScrollViewFieldEdit.removeAllViews()

        mFieldController?.let {
           it.setField(mField)
           it.setFieldIndex(mFieldIndex)
           it.setNote(mNote)
           it.setEditingActivity(this)
           it.createUI(this, linearLayoutInScrollViewFieldEdit)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_edit_text, menu)
        menu.findItem(R.id.multimedia_edit_field_to_text).isVisible = mField.type !== EFieldType.TEXT
        menu.findItem(R.id.multimedia_edit_field_to_audio).isVisible = mField.type !== EFieldType.AUDIO
        menu.findItem(R.id.multimedia_edit_field_to_image).isVisible = mField.type !== EFieldType.IMAGE
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.multimedia_edit_field_to_text -> {
                Timber.i("To text field button pressed")
                mFieldController!!.onFocusLost()
                toTextField()
                supportInvalidateOptionsMenu()
                return true
            }

            R.id.multimedia_edit_field_to_image -> {
                Timber.i("To image button pressed")
                mFieldController!!.onFocusLost()
                toImageField()
                supportInvalidateOptionsMenu()
                return true
            }

            R.id.multimedia_edit_field_to_audio -> {
                Timber.i("To audio button pressed")
                mFieldController!!.onFocusLost()
                toAudioField()
                supportInvalidateOptionsMenu()
                return true
            }

            R.id.multimedia_edit_field_done -> {
                Timber.i("Save button pressed")
                done()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun done() {
        mFieldController!!.onDone()

        val resultData = Intent()

        var bChangeToText = false

        when(mField.type) {

            EFieldType.TEXT -> { /* nothing */ }
            EFieldType.IMAGE -> {
                if (mField.imagePath == null) {
                    bChangeToText = true
                }

                if (!bChangeToText) {
                    val f = File(mField.imagePath)
                    if (!f.exists()) {
                        bChangeToText = true
                    }
                }
            }
            EFieldType.AUDIO -> {
                if (mField.audioPath == null) {
                    bChangeToText = true
                }

                if (!bChangeToText) {
                    val f = File(mField.audioPath)
                    if (!f.exists()) {
                        bChangeToText = true
                    }
                }
            }
        }

        if (bChangeToText) {
            mField = TextField()
        }

        resultData.putExtra(EXTRA_RESULT_FIELD, mField)
        resultData.putExtra(EXTRA_RESULT_FIELD_INDEX, mFieldIndex)

        setResult(Activity.RESULT_OK, resultData)

        finish();
    }

    private fun toAudioField() {
        if (mField.type !== EFieldType.AUDIO) {
            mField = AudioField()
            recreateEditingUi()
        }
    }

    private fun toImageField() {
        if (mField.type !== EFieldType.IMAGE) {
            mField = ImageField()
            recreateEditingUi()
        }

    }

    private fun toTextField() {
        if (mField.type !== EFieldType.TEXT) {
            mField = TextField()
            recreateEditingUi()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (mFieldController != null) {
            mFieldController!!.onActivityResult(requestCode, resultCode, data)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_AUDIO_PERMISSION && permissions.size == 1) {
            // TODO:  Disable the record button / show some feedback to the user
            recreateEditingUi()
        }
    }

    fun handleFieldChanged(newField: IField) {
        mField = newField
        recreateEditingUi()
    }


    override fun onDestroy() {
        super.onDestroy()

        if (mFieldController != null) {
            mFieldController!!.onDestroy()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_KEY_SHUT_OFF, true)
    }

    companion object {

        val EXTRA_RESULT_FIELD = "edit.field.result.field"
        val EXTRA_RESULT_FIELD_INDEX = "edit.field.result.field.index"

        val EXTRA_FIELD_INDEX = "multim.card.ed.extra.field.index"
        val EXTRA_FIELD = "multim.card.ed.extra.field"
        val EXTRA_WHOLE_NOTE = "multim.card.ed.extra.whole.note"

        private val BUNDLE_KEY_SHUT_OFF = "key.edit.field.shut.off"
        private val REQUEST_AUDIO_PERMISSION = 0
    }

}
