package com.ichi2.libanki.importer

import com.ichi2.anki.R
import com.ichi2.anki.deckpicker.model.TaskData
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Storage
import timber.log.Timber

open class Anki2ImporterInKotlin(col: Collection, file: String) : Importer(col, file) {

    var deckPrefix: String = ""
    var allowUpdate: Boolean
    val dupeOnSchemaChange: Boolean

    val decks = hashMapOf<Long, Long>()
    val modelMap = hashMapOf<Long, Long>()
    val notes = hashMapOf<String, Array<Any>>()

    init {
        needMapper = false
//        deckPrefix = null
        allowUpdate = true
        dupeOnSchemaChange = false
    }

    override fun runImport() {
        publishProgress(0, 0, 0)

        try {
            prepareFiles()
            try {
                import()
            } finally {
                src.close(false)
            }

        } catch (e: RuntimeException) {
            Timber.e(e, "RuntimeException while importing")
        }
    }

    private fun prepareFiles() {
        dst = col
        src = Storage.Collection(context, file)
    }

    private fun import() {
        try {
            // Use transactions for performance and rollbacks in case of error
            dst?.db?.database?.beginTransaction()
            dst?.media?.db?.database?.beginTransaction()

            if (!deckPrefix.isBlank()) {
                val id = dst.decks.id(deckPrefix)
                dst.decks.select(id)
            }

            prepareTS()
            importNotes() // TODO: Start here when working on this again

        } finally {
            dst?.db?.database?.endTransaction()
            dst?.media?.db?.database?.endTransaction()
        }
    }

    //region Preparado!

    //endregion

    fun importNotes() {
        // build guid -> (id,mod,mid) hash & map of existing note ids
        val existing = hashMapOf<Long, Boolean>()


    }

    /**
     * @param notesDone Percentage of notes complete.
     * @param cardsDone Percentage of cards complete.
     * @param postProcess Percentage of remaining tasks complete.
     */
    protected fun publishProgress(notesDone: Int, cardsDone: Int, postProcess: Int) {
        val taskData = TaskData(res.getString(R.string.import_progress,
                notesDone, cardsDone, postProcess))

        progress?.publishProgress(taskData)
        progressUpdate(taskData)
    }

}