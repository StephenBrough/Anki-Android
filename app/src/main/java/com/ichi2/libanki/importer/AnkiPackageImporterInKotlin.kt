package com.ichi2.libanki.importer

import com.ichi2.anki.R
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Storage
import com.ichi2.libanki.Utils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class AnkiPackageImporterInKotlin(col: Collection, file: String) : Anki2ImporterInKotlin(col, file) {

    lateinit var zip: ZipFile

    override fun runImport() {
        publishProgress(0, 0, 0)
        val tmpDir = File(File(col.path).parent, "tmpzip")

        // We extract the zip contents into a temporary directory and do a little more
        // validation than the desktop client to ensure the extracted collection is an apkg
        try {
            // Extract the deck from the zip file
            zip = ZipFile(File(file), ZipFile.OPEN_READ)
            Utils.unzipFiles(zip, tmpDir.absolutePath, arrayOf(ANKI_DB_NAME, MEDIA_DB_NAME), null)
        } catch (e: IOException) {
            Timber.e(e, "Failed to unzip apkg")
            log.add(res.getString(R.string.import_log_no_apkg))
            return
        }

        val colFile = File(tmpDir, ANKI_DB_NAME)

        if (!colFile.exists()) {
            log.add(res.getString(R.string.import_log_no_apkg))
            return
        }

        val tmpCol = Storage.Collection(context, colFile.absolutePath)
    }

    companion object {
        const val ANKI_DB_NAME = "collection.anki2"
        const val MEDIA_DB_NAME = "media"
    }
}