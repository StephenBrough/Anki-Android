package com.ichi2.libanki

import android.content.Context
import com.ichi2.libanki.hooks.Hooks
import java.io.File

object StorageInKotlin {

    const val ANKI_FILE_EXTENSION = ".anki2"

    fun collection(context: Context, path: String, server: Boolean = false, log: Boolean = false) {
        assert(path.endsWith(ANKI_FILE_EXTENSION))

        // Since this is the entry point into libanki, initialize the hooks here.
        Hooks.getInstance(context)

        val dbFile = File(path)
        val dbExists = !dbFile.exists()

        // Connect
//        try {
//            // initialize
//            val ver = if (dbExists) createDb()
//            else upgradeSchema(db)
//        }
    }

    fun createDb() {
        // db.execute("PRAGMA page_size = 4096")
        // db.execute("PRAGMA legacy_file_format = 0"); ????? DO I NEED THIS??
        // db.execute("VACUUM")

        // Create tables
//        db.execute("INSERT OR IGNORE INTO col VALUES(1,0,0," +
//                Utils.intNow(1000) + "," + Consts.SCHEMA_VERSION +
//                ",0,0,0,'','{}','','','{}')")

        // Create default JSON data (see _setColVars())

        // db.execute("ANALYZE")

    }
}