package com.ichi2.anki.deckpicker

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.net.Uri
import android.text.TextUtils
import com.google.gson.stream.JsonReader
import com.ichi2.anki.*
import com.ichi2.anki.deckpicker.model.ExportTaskData
import com.ichi2.anki.deckpicker.model.TaskData
import com.ichi2.anki.dialogs.ConfirmationDialog
import com.ichi2.anki.junkdrawer.BackupManager
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.utils.compat.CompatHelper
import com.ichi2.libanki.AnkiPackageExporter
import com.ichi2.libanki.importer.AnkiPackageImporter
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.json.JSONException
import timber.log.Timber
import java.io.File
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Storage
import com.ichi2.libanki.Utils
import kotlinx.coroutines.experimental.delay
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.HashMap
import java.util.zip.ZipFile


class DeckPickerViewModel(application: Application) : AndroidViewModel(application) {

    var contextMenuDeckId: Long = 0
    // flag asking user to do a full sync which is used in upgrade path
    var recommendFullSync = false
    var deckPickerView: DeckPickerView? = null

    val col: Collection?
        get() = CollectionHelper.getInstance().getCol(getApplication())

    /**
     * Keep track of which deck was last given focus in the deck list. If we find that this value
     * has changed between deck list refreshes, we need to recenter the deck list to the new current
     * deck.
     */
    var focusedDeck = 0L

    fun deckExpander(deckId: Long) {
        val collection = CollectionHelper.getInstance().getCol(getApplication())
        if (collection.decks.children(deckId).size > 0) {
            collection.decks.collapse(deckId)
            deckPickerView?.updateDeckList()
            deckPickerView?.dismissAllDialogFragments()
        }
    }

    fun selectDeck(deckId: Long) {
        Timber.i("DeckPickerActivity:: Selected deck with id %d", deckId)
        deckPickerView?.collapseActionsMenu()
        deckPickerView?.handleDeckSelection(deckId, false)
        deckPickerView?.notifyDataSetChanged()
    }

    fun countsClick(deckId: Long) {
        Timber.i("DeckPickerActivity:: Selected deck with id %d", deckId)
        deckPickerView?.collapseActionsMenu()
        deckPickerView?.handleDeckSelection(deckId, true)
        deckPickerView?.notifyDataSetChanged()
    }

    fun deckLongClick(deckId: Long): Boolean {
        Timber.i("DeckPickerActivity:: Long tapped on deck with id %d", deckId)
        contextMenuDeckId = deckId
        deckPickerView?.showDialogFragment(deckId)
        return true
    }

    fun importAdd(path: String) {
        launch(UI) {
            // Pre execute
            // Show progress dialog
            deckPickerView?.showProgress("Importing")
            delay(2000)
            // Actual task
            Timber.d("doInBackgroundImportAdd")
            val imp = AnkiPackageImporter(col, path)
            imp.setProgressCallback {
                // On Progress Update
                deckPickerView?.showProgress(it.string ?: "")
            }
            imp.runImport()

            // Post execute
            delay(1000)
            deckPickerView?.dismissProgressSnackbar()
            // TODO: Handle showing notification if activity is destroyed...? Originally this was handled
            deckPickerView?.showSimpleMessageDialogLocal(TextUtils.join("\n", imp.log))
            deckPickerView?.updateDeckList()
        }
    }

    fun importReplace(importPath: String) {
        // Pre-Execute
        deckPickerView?.showProgress(R.string.import_replacing)

        // Post Execute
        fun handleResult(result: TaskData) {
            deckPickerView?.dismissProgressSnackbar()
            if (result != null && result.boolean) {
                val code = result.int
                if (code == -2) {
                    // not a valid apkg file
                    deckPickerView?.showSimpleMessageDialogLocal(R.string.import_log_no_apkg)
                }
                deckPickerView?.updateDeckList()
                deckPickerView?.showSimpleMessageDialogLocal("Importing finished")
            } else {
                deckPickerView?.showSimpleMessageDialogLocal(R.string.import_log_no_apkg)
            }
        }

        launch(UI) {
            delay(2000)

            Timber.d("doInBackgroundImportReplace")
            val res = AnkiDroidApp.getInstance().baseContext.resources

            // extract the deck from the zip file
            val colPath = col!!.path
            val dir = File(File(colPath).parentFile, "tmpzip")
            if (dir.exists()) {
                BackupManager.removeDir(dir)
            }

            // from anki2.py
            val colFile = File(dir, "collection.anki2").absolutePath
            val zip: ZipFile
            try {
                zip = ZipFile(File(importPath), ZipFile.OPEN_READ)
            } catch (e: IOException) {
                Timber.e(e, "doInBackgroundImportReplace - Error while unzipping")
                handleResult(TaskData(false))
                return@launch
            }

            try {
                Utils.unzipFiles(zip, dir.absolutePath, arrayOf("collection.anki2", "media"), null)
            } catch (e: IOException) {
                handleResult(TaskData(-2, null, false))
                return@launch
            }

            if (!File(colFile).exists()) {
                handleResult(TaskData(-2, null, false))
                return@launch
            }

            var tmpCol: Collection? = null
            try {
                tmpCol = Storage.Collection(getApplication(), colFile)
                if (!tmpCol!!.validCollection()) {
                    tmpCol.close()
                    handleResult(TaskData(-2, null, false))
                    return@launch
                }
            } catch (e: Exception) {
                Timber.e("Error opening new collection file... probably it's invalid")
                try {
                    tmpCol!!.close()
                } catch (e2: Exception) {
                    // do nothing
                }
                handleResult(TaskData(-2, null, false))
                return@launch
            } finally {
                if (tmpCol != null) {
                    tmpCol.close()
                }
            }

            deckPickerView?.showProgress(R.string.importing_collection)
            delay(2000)
            if (col != null) {
                // unload collection and trigger a backup
                CollectionHelper.getInstance().closeCollection(true)
                CollectionHelper.getInstance().lockCollection()
                BackupManager.performBackupInBackground(colPath, true)
            }
            // overwrite collection
            val f = File(colFile)
            if (!f.renameTo(File(colPath))) {
                // Exit early if this didn't work
                handleResult(TaskData(-2, null, false))
                return@launch
            }
            try {
                CollectionHelper.getInstance().unlockCollection()

                // because users don't have a backup of media, it's safer to import new
                // data and rely on them running a media db check to get rid of any
                // unwanted media. in the future we might also want to duplicate this step
                // import media
                // TODO: The following never seems to put anything into nameToNum... investigate why
                val nameToNum = HashMap<String, String>()
                val numToName = HashMap<String, String>()
                val mediaMapFile = File(dir.absolutePath, "media")
                if (mediaMapFile.exists()) {
                    val jr = JsonReader(FileReader(mediaMapFile))
                    jr.beginObject()
                    var name: String
                    var num: String
                    while (jr.hasNext()) {
                        num = jr.nextName()
                        name = jr.nextString()
                        nameToNum.put(name, num)
                        numToName.put(num, name)
                    }
                    jr.endObject()
                    jr.close()
                }
                Timber.d("NameToNum: $nameToNum")
                val mediaDir = col!!.media.dir()
                val total = nameToNum.size
                var i = 0
                Timber.d("NameToNum size: ${nameToNum.size}")
                for ((file, c) in nameToNum) {
                    val of = File(mediaDir, file)
                    if (!of.exists()) {
                        Utils.unzipFiles(zip, mediaDir, arrayOf(c), numToName)
                    }
                    ++i
                    deckPickerView?.showProgress(res.getString(R.string.import_media_count, (i + 1) * 100 / total))
                    delay(500)
                }
                zip.close()
                // delete tmp dir
                BackupManager.removeDir(dir)
                handleResult(TaskData(true))
                return@launch
            } catch (e: RuntimeException) {
                Timber.e(e, "doInBackgroundImportReplace - RuntimeException")
                handleResult(TaskData(false))
                return@launch
            } catch (e: FileNotFoundException) {
                Timber.e(e, "doInBackgroundImportReplace - FileNotFoundException")
                handleResult(TaskData(false))
                return@launch
            } catch (e: IOException) {
                Timber.e(e, "doInBackgroundImportReplace - IOException")
                handleResult(TaskData(false))
                return@launch
            }
        }
    }

    fun joinSyncMessages(dialogMessage: String, syncMessage: String): String {
        // If both strings have text, separate them by a new line, otherwise return whichever has text
        return if (!TextUtils.isEmpty(dialogMessage) && !TextUtils.isEmpty(syncMessage)) {
            dialogMessage + "\n\n" + syncMessage
        } else if (!TextUtils.isEmpty(dialogMessage)) {
            dialogMessage
        } else {
            syncMessage
        }
    }

    /**
     * Try to open the Collection for the first time, and do some errorSnackbar handling if it wasn't successful
     * @return whether or not we were successful
     */
    fun firstCollectionOpen(): Boolean {
        if (CollectionHelper.hasStorageAccessPermission(getApplication())) {
            // Show errorSnackbar dialog if collection could not be opened
            if (CollectionHelper.getInstance().getColSafe(getApplication()) == null) {
                return false
            }
        } else {
            // Request storage permission if we don't have it
            deckPickerView?.requestStoragePermission()
            return false
        }
        return true
    }

    fun exportApkg(path: String?, did: Long?, includeSched: Boolean, includeMedia: Boolean) {
        // Export the file to sdcard/AnkiDroid/export regardless of actual col directory, so that we can use FileProvider API
        val exportDir = File(CollectionHelper.getDefaultAnkiDroidDirectory(), "export")
        exportDir.mkdirs()
        val exportPath: File
        if (path != null) {
            // path has been explicitly specified
            exportPath = File(exportDir, path)
        } else if (did != null) {
            // path not explicitly specified, but a deck has been specified so use deck name
            try {
                exportPath = File(exportDir, col!!.decks.get(did).getString("name").replace("\\W+".toRegex(), "_") + ".apkg")
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

        } else if (!includeSched) {
            // full export without scheduling is assumed to be shared with someone else -- use "All Decks.apkg"
            exportPath = File(exportDir, "All Decks.apkg")
        } else {
            // full collection export -- use "collection.apkg"
            val colPath = File(col!!.path)
            exportPath = File(exportDir, colPath.name.replace(".anki2", ".apkg"))
        }

        launch(UI) {
            exportAnkiDeck(ExportTaskData(col!!, exportPath.path, did, includeSched, includeMedia))
        }
    }

    suspend fun exportAnkiDeck(exportTaskData: ExportTaskData) {
        // TODO: Handle canceling this job
        // TODO: Add this dialog to the fragment back stack
        deckPickerView?.showProgress("")

        Timber.d("doInBackgroundExportApkg")

        var result = TaskData(false)

        try {
            AnkiPackageExporter(exportTaskData.col).apply {
                setIncludeSched(exportTaskData.includeSched)
                setIncludeMedia(exportTaskData.includeMedia)
                setDid(exportTaskData.deckId)
                exportInto(exportTaskData.apkgPath, getApplication())
            }

            result = TaskData(exportTaskData.apkgPath)
        } catch (e: FileNotFoundException) {
            Timber.e(e, "FileNotFoundException in doInBackgroundExportApkg")
        } catch (e: IOException) {
            Timber.e(e, "IOException in doInBackgroundExportApkg")
        } catch (e: JSONException) {
            Timber.e(e, "JSOnException in doInBackgroundExportApkg")
        }


        // FIXME: Delay added because in some circumstances the dialog dismiss call happens
        //        before the dialog is rendered, causing it to miss the dismiss call.
        // TODO:  Get rid of progress dialog
        delay(500)
        deckPickerView?.dismissStyledProgressDialog()
        val exportPath = result.string
        if (exportPath != null) {
            deckPickerView?.showExportCompleteDialog(exportPath)
        } else {
            deckPickerView?.showThemedToast(R.string.export_unsuccessful)
        }
    }

    fun emailFile(path: String) {
        // Make sure the file actually exists
        val attachment = File(path)
        if (!attachment.exists()) {
            Timber.e("Specified apkg file %s does not exist", path)
            deckPickerView?.showThemedToast(R.string.apk_share_error)
            return
        }
        // Get a URI for the file to be shared via the FileProvider API
        val uri: Uri
        try {
            uri = CompatHelper.compat.getExportUri(getApplication(), attachment)
        } catch (e: IllegalArgumentException) {
            Timber.e("Could not generate a valid URI for the apkg file")
            deckPickerView?.showThemedToast(R.string.apk_share_error)
            return
        }

        deckPickerView?.resolveIntent(uri, attachment.name)
    }

    // TODO: Test if this works... I don't know how to enable 'dyn' decks to test this
    fun deckTaskRebuildCram(fragmented: Boolean) {
        launch(UI) {
            col!!.decks.select(contextMenuDeckId)

            // Pre execute
            deckPickerView?.showProgress("")

            // Execution
            Timber.d("doInBackgroundEmptyCram")
            col!!.sched.emptyDyn(col!!.decks.selected())

            Timber.d("doInBackgroundUpdateValuesFromDeck")
            try {
                val sched = col!!.sched
                sched.reset()
            } catch (e: RuntimeException) {
                Timber.e(e, "doInBackgroundUpdateValuesFromDeck - an errorSnackbar occurred")
                return@launch
            }


            // Post execute
            deckPickerView?.updateDeckList()
            if (fragmented)
                deckPickerView?.loadStudyOptionsFragment(false)
        }
    }

    // TODO: Test this out
    fun handleEmptyCards() {
        // Pre-Execute
        deckPickerView?.showProgress(R.string.empty_cards_finding)

        // Execution
        val cids = col!!.emptyCids()

        // Post Execute
        if (cids.isEmpty()) {
            deckPickerView?.showSimpleMessageDialogLocal(R.string.empty_cards_none)
//            showSimpleMessageDialog(resources.getString(R.string.empty_cards_none))
        } else {
            val msg = String.format(getApplication<Application>().resources.getString(R.string.empty_cards_count), cids.size)
            val dialog = ConfirmationDialog()
            dialog.setArgs(msg)
            val confirm = Runnable {
                col!!.remCards(Utils.arrayList2array(cids))
                deckPickerView?.showSimpleSnackbar(String.format(getApplication<Application>().resources
                        .getString(R.string.empty_cards_deleted), cids.size))
//                UIUtils.showSimpleSnackbar(this@DeckPickerActivity, String.format(
//                        resources.getString(R.string.empty_cards_deleted), cids.size), false)
            }
            dialog.setConfirm(confirm)
//            showDialogFragment(dialog)
            deckPickerView?.showDialogFragment(dialog)
        }

        deckPickerView?.dismissProgressSnackbar()

//        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
//            mProgressDialog!!.dismiss()
//        }
    }

}