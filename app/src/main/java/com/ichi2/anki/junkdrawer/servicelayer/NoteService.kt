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

package com.ichi2.anki.junkdrawer.servicelayer

import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.fields.*
import com.ichi2.anki.multimediacard.impl.MultimediaEditableNote
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Note

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.IOException

object NoteService {
    /**
     * Creates an empty Note from given Model
     *
     * @param model the model in JSOBObject format
     * @return a new note instance
     */
    fun createEmptyNote(model: JSONObject): MultimediaEditableNote? {
        try {
            val fieldsArray = model.getJSONArray("flds")
            val numOfFields = fieldsArray.length()
            if (numOfFields > 0) {
                val note = MultimediaEditableNote()
                note.setNumFields()

                for (i in 0 until numOfFields) {
                    val fieldObject = fieldsArray.getJSONObject(i)
                    val uiTextField = TextField()
                    uiTextField.name = fieldObject.getString("name")
                    uiTextField.text = fieldObject.getString("name")
                    note.setField(i, uiTextField)
                }
                note.modelId = model.getLong("id")
                return note
            }
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return null
    }


    fun updateMultimediaNoteFromJsonNote(col: Collection, editorNoteSrc: Note, noteDst: IMultimediaEditableNote) {
        if (noteDst is MultimediaEditableNote) {
            val values = editorNoteSrc.fields
            for (i in values.indices) {
                val value = values[i]
                var field: IField? = null
                field = when {
                    value.startsWith("<img") -> ImageField()
                    value.startsWith("[sound:") -> AudioField()
                    else -> TextField()
                }
                field.setFormattedString(col, value)
                noteDst.setField(i, field)
            }
            noteDst.modelId = editorNoteSrc.mid
            // TODO: set current id of the note as well
        }
    }


    /**
     * Updates the JsonNote field values from MultimediaEditableNote When both notes are using the same Model, it updaes
     * the destination field values with source values. If models are different it throws an Exception
     *
     * @param noteSrc
     * @param editorNoteDst
     */
    fun updateJsonNoteFromMultimediaNote(noteSrc: IMultimediaEditableNote, editorNoteDst: Note) {
        if (noteSrc is MultimediaEditableNote) {
            if (noteSrc.modelId != editorNoteDst.mid) {
                throw RuntimeException("Source and Destination Note ID do not match.")
            }

            val totalFields = noteSrc.numberOfFields
            for (i in 0 until totalFields) {
                editorNoteDst.values()[i] = noteSrc.getField(i)!!.formattedValue
            }
        }
    }


    /**
     * Saves the multimedia associated with this card to proper path inside anki folder. For each field associated with
     * the note it checks for the following condition a. The field content should have changed b. The field content does
     * not already point to a media inside anki media path If both condition satisfies then it copies the file inside
     * the media path and deletes the file referenced by the note
     *
     * @param noteNew
     */
    fun saveMedia(col: Collection, noteNew: MultimediaEditableNote) {
        // if (noteNew.getModelId() == noteOld.getModelId())
        // {
        // int fieldCount = noteNew.getNumberOfFields();
        // for (int i = 0; i < fieldCount; i++)
        // {
        // IField newField = noteNew.getField(i);
        // IField oldField = noteOld.getField(i);
        // if
        // (newField.getFormattedValue().equals(oldField.getFormattedValue()))
        // {
        // continue;
        // }
        // importMediaToDirectory(newField);
        // }
        // }
        // else
        // {
        val fieldCount = noteNew.numberOfFields
        (0 until fieldCount)
                .map { noteNew.getField(it) }
                .forEach { importMediaToDirectory(col, it) }
        // }
    }


    /**
     * Considering the field is new, if it has media handle it
     *
     * @param field
     */
    private fun importMediaToDirectory(col: Collection, field: IField?) {
        var tmpMediaPath: String? = null
        when (field!!.type) {
            EFieldType.AUDIO -> tmpMediaPath = field.audioPath

            EFieldType.IMAGE -> tmpMediaPath = field.imagePath

            EFieldType.TEXT -> {
            }
        }
        if (tmpMediaPath != null) {
            try {
                val inFile = File(tmpMediaPath)
                if (inFile.exists()) {
                    val fname = col.media.addFile(inFile)
                    val outFile = File(col.media.dir(), fname)
                    if (field.hasTemporaryMedia() && outFile.absolutePath != tmpMediaPath) {
                        // Delete original
                        inFile.delete()
                    }
                    when (field.type) {
                        EFieldType.AUDIO -> field.audioPath = outFile.absolutePath
                        EFieldType.IMAGE -> field.imagePath = outFile.absolutePath
                        else -> {
                        }
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

        }
    }
}
