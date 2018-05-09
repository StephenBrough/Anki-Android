/***************************************************************************************
 * Copyright (c) 2016 Houssam Salem <houssam.salem.au></houssam.salem.au>@gmail.com>                        *
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

package com.ichi2.libanki.importer


import com.google.gson.stream.JsonReader
import com.ichi2.anki.junkdrawer.BackupManager
import com.ichi2.anki.R
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Storage
import com.ichi2.libanki.Utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.HashMap
import java.util.zip.ZipFile

import timber.log.Timber


class AnkiPackageImporter(col: Collection, file: String) : Anki2Importer(col, file) {

    private var mZip: ZipFile? = null
    private var mNameToNum: MutableMap<String, String>? = null

    override fun runImport() {
        publishProgress(0, 0, 0)
        val tempDir = File(File(col.path).parent, "tmpzip")
        val tmpCol: Collection?
        try {
            // We extract the zip contents into a temporary directory and do a little more
            // validation than the desktop client to ensure the extracted collection is an apkg.
            try {
                // extract the deck from the zip file
                mZip = ZipFile(File(file), ZipFile.OPEN_READ)
                Utils.unzipFiles(mZip, tempDir.absolutePath, arrayOf("collection.anki2", "media"), null)
            } catch (e: IOException) {
                Timber.e(e, "Failed to unzip apkg.")
                log.add(res.getString(R.string.import_log_no_apkg))
                return
            }

            val colpath = File(tempDir, "collection.anki2").absolutePath
            if (!File(colpath).exists()) {
                log.add(res.getString(R.string.import_log_no_apkg))
                return
            }
            tmpCol = Storage.Collection(context, colpath)
            try {
                if (!tmpCol!!.validCollection()) {
                    log.add(res.getString(R.string.import_log_no_apkg))
                    return
                }
            } finally {
                tmpCol?.close()
            }
            file = colpath
            // we need the media dict in advance, and we'll need a map of fname ->
            // number to use during the import
            val mediaMapFile = File(tempDir, "media")
            mNameToNum = HashMap()
            // We need the opposite mapping in AnkiDroid since our extraction method requires it.
            val numToName = HashMap<String, String>()
            try {
                val jr = JsonReader(FileReader(mediaMapFile))
                jr.beginObject()
                var name: String
                var num: String
                while (jr.hasNext()) {
                    num = jr.nextName()
                    name = jr.nextString()
                    mNameToNum!![name] = num
                    numToName[num] = name
                }
                jr.endObject()
                jr.close()
            } catch (e: FileNotFoundException) {
                Timber.e("Apkg did not contain a media dict. No media will be imported.")
            } catch (e: IOException) {
                Timber.e("Malformed media dict. Media import will be incomplete.")
            }

            // runImport anki2 importer
            super.runImport()
            // import static media
            for ((file, c) in mNameToNum!!) {
                if (!file.startsWith("_") && !file.startsWith("latex-")) {
                    continue
                }
                val path = File(col.media.dir(), Utils.nfcNormalized(file))
                if (!path.exists()) {
                    try {
                        Utils.unzipFiles(mZip, col.media.dir(), arrayOf(c), numToName)
                    } catch (e: IOException) {
                        Timber.e("Failed to extract static media file. Ignoring.")
                    }

                }
            }
        } finally {
            // Clean up our temporary files
            if (tempDir.exists()) {
                BackupManager.removeDir(tempDir)
            }
        }
        publishProgress(100, 100, 100)
    }

    override fun _srcMediaData(fname: String): BufferedInputStream? {
        if (mNameToNum!!.containsKey(fname)) {
            try {
                return BufferedInputStream(mZip!!.getInputStream(mZip!!.getEntry(mNameToNum!![fname])))
            } catch (e: IOException) {
                Timber.e("Could not extract media file " + fname + "from zip file.")
            }

        }
        return null
    }
}
