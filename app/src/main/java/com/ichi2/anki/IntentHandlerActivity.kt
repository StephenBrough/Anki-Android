package com.ichi2.anki

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity

import com.crashlytics.android.Crashlytics
import com.ichi2.anki.dialogs.InvalidApkgFileDialog
import com.ichi2.anki.flashcardviewer.Reviewer
import com.ichi2.utils.Utils
import com.ichi2.utils.anim.ActivityTransitionAnimation
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.junkdrawer.services.ReminderService

import java.io.File
import timber.log.Timber

/**
 * Class which handles how the application responds to different intents, forcing it to always be single task,
 * but allowing custom behavior depending on the intent
 *
 * @author Tim
 */

class IntentHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_bar)

        val intent = intent
        Timber.v(intent.toString())
        val redirectIntent = Intent(this, DeckPickerActivity::class.java)
        redirectIntent.setDataAndType(getIntent().data, getIntent().type)
        val action = intent.action

        if (Intent.ACTION_VIEW == action) {
            // This intent is used for opening apkg package files
            // We want to go immediately to DeckPickerActivity, clearing any history in the process
            Timber.i("IntentHandlerActivity/ User requested to view a file")
            var successful = false
            var errorMessage = resources.getString(R.string.import_error_content_provider, AnkiDroidApp.getManualUrl() + "#importing")
            // If the file is being sent from a content provider we need to read the content before we can open the file
            if (intent.data!!.scheme == "content") {
                // Get the original filename from the content provider URI
                var filename: String? = null
                var cursor: Cursor? = null
                try {
                    cursor = this.contentResolver.query(intent.data!!, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        filename = cursor.getString(0)
                    }
                } finally {
                    if (cursor != null)
                        cursor.close()
                }

                // Hack to fix bug where ContentResolver not returning filename correctly
                if (filename == null) {
                    if (intent.type == "application/apkg" || Utils.hasValidZipFile(this, intent)) {
                        // Set a dummy filename if MIME type provided or is a valid zip file
                        filename = "unknown_filename.apkg"
                        Timber.w("Could not retrieve filename from ContentProvider, but was valid zip file so we try to continue")
                    } else {
                        Timber.e("Could not retrieve filename from ContentProvider or read content as ZipFile")
                    }
                }

                if (filename != null && !filename.toLowerCase().endsWith(".apkg")) {
                    // Don't import if not apkg file
                    errorMessage = resources.getString(R.string.import_error_not_apkg_extension, filename)
                } else if (filename != null) {
                    // Copy to temporary file
                    val tempOutDir = Uri.fromFile(File(cacheDir, filename)).encodedPath
                    successful = Utils.copyFileToCache(this, intent, tempOutDir)
                    // Show import dialog
                    if (successful) {
                        Utils.sendShowImportFileDialogMsg(tempOutDir)
                    } else {
                        Crashlytics.logException(Throwable("IntentHandlerActivity.java - Error importing apkg file"))
                    }
                }
            } else if (intent.data!!.scheme == "file") {
                // When the VIEW intent is sent as a file, we can open it directly without copying from content provider
                val filename = intent.data!!.path
                if (filename != null && filename.endsWith(".apkg")) {
                    // If file has apkg extension then send message to show Import dialog
                    Utils.sendShowImportFileDialogMsg(filename)
                    successful = true
                } else {
                    errorMessage = resources.getString(R.string.import_error_not_apkg_extension, filename)
                }
            }
            // Start DeckPickerActivity if we correctly processed ACTION_VIEW
            if (successful) {
                redirectIntent.action = action
                redirectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(redirectIntent)
                finishWithFade()
            } else {
                // Don't import the file if it didn't load properly or doesn't have apkg extension
                //Themes.showThemedToast(this, getResources().getString(R.string.import_log_no_apkg), true);
                val title = resources.getString(R.string.import_log_no_apkg)
                // TODO: Move this to a dialog fragment
                InvalidApkgFileDialog.show(supportFragmentManager, title, errorMessage) { finishWithFade() }
            }
        } else if ("com.ichi2.anki.DO_SYNC" == action) {
            Utils.sendDoSyncMsg()
            redirectIntent.action = action
            redirectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(redirectIntent)
            finishWithFade()
        } else if (intent.hasExtra(ReminderService.EXTRA_DECK_ID)) {
            val reviewIntent = Intent(this, Reviewer::class.java)

            CollectionHelper.getInstance().getCol(this)!!.decks.select(intent.getLongExtra(ReminderService.EXTRA_DECK_ID, 0))
            startActivity(reviewIntent)
            finishWithFade()
        } else {
            // Launcher intents should start DeckPickerActivity if no other task exists,
            // otherwise go to previous task
            redirectIntent.action = Intent.ACTION_MAIN
            redirectIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            redirectIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivityIfNeeded(redirectIntent, 0)
            finishWithFade()
        }
    }

    /** Finish Activity using FADE animation  */
    private fun finishWithFade() {
        finish()
        ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.UP)
    }
}