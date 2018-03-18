/****************************************************************************************
 * Copyright (c) 2009 Andrew Dubya <andrewdubya></andrewdubya>@gmail.com>                              *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul></nicolas.raoul>@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu></edu.zasu>@gmail.com>                                   *
 * Copyright (c) 2009 Daniel Svard <daniel.svard></daniel.svard>@gmail.com>                             *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>
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

package com.ichi2.anki.deckpicker

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ShareCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.*
import com.ichi2.anki.account.MyAccountActivity
import com.ichi2.anki.cardbrowser.CardBrowser
import com.ichi2.anki.studyoptions.StudyOptionsFragment.StudyOptionsListener
import com.ichi2.anki.deckpicker.model.TaskData
import com.ichi2.anki.dialogs.*
import com.ichi2.anki.exception.ConfirmModSchemaException
import com.ichi2.anki.exception.DeckRenameException
import com.ichi2.anki.flashcardviewer.Reviewer
import com.ichi2.anki.info.Info
import com.ichi2.anki.junkdrawer.BackupManager
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.junkdrawer.receiver.SdCardReceiver
import com.ichi2.anki.stats.AnkiStatsTaskHandler
import com.ichi2.anki.studyoptions.StudyOptionsActivity
import com.ichi2.anki.studyoptions.StudyOptionsFragment
import com.ichi2.anki.junkdrawer.widgets.DeckAdapter
import com.ichi2.utils.async.Connection
import com.ichi2.utils.async.Connection.Payload
import com.ichi2.utils.async.DeckTask
import com.ichi2.utils.compat.CompatHelper
import com.ichi2.libanki.Sched
import com.ichi2.libanki.Utils
import com.ichi2.anki.preferences.DeckOptions
import com.ichi2.anki.preferences.FilteredDeckOptions
import com.ichi2.utils.themes.StyledProgressDialog
import com.ichi2.ui.DividerItemDecoration
import com.ichi2.utils.VersionUtils
import com.ichi2.utils.dismissExisting
import com.ichi2.widget.WidgetStatus
import kotlinx.android.synthetic.main.deck_picker.*
import kotlinx.android.synthetic.main.floating_add_button.*
import org.json.JSONException
import timber.log.Timber
import java.io.File
import java.util.*


// TODO: Move logic and state to ViewModel
// TODO: Add tablet check extension
// TODO: Add a better prefs
// TODO: Add instrumentation tests
class DeckPickerActivity : NavigationDrawerActivity(), DeckPickerView, StudyOptionsListener,
        SyncErrorDialog.SyncErrorDialogListener, ImportDialog.ImportDialogListener,
        MediaCheckDialog.MediaCheckDialogListener, ExportDialog.ExportDialogListener,
        ActivityCompat.OnRequestPermissionsResultCallback, CustomStudyDialog.CustomStudyListener {

    private var mProgressDialog: ProgressDialog? = null
    private var mStudyoptionsFrame: View? = null
    private var mRecyclerViewLayoutManager: LinearLayoutManager? = null
    private lateinit var mDeckListAdapter: DeckAdapter

    private var mUnmountReceiver: BroadcastReceiver? = null

    // flag keeping track of when the app has been paused
    private var mActivityPaused = false

    lateinit var viewModel: DeckPickerViewModel

    // Snackbar for progress updates
    private var snack: CustomSnackbar? = null

    /**
     * Flag to indicate whether the activity will perform a sync in its onResume.
     * Since syncing closes the database, this flag allows us to avoid doing any
     * work in onResume that might use the database and go straight to syncing.
     */
    private var mSyncOnResume = false

    //region Lifecycles

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")

        // Setup ViewModel
        viewModel = ViewModelProviders.of(this).get(DeckPickerViewModel::class.java)
        viewModel.deckPickerView = this

        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)

        // Open Collection on UI thread while splash screen is showing
        val colOpen = viewModel.firstCollectionOpen()

        // Then set theme and content view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)

        val mainView = findViewById<View>(android.R.id.content)

        // check, if tablet layout
        mStudyoptionsFrame = findViewById(R.id.studyoptions_fragment)

        // set protected variable from NavigationDrawerActivity
        mFragmented = mStudyoptionsFrame != null && mStudyoptionsFrame!!.visibility == View.VISIBLE

        registerExternalStorageListener()

        // create inherited navigation drawer layout here so that it can be used by parent class
        initNavigationDrawer(mainView)
        title = resources.getString(R.string.app_name)


        mRecyclerViewLayoutManager = LinearLayoutManager(this)
        filesRecyclerView.addItemDecoration(DividerItemDecoration(this))
        filesRecyclerView.layoutManager = mRecyclerViewLayoutManager

        // create and set an adapter for the RecyclerView
        mDeckListAdapter = DeckAdapter(layoutInflater, this)
        mDeckListAdapter.setDeckClickListener(OnClickListener {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        })
        mDeckListAdapter.setDeckClickListener(OnClickListener { viewModel.selectDeck(it.tag as Long) })
        mDeckListAdapter.setCountsClickListener(OnClickListener { viewModel.countsClick(it.tag as Long) })
        mDeckListAdapter.setDeckExpanderClickListener(OnClickListener { viewModel.deckExpander(it.tag as Long) })
        mDeckListAdapter.setDeckLongClickListener(View.OnLongClickListener { viewModel.deckLongClick(it.tag as Long) })
        filesRecyclerView.adapter = mDeckListAdapter

        refreshLayout.setDistanceToTriggerSync(SWIPE_TO_SYNC_TRIGGER_DISTANCE)
        refreshLayout.setOnRefreshListener {
            refreshLayout.isRefreshing = false
            sync()
        }

        // Setup the FloatingActionButtons
        addContentMenu.findViewById<View>(R.id.fab_expand_menu_button).contentDescription = getString(R.string.menu_add)
        configureFloatingActionsMenu()

        // Hide the fragment until the counts have been loaded so that the Toolbar fills the whole screen on tablets
        if (mFragmented) {
            mStudyoptionsFrame!!.visibility = View.GONE
        }

        if (colOpen) {
            // Show any necessary dialogs (e.g. changelog, special messages, etc)
            showStartupScreensAndDialogs(preferences, 0)
        } else {
            // Show errorSnackbar dialogs
            if (!CollectionHelper.hasStorageAccessPermission(this)) {
                // This case is handled by onRequestPermissionsResult() so don't need to do anything
            } else if (!AnkiDroidApp.isSdCardMounted()) {
                // SD card not mounted
                onSdCardNotMounted()
            } else if (!CollectionHelper.isCurrentAnkiDroidDirAccessible(this)) {
                // AnkiDroid directory inaccessible
                val i = CompatHelper.compat!!.getPreferenceSubscreenIntent(this, "com.ichi2.anki.prefs.advanced")
                startActivityForResult(i, REQUEST_PATH_UPDATE)
                Toast.makeText(this, R.string.directory_inaccessible, Toast.LENGTH_LONG).show()
            } else if (CollectionHelper.getInstance().exceededCursorSizeLimit(this)) {
                showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CURSOR_SIZE_LIMIT_EXCEEDED)
            } else {
                showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED)
            }
        }
    }


    override fun onAttachedToWindow() {

        if (!mFragmented) {
            val window = window
            window.setFormat(PixelFormat.RGBA_8888)
        }
    }


    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
        mActivityPaused = false
        if (mSyncOnResume) {
            sync()
            mSyncOnResume = false
        } else if (colIsOpen()) {
            selectNavigationItem(R.id.nav_decks)
            updateDeckList()
            title = resources.getString(R.string.app_name)
        }
    }


    override fun onPause() {
        Timber.d("onPause()")
        mActivityPaused = true
        super.onPause()
    }


    override fun onStop() {
        Timber.d("onStop()")
        super.onStop()
        if (colIsOpen()) {
            WidgetStatus.update(this)
            UIUtils.saveCollectionInBackground()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver)
        }
        dismissProgressDialog()
        Timber.d("onDestroy()")
    }

    //endregion


    //region Instance State Handling

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putLong("viewModel.contextMenuDeckId", viewModel.contextMenuDeckId)
    }


    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.contextMenuDeckId = savedInstanceState.getLong("viewModel.contextMenuDeckId")
    }

    //endregion


    //region Menu Handling

    private fun configureFloatingActionsMenu() {
        addDeckAction.setOnClickListener({
            addContentMenu.collapse()

            val editText = EditText(this@DeckPickerActivity).apply { setSingleLine(true) }

            AlertDialog.Builder(this@DeckPickerActivity)
                    .setTitle(R.string.new_deck)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val deckName = editText.text.toString()
                        Timber.i("DeckPickerActivity:: Creating new deck...")
                        col!!.decks.id(deckName, true)
                        updateDeckList()
                    }
                    .setView(editText)
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        })
        addSharedAction.setOnClickListener {
            addContentMenu.collapse()
            addSharedDeck()
        }
        addNoteAction.setOnClickListener {
            addContentMenu.collapse()
            addNote()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Null check to prevent crash when col inaccessible
        if (CollectionHelper.getInstance().getColSafe(this) == null) {
            return false
        }
        // Show / hide undo
        if (mFragmented || !viewModel.col!!.undoAvailable()) {
            menu.findItem(R.id.action_undo).isVisible = false
        } else {
            menu.findItem(R.id.action_undo).isVisible = true
            val undo = resources.getString(R.string.studyoptions_congrats_undo, col!!.undoName(resources))
            menu.findItem(R.id.action_undo).title = undo
        }
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.deck_picker, menu)
        val sdCardAvailable = AnkiDroidApp.isSdCardMounted()
        menu.findItem(R.id.action_sync).isEnabled = sdCardAvailable
        menu.findItem(R.id.action_new_filtered_deck).isEnabled = sdCardAvailable
        menu.findItem(R.id.action_check_database).isEnabled = sdCardAvailable
        menu.findItem(R.id.action_check_media).isEnabled = sdCardAvailable
        menu.findItem(R.id.action_empty_cards).isEnabled = sdCardAvailable

        // Hide import, export, and restore backup on ChromeOS as users
        // don't have access to the file system.
        if (CompatHelper.isChromebook) {
            menu.findItem(R.id.action_restore_backup).isVisible = false
            menu.findItem(R.id.action_import).isVisible = false
            menu.findItem(R.id.action_export).isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        when (item.itemId) {
            R.id.action_undo -> {
                Timber.i("DeckPickerActivity:: Undo button pressed")
                undo()
                return true
            }

            R.id.action_sync -> {
                Timber.i("DeckPickerActivity:: Sync button pressed")
                sync()
                return true
            }

            R.id.action_import -> {
                Timber.i("DeckPickerActivity:: Import button pressed")
                showImportDialog(ImportDialog.DIALOG_IMPORT_HINT)
                return true
            }

            R.id.action_new_filtered_deck -> {
                Timber.i("DeckPickerActivity:: New filtered deck button pressed")
                val mDialogEditText = EditText(this@DeckPickerActivity)
                val names = col!!.decks.allNames()
                var n = 1
                var name = String.format(Locale.getDefault(), "%s %d", resources.getString(R.string.filtered_deck_name), n)
                while (names.contains(name)) {
                    n++
                    name = String.format(Locale.getDefault(), "%s %d", resources.getString(R.string.filtered_deck_name), n)
                }
                mDialogEditText.setText(name)
                // mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
                AlertDialog.Builder(this@DeckPickerActivity)
                        .setTitle(resources.getString(R.string.new_deck))
                        .setView(mDialogEditText)
                        .setPositiveButton(resources.getString(R.string.create)) { _, _ ->
                            val filteredDeckName = mDialogEditText.text.toString()
                            Timber.i("DeckPickerActivity:: Creating filtered deck...")
                            col!!.decks.newDyn(filteredDeckName)
                            openStudyOptions(true)
                        }
                        .setNegativeButton(resources.getString(R.string.dialog_cancel), null)
                        .show()
                return true
            }

            R.id.action_check_database -> {
                Timber.i("DeckPickerActivity:: Check database button pressed")
                showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_DATABASE_CHECK)
                return true
            }

            R.id.action_check_media -> {
                Timber.i("DeckPickerActivity:: Check media button pressed")
                showMediaCheckDialog(MediaCheckDialog.DIALOG_CONFIRM_MEDIA_CHECK)
                return true
            }

            R.id.action_empty_cards -> {
                Timber.i("DeckPickerActivity:: Empty cards button pressed")
                handleEmptyCards()
                return true
            }

            R.id.action_model_browser_open -> {
                Timber.i("DeckPickerActivity:: Model browser button pressed")
                val noteTypeBrowser = Intent(this, ModelBrowser::class.java)
                startActivityForResult(noteTypeBrowser, 0)
                return true
            }

            R.id.action_restore_backup -> {
                Timber.i("DeckPickerActivity:: Restore from backup button pressed")
                showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_RESTORE_BACKUP)
                return true
            }

            R.id.action_export -> {
                Timber.i("DeckPickerActivity:: Export collection button pressed")
                val msg = resources.getString(R.string.confirm_apkg_export)
                showDialogFragment(ExportDialog.newInstance(msg))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    //endregion


    //region System Callbacks

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        when {
            resultCode == RESULT_MEDIA_EJECTED -> {
                onSdCardNotMounted()
                return
            }
            resultCode == RESULT_DB_ERROR -> {
                handleDbError()
                return
            }
            requestCode == REPORT_ERROR -> showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(baseContext), 4)
            requestCode == SHOW_INFO_WELCOME || requestCode == SHOW_INFO_NEW_VERSION -> if (resultCode == Activity.RESULT_OK) {
                showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(baseContext),
                        if (requestCode == SHOW_INFO_WELCOME) 2 else 3)
            } else {
                finish()
            }
            requestCode == LOG_IN_FOR_SYNC && resultCode == Activity.RESULT_OK -> mSyncOnResume = true
            (requestCode == REQUEST_REVIEW || requestCode == SHOW_STUDYOPTIONS) && resultCode == Reviewer.RESULT_NO_MORE_CARDS -> {
                // Show a message when reviewing has finished
                val studyOptionsCounts = col!!.sched.counts()
                if (studyOptionsCounts[0] + studyOptionsCounts[1] + studyOptionsCounts[2] == 0) {
                    UIUtils.showSimpleSnackbar(this, R.string.studyoptions_congrats_finished, false)
                } else {
                    UIUtils.showSimpleSnackbar(this, R.string.studyoptions_no_cards_due, false)
                }
            }
            requestCode == REQUEST_BROWSE_CARDS -> // Store the selected deck after opening browser
                if (intent != null && intent.getBooleanExtra("allDecksSelected", false)) {
                    AnkiDroidApp.getSharedPrefs(this).edit().putLong("browserDeckIdFromDeckPicker", -1L).apply()
                } else {
                    val selectedDeck = col!!.decks.selected()
                    AnkiDroidApp.getSharedPrefs(this).edit().putLong("browserDeckIdFromDeckPicker", selectedDeck).apply()
                }
            requestCode == REQUEST_PATH_UPDATE -> // The collection path was inaccessible on startup so just close the activity and let user restart
                finish();
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_STORAGE_PERMISSION && permissions.size == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(this), 0)
            } else {
                // User denied access to the SD card so show errorSnackbar toast and finish activity
                Toast.makeText(this, R.string.directory_inaccessible, Toast.LENGTH_LONG).show()
                finish();
                // Open the Android settings page for our app so that the user can grant the missing permission
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }


    override fun onBackPressed() {
        if (isDrawerOpen) {
            super.onBackPressed()
        } else {
            Timber.i("Back key pressed")
            if (addContentMenu != null && addContentMenu!!.isExpanded) {
                addContentMenu!!.collapse()
            } else {
                automaticSync()
                finish();
            }
        }
    }

    //endregion


    private fun automaticSync() {
        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)

        // Check whether the option is selected, the user is signed in and last sync was AUTOMATIC_SYNC_TIME ago
        // (currently 10 minutes)
        val hkey = preferences.getString("hkey", "")
        val lastSyncTime = preferences.getLong("lastSyncTime", 0)
        if (hkey!!.isNotEmpty() && preferences.getBoolean("automaticSyncMode", false) &&
                Connection.isOnline() && Utils.intNow(1000) - lastSyncTime > AUTOMATIC_SYNC_MIN_INTERVAL) {
            sync()
        }
    }


    // ----------------------------------------------------------------------------
    // LISTENERS
    // ----------------------------------------------------------------------------


    override fun collapseActionsMenu() {
        if (addContentMenu != null && addContentMenu!!.isExpanded) {
            addContentMenu!!.collapse()
        }
    }

    override fun notifyDataSetChanged() {
        if (mFragmented || !CompatHelper.isLollipop) {
            // Calling notifyDataSetChanged() will update the color of the selected deck.
            // This interferes with the ripple effect, so we don't do it if lollipop and not tablet view
            mDeckListAdapter.notifyDataSetChanged()
        }
    }

    //region Dialogs
    override fun showDialogFragment(deckId: Long) = showDialogFragment(DeckPickerContextMenu.newInstance(deckId))

    override fun showProgress(msg: String) {
        if (snack == null)
            snack = CustomSnackbar.make(root_layout, Snackbar.LENGTH_INDEFINITE)
        snack?.setText(msg)
        if (!snack!!.isShown)
            snack?.show()
    }

    override fun showProgress(msg: Int) {
        showProgress(resources.getString(msg))
    }

    override fun dismissProgressSnackbar() = snack?.dismiss() ?: Unit

    override fun dismissStyledProgressDialog() {
        supportFragmentManager.dismissExisting<StyledProgressDialog>()
    }

    override fun dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }

    override fun showSimpleMessageDialogLocal(msg: String) {
//        showSimpleMessageDialogLocal(msg)
        showSimpleMessageDialog(msg)
    }

    override fun showSimpleMessageDialogLocal(msg: Int) {
        showSimpleMessageDialogLocal(resources.getString(msg))
    }

    override fun showExportCompleteDialog(exportPath: String) {
        DeckPickerExportCompleteDialog.show(supportFragmentManager, exportPath)
    }

    // TODO: Move to Snackbar with progress bar
    override fun setProgressDialogMessage(message: String?) {
//        (supportFragmentManager.findFragmentByTag(StyledProgressDialog::class.java.simpleName) as StyledProgressDialog).theDialog?.
//        mProgressDialog?.setMessage(message)
    }

    override fun showSimpleSnackbar(msg: String) {
        UIUtils.showSimpleSnackbar(this@DeckPickerActivity, msg, false)
    }

    //endregion

    override fun showThemedToast(msgResId: Int) {
        UIUtils.showThemedToast(this@DeckPickerActivity, resources.getString(msgResId), true)
    }

    // TODO: Move all permissions related calls to parent
    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                DeckPickerActivity.REQUEST_STORAGE_PERMISSION)
    }

    //region listeners

    private val mSyncListener = object : Connection.CancellableTaskListener {
        private var currentMessage: String = ""
        private var countUp: Long = 0
        private var countDown: Long = 0

        override fun onDisconnected() {
            showSyncLogMessage(R.string.youre_offline, "")
        }

        override fun onCancelled() {
            mProgressDialog!!.dismiss()
            showSyncLogMessage(R.string.sync_cancelled, "")
            // update deck list in case sync was cancelled during media sync and main sync was actually successful
            updateDeckList()
        }

        override fun onPreExecute() {
            countUp = 0
            countDown = 0
            // Store the current time so that we don't bother the user with a sync prompt for another 10 minutes
            // Note: getLs() in Libanki doesn't take into account the case when no changes were found, or sync cancelled
            val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
            val syncStartTime = System.currentTimeMillis()
            preferences.edit().putLong("lastSyncTime", syncStartTime).apply()

            if (mProgressDialog == null || !mProgressDialog!!.isShowing) {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, resources.getString(R.string.sync_title),
                        resources.getString(R.string.sync_title) + "\n"
                                + resources.getString(R.string.sync_up_down_size, countUp, countDown),
                        false)

                // Override the back key so that the user can cancel a sync which is in progress
                mProgressDialog!!.setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
                    // Make sure our method doesn't get called twice
                    if (event.action != KeyEvent.ACTION_DOWN) {
                        return@OnKeyListener true
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK && Connection.isCancellable() &&
                            !Connection.getIsCancelled()) {
                        // If less than 2s has elapsed since sync started then don't ask for confirmation
                        if (System.currentTimeMillis() - syncStartTime < 2000) {
                            Connection.cancel()
                            mProgressDialog?.setMessage(resources.getString(R.string.sync_cancel_message))
//                            mProgressDialog!!.setContent(R.string.sync_cancel_message)
                            return@OnKeyListener true
                        }
                        // Show confirmation dialog to check if the user wants to cancel the sync
                        val builder = MaterialDialog.Builder(mProgressDialog!!.context)
                        builder.content(R.string.cancel_sync_confirm)
                                .cancelable(false)
                                .positiveText(R.string.ok)
                                .negativeText(R.string.continue_sync)
                                .callback(object : MaterialDialog.ButtonCallback() {
                                    override fun onPositive(dialog: MaterialDialog?) {
                                        mProgressDialog?.setMessage(resources.getString(R.string.sync_cancel_message))
//                                        mProgressDialog!!.setContent(R.string.sync_cancel_message)
                                        Connection.cancel()
                                    }
                                })
                        builder.show()
                        true
                    } else {
                        false
                    }
                })
            }
        }

        override fun onProgressUpdate(vararg values: Any) {
            val res = resources
            if (values[0] is Boolean) {
                // This is the part Download missing media of syncing
                // TODO: Figure out wtf is happening here
//                val total = values[1] as Int
//                val done = values[2] as Int
//                values[0] = values[3]
//                values[1] = resources.getString(R.string.sync_downloading_media, done, total)
            } else if (values[0] is Int) {
                val id = values[0] as Int
                if (id != 0) {
                    currentMessage = resources.getString(id)
                }
                if (values.size >= 3) {
                    countUp = values[1] as Long
                    countDown = values[2] as Long
                }
            } else if (values[0] is String) {
                currentMessage = values[0] as String
                if (values.size >= 3) {
                    countUp = values[1] as Long
                    countDown = values[2] as Long
                }
            }
            if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                // mProgressDialog.setTitle((String) values[0]);
                mProgressDialog?.setMessage(currentMessage + "\n"
                        + res
                        .getString(R.string.sync_up_down_size, countUp / 1024, countDown / 1024))
//                mProgressDialog!!.setContent(currentMessage + "\n"
//                        + res
//                        .getString(R.string.sync_up_down_size, countUp / 1024, countDown / 1024))
            }
        }

        override fun onPostExecute(data: Payload) {
            refreshLayout.isRefreshing = false
            var dialogMessage = ""
            val syncMessage = data.message
            Timber.d("Sync Listener onPostExecute()")
            try {
                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Could not dismiss mProgressDialog. The Activity must have been destroyed while the AsyncTask was running")
            }

            if (!data.success) {
                val result = data.result as Array<Any>
                if (result[0] is String) {
                    val resultType = result[0] as String
                    when (resultType) {
                        "badAuth" -> {
                            // delete old auth information
                            AnkiDroidApp.getSharedPrefs(baseContext).edit().apply {
                                putString("username", "")
                                putString("hkey", "")
                                apply()
                            }
                            // then show not logged in dialog
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC)
                        }
                        "noChanges" -> // show no changes message, use false flag so we don't show "sync errorSnackbar" as the Dialog title
                            showSyncLogMessage(R.string.sync_no_changes_message, "")
                        "clockOff" -> {
                            val diff = result[1] as Long
                            dialogMessage = if (diff >= 86100) {
                                // The difference if more than a day minus 5 minutes acceptable by ankiweb errorSnackbar
                                resources.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                        resources.getString(R.string.sync_log_clocks_unsynchronized_date))
                            } else if (Math.abs(diff % 3600.0 - 1800.0) >= 1500.0) {
                                // The difference would be within limit if we adjusted the time by few hours
                                // It doesn't work for all timezones, but it covers most and it's a guess anyway
                                resources.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                        resources.getString(R.string.sync_log_clocks_unsynchronized_tz))
                            } else {
                                resources.getString(R.string.sync_log_clocks_unsynchronized, diff, "")
                            }
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "fullSync" -> if (col!!.isEmpty) {
                            // don't prompt user to resolve sync conflict if local collection empty
                            sync("download")
                            // TODO: Also do reverse check to see if AnkiWeb collection is empty if Anki Desktop
                            // implements it
                        } else {
                            // If can't be resolved then automatically then show conflict resolution dialog
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_CONFLICT_RESOLUTION)
                        }
                        "dbError", "basicCheckFailed" -> {
                            val repairUrl = resources.getString(R.string.repair_deck)
                            dialogMessage = resources.getString(R.string.sync_corrupt_database, repairUrl)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "overwriteError" -> {
                            dialogMessage = resources.getString(R.string.sync_overwrite_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "remoteDbError" -> {
                            dialogMessage = resources.getString(R.string.sync_remote_db_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "sdAccessError" -> {
                            dialogMessage = resources.getString(R.string.sync_write_access_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "finishError" -> {
                            dialogMessage = resources.getString(R.string.sync_log_finish_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "connectionError" -> {
                            dialogMessage = resources.getString(R.string.sync_connection_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "IOException" -> handleDbError()
                        "genericError" -> {
                            dialogMessage = resources.getString(R.string.sync_generic_error)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "OutOfMemoryError" -> {
                            dialogMessage = resources.getString(R.string.error_insufficient_memory)
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "sanityCheckError" -> {
                            dialogMessage = resources.getString(R.string.sync_sanity_failed)
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_SANITY_ERROR,
                                    viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        "serverAbort" -> // syncMsg has already been set above, no need to fetch it here.
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        "mediaSyncServerError" -> {
                            dialogMessage = resources.getString(R.string.sync_media_error_check)
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_MEDIA_SYNC_ERROR,
                                    viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                        else -> {
                            dialogMessage = if (result.size > 1 && result[1] is Int) {
                                val type = result[1] as Int
                                when (type) {
                                    501 -> resources.getString(R.string.sync_error_501_upgrade_required)
                                    503 -> resources.getString(R.string.sync_too_busy)
                                    409 -> resources.getString(R.string.sync_error_409)
                                    else -> resources.getString(R.string.sync_log_error_specific,
                                            Integer.toString(type), result[2])
                                }
                            } else if (result[0] is String) {
                                resources.getString(R.string.sync_log_error_specific, Integer.toString(-1), result[0])
                            } else {
                                resources.getString(R.string.sync_generic_error)
                            }
                            showSyncErrorMessage(viewModel.joinSyncMessages(dialogMessage, syncMessage))
                        }
                    }
                }
            } else {
                // Sync was successful!
                if (data.data[2] != null && data.data[2] != "") {
                    // There was a media errorSnackbar, so show it
                    val message = resources.getString(R.string.sync_database_acknowledge) + "\n\n" + data.data[2]
                    showSimpleMessageDialog(message)
                } else if (data.data.isNotEmpty() && data.data[0] is String
                        && (data.data[0] as String).length > 0) {
                    // A full sync occurred
                    val dataString = data.data[0] as String
                    when (dataString) {
                        "upload" -> showSyncLogMessage(R.string.sync_log_uploading_message, syncMessage)
                        "download" -> showSyncLogMessage(R.string.sync_log_downloading_message, syncMessage)
                        else -> showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage)
                    }
                } else {
                    // Regular sync completed successfully
                    showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage)
                }
                updateDeckList()
                WidgetStatus.update(this@DeckPickerActivity)
                if (mFragmented) {
                    try {
                        loadStudyOptionsFragment(false)
                    } catch (e: IllegalStateException) {
                        // Activity was stopped or destroyed when the sync finished. Losing the
                        // fragment here is fine since we build a fresh fragment on resume anyway.
                        Timber.w(e, "Failed to load StudyOptionsFragment after sync.")
                    }

                }
            }
        }
    }


    val fragment: StudyOptionsFragment?
        get() {
            val frag = supportFragmentManager.findFragmentById(R.id.studyoptions_fragment)
            return if (frag != null && frag is StudyOptionsFragment) {
                frag
            } else null
        }


    // TODO: Finish
    suspend fun progressRebuildCram(deckId: Long) {
        showProgressBar()

        Timber.d("doInBackgroundRebuildCram")
        val col = CollectionHelper.getInstance().getCol(DeckPicker@ this)
        col!!.sched.rebuildDyn(col.decks.selected())
        updateDeckList()
        if (mFragmented)
            loadStudyOptionsFragment(false)
    }

    /**
     * Show progress bars and rebuild deck list on completion
     */
    private var mSimpleProgressListener: DeckTask.TaskListener = object : DeckTask.TaskListener() {

        override fun onPreExecute() {
            showProgressBar()
        }


        override fun onPostExecute(result: TaskData) {
            updateDeckList()
            if (mFragmented) {
                loadStudyOptionsFragment(false)
            }
        }


        override fun onProgressUpdate(vararg values: TaskData) {}


        override fun onCancelled() {}
    }

    private var mSnackbarShowHideCallback: Snackbar.Callback = object : Snackbar.Callback() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            if (!CompatHelper.isHoneycomb) {
                addNoteAction.isEnabled = true
            }
        }

        override fun onShown(snackbar: Snackbar?) {
            if (!CompatHelper.isHoneycomb) {
                addNoteAction.isEnabled = false
            }
        }
    }
//endregion


    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------


    /**
     * Perform the following tasks:
     * Automatic backup
     * loadStudyOptionsFragment() if tablet
     * Automatic sync
     */
    private fun onFinishedStartup() {
        // create backup in background if needed
        BackupManager.performBackupInBackground(col!!.path)

        // Force a full sync if flag was set in upgrade path, asking the user to confirm if necessary
        if (viewModel.recommendFullSync) {
            viewModel.recommendFullSync = false
            try {
                col!!.modSchema()
            } catch (e: ConfirmModSchemaException) {
                // If libanki determines it's necessary to confirm the full sync then show a confirmation dialog
                // We have to show the dialog via the DialogHandler since this method is called via a Loader
                val handlerMessage = Message.obtain()
                handlerMessage.what = DialogHandler.MSG_SHOW_FORCE_FULL_SYNC_DIALOG
                val handlerMessageData = Bundle()
                handlerMessageData.putString("message", resources.getString(R.string.full_sync_confirmation_upgrade) +
                        "\n\n" + resources.getString(R.string.full_sync_confirmation))
                handlerMessage.data = handlerMessageData
                dialogHandler.sendMessage(handlerMessage)
            }

        }
        // Open StudyOptionsFragment if in fragmented mode
        if (mFragmented) {
            loadStudyOptionsFragment(false)
        }
        automaticSync()
    }

    override fun onCollectionLoadError() {
        dialogHandler.sendEmptyMessage(DialogHandler.MSG_SHOW_COLLECTION_LOADING_ERROR_DIALOG)
    }

    fun addNote() {
        val intent = Intent(this@DeckPickerActivity, NoteEditor::class.java)
        intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER)
        startActivityForResult(intent, ADD_NOTE)
    }

    private fun showStartupScreensAndDialogs(preferences: SharedPreferences, skip: Int) {
        if (!BackupManager.enoughDiscSpace(CollectionHelper.getCurrentAnkiDroidDirectory(this))) {
            // Not enough space to do backup
            showDialogFragment(DeckPickerNoSpaceLeftDialog.newInstance())
        } else if (preferences.getBoolean("noSpaceLeft", false)) {
            // No space left
            showDialogFragment(DeckPickerBackupNoSpaceLeftDialog.newInstance())
            preferences.edit().remove("noSpaceLeft").apply()
        } else if (preferences.getString("lastVersion", "") == "") {
            // Fresh install
            preferences.edit().putString("lastVersion", VersionUtils.pkgVersionName).apply()
            onFinishedStartup()
        } else if (skip < 2 && preferences.getString("lastVersion", "") != VersionUtils.pkgVersionName) {
            // AnkiDroid is being updated and a collection already exists. We check if we are upgrading
            // to a version that contains additions to the database integrity check routine that we would
            // like to runImport on all collections. A missing version number is assumed to be a fresh
            // installation of AnkiDroid and we don't runImport the check.
            val current = VersionUtils.pkgVersionCode
            val previous: Int
            if (!preferences.contains("lastUpgradeVersion")) {
                // Fresh install
                previous = current
            } else {
                previous = try {
                    preferences.getInt("lastUpgradeVersion", current)
                } catch (e: ClassCastException) {
                    // Previous versions stored this as a string.
                    val s = preferences.getString("lastUpgradeVersion", "")
                    // The last version of AnkiDroid that stored this as a string was 2.0.2.
                    // We manually set the version here, but anything older will force a DB
                    // check.
                    if (s == "2.0.2") {
                        40
                    } else {
                        0
                    }
                }

            }
            preferences.edit().putInt("lastUpgradeVersion", current).apply()
            preferences.edit().remove("sentExceptionReports").apply()     // clear cache of sent exception reports
            // Delete the media database made by any version before 2.3 beta due to upgrade errors.
            // It is rebuilt on the next sync or media check
            if (previous < 20300200) {
                val mediaDb = File(CollectionHelper.getCurrentAnkiDroidDirectory(this), "collection.media.ad.db2")
                if (mediaDb.exists()) {
                    mediaDb.delete()
                }
            }
            // Recommend the user to do a full-sync if they're upgrading from before 2.3.1beta8
            if (previous < 20301208) {
                viewModel.recommendFullSync = true
            }

            // Fix "font-family" definition in templates created by AnkiDroid before 2.6alhpa23
            if (previous < 20600123) {
                try {
                    val models = col!!.models
                    for (m in models.all()) {
                        val css = m.getString("css")
                        if (css.contains("font-familiy")) {
                            m.put("css", css.replace("font-familiy", "font-family"))
                            models.save(m)
                        }
                    }
                    models.flush()
                } catch (e: JSONException) {
                    Timber.e(e, "Failed to upgrade css definitions.")
                }

            }

            // Check if preference upgrade or database check required, otherwise go to new feature screen
            val upgradePrefsVersion = AnkiDroidApp.CHECK_PREFERENCES_AT_VERSION
            val upgradeDbVersion = AnkiDroidApp.CHECK_DB_AT_VERSION

            if (previous < upgradeDbVersion || previous < upgradePrefsVersion) {
                if (upgradePrefsVersion in (previous + 1)..current) {
                    Timber.d("Upgrading preferences")
                    CompatHelper.removeHiddenPreferences(this.applicationContext)
                    upgradePreferences(previous)
                }
                // Integrity check loads asynchronously and then restart deckpicker when finished
                if (upgradeDbVersion in (previous + 1)..current) {
                    integrityCheck()
                } else if (upgradePrefsVersion in (previous + 1)..current) {
                    // If integrityCheck() doesn't occur, but we did update preferences we should restart DeckPickerActivity to
                    // proceed
                    restartActivity()
                }
            } else {
                // If no changes are required we go to the new features activity
                // There the "lastVersion" is set, so that this code is not reached again
                if (VersionUtils.isReleaseVersion) {
                    val infoIntent = Intent(this, Info::class.java)
                    infoIntent.putExtra(Info.TYPE_EXTRA, Info.TYPE_NEW_VERSION)

                    if (skip != 0) {
                        startActivityForResult(infoIntent, SHOW_INFO_NEW_VERSION)
                    } else {
                        startActivityForResult(infoIntent, SHOW_INFO_NEW_VERSION)
                    }
                } else {
                    // Don't show new features dialog for development builds
                    preferences.edit().putString("lastVersion", VersionUtils.pkgVersionName).apply()
                    val ver = resources.getString(R.string.updated_version, VersionUtils.pkgVersionName)
                    UIUtils.showSnackbar(this, ver, true, -1, null, findViewById(R.id.root_layout), null)
                    showStartupScreensAndDialogs(preferences, 2)
                }
            }
        } else {
            // This is the main call when there is nothing special required
            onFinishedStartup()
        }
    }


    private fun upgradePreferences(previousVersionCode: Int) {
        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
        // clear all prefs if super old version to prevent any errors
        if (previousVersionCode < 20300130) {
            preferences.edit().clear().apply()
        }
        // when upgrading from before 2.5alpha35
        if (previousVersionCode < 20500135) {
            // Card zooming behaviour was changed the preferences renamed
            val oldCardZoom = preferences.getInt("relativeDisplayFontSize", 100)
            val oldImageZoom = preferences.getInt("relativeImageSize", 100)
            preferences.edit().putInt("cardZoom", oldCardZoom).apply()
            preferences.edit().putInt("imageZoom", oldImageZoom).apply()
            if (!preferences.getBoolean("useBackup", true)) {
                preferences.edit().putInt("backupMax", 0).apply()
            }
            preferences.edit().remove("useBackup").apply()
            preferences.edit().remove("intentAdditionInstantAdd").apply()
        }

        if (preferences.contains("fullscreenReview")) {
            // clear fullscreen flag as we use a integer
            try {
                val old = preferences.getBoolean("fullscreenReview", false)
                preferences.edit().putString("fullscreenMode", if (old) "1" else "0").apply()
            } catch (e: ClassCastException) {
                // TODO:  can remove this catch as it was only here to fix an errorSnackbar in the betas
                preferences.edit().remove("fullscreenMode").apply()
            }

            preferences.edit().remove("fullscreenReview").apply()
        }
    }

    private fun undo() {
        val undoReviewString = resources.getString(R.string.undo_action_review)
        val isReview = undoReviewString == col!!.undoName(resources)
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_UNDO, object : DeckTask.TaskListener() {
            override fun onCancelled() {
                hideProgressBar()
            }

            override fun onPreExecute() {
                showProgressBar()
            }

            override fun onPostExecute(result: TaskData) {
                hideProgressBar()
                if (isReview) {
                    openReviewer()
                }
            }

            override fun onProgressUpdate(vararg values: TaskData) {}
        })
    }


    // Show dialogs to deal with database loading issues etc
    fun showDatabaseErrorDialog(id: Int) {
        val newFragment = DatabaseErrorDialog.newInstance(id)
        showAsyncDialogFragment(newFragment)
    }


    override fun showMediaCheckDialog(dialogType: Int) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(dialogType))
    }

    override fun showMediaCheckDialog(dialogType: Int, checkList: List<List<String>>) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(dialogType, checkList))
    }


    /**
     * Show a specific sync errorSnackbar dialog
     * @param dialogType dialogType of dialog to show
     */
    override fun showSyncErrorDialog(dialogType: Int) {
        showSyncErrorDialog(dialogType, "")
    }

    /**
     * Show a specific sync errorSnackbar dialog
     * @param dialogType dialogType of dialog to show
     * @param message text to show
     */
    override fun showSyncErrorDialog(dialogType: Int, message: String) {
        val newFragment = SyncErrorDialog.newInstance(dialogType, message)
        showAsyncDialogFragment(newFragment)
    }

    /**
     * Show simple errorSnackbar dialog with just the message and OK button. Reload the activity when dialog closed.
     * @param message
     */
    private fun showSyncErrorMessage(message: String) {
        val title = resources.getString(R.string.sync_error)
        showSimpleMessageDialog(title, message, true)
    }

    /**
     * Show a simple snackbar message or notification if the activity is not in foreground
     * @param messageResource String resource for message
     */
    private fun showSyncLogMessage(messageResource: Int, syncMessage: String?) =
            if (mActivityPaused) {
                showSimpleNotification(resources.getString(R.string.app_name), resources.getString(messageResource))
            } else {
                if (syncMessage == null || syncMessage.isEmpty()) {
                    UIUtils.showSimpleSnackbar(this, messageResource, false)
                } else {
                    showSimpleMessageDialog(resources.getString(messageResource), syncMessage, false)
                }
            }


    override fun showImportDialog(id: Int) {
        showImportDialog(id, "")
    }


    override fun showImportDialog(id: Int, message: String) {
        val newFragment = ImportDialog.newInstance(id, message)
        showDialogFragment(newFragment)
    }

    fun onSdCardNotMounted() {
        UIUtils.showThemedToast(this, resources.getString(R.string.sd_card_not_mounted), false)
        finish();
    }


    // Callback method to handle repairing deck
    fun repairDeck() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_REPAIR_DECK, object : DeckTask.TaskListener() {

            override fun onPreExecute() {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, "",
                        resources.getString(R.string.backup_repair_deck_progress), false)
            }


            override fun onPostExecute(result: TaskData?) {
                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
                if (result == null || !result.boolean) {
                    UIUtils.showThemedToast(this@DeckPickerActivity, resources.getString(R.string.deck_repair_error), true)
                    onCollectionLoadError()
                }
            }


            override fun onProgressUpdate(vararg values: TaskData) {}


            override fun onCancelled() {}
        })
    }


    // Callback method to handle database integrity check
    fun integrityCheck() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_CHECK_DATABASE, object : DeckTask.TaskListener() {
            override fun onPreExecute() {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, "",
                        resources.getString(R.string.check_db_message), false)
            }


            override fun onPostExecute(result: TaskData?) {
                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
                if (result != null && result.boolean) {
                    val shrunk = Math.round(result.long / 1024.0)
                    val msg = if (shrunk > 0.0) {
                        String.format(Locale.getDefault(),
                                resources.getString(R.string.check_db_acknowledge_shrunk), shrunk.toInt())
                    } else {
                        resources.getString(R.string.check_db_acknowledge)
                    }
                    // Show result of database check and restart the app
                    showSimpleMessageDialog(msg, true)
                } else {
                    handleDbError()
                }
            }


            override fun onProgressUpdate(vararg values: TaskData) {}


            override fun onCancelled() {}
        })
    }


    override fun mediaCheck() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_CHECK_MEDIA, object : DeckTask.TaskListener() {
            override fun onPreExecute() {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, "",
                        resources.getString(R.string.check_media_message), false)
            }


            override fun onPostExecute(result: TaskData?) {
                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
                if (result != null && result.boolean) {
                    val checkList = result.objArray?.get(0) as List<List<String>>
                    showMediaCheckDialog(MediaCheckDialog.DIALOG_MEDIA_CHECK_RESULTS, checkList)
                } else {
                    showSimpleMessageDialog(resources.getString(R.string.check_media_failed))
                }
            }


            override fun onProgressUpdate(vararg values: TaskData) {}


            override fun onCancelled() {}
        })
    }


    override fun deleteUnused(unused: List<String>?) {
        val m = col!!.media
        for (fname in unused!!) {
            m.removeFile(fname)
        }
        showSimpleMessageDialog(String.format(resources.getString(R.string.check_media_deleted), unused.size))
    }


    fun exit() {
        CollectionHelper.getInstance().closeCollection(false)
        finish()
        System.exit(0)
    }


    fun handleDbError() {
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED)
    }


    fun restoreFromBackup(path: String) {
        importReplace(path)
    }


    // Helper function to check if there are any saved stacktraces
    fun hasErrorFiles(): Boolean = this.fileList().any { it.endsWith(".stacktrace") }


    // Sync with Anki Web
    override fun sync() {
        sync(null)
    }


    /**
     * The mother of all syncing attempts. This might be called from sync() as first attempt to sync a collection OR
     * from the mSyncConflictResolutionListener if the first attempt determines that a full-sync is required.
     *
     * @param conflict Either "upload" or "download", depending on the user's choice.
     */
    override fun sync(conflict: String?) {
        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
        val hkey = preferences.getString("hkey", "")
        if (hkey!!.isEmpty()) {
            refreshLayout!!.isRefreshing = false
            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC)
        } else {
            Connection.sync(mSyncListener,
                    Connection.Payload(arrayOf(hkey, preferences.getBoolean("syncFetchesMedia", true), conflict)))
        }
    }

    override fun loginToSyncServer() {
        val myAccount = Intent(this, MyAccountActivity::class.java)
        myAccount.putExtra("notLoggedIn", true)
        startActivityForResult(myAccount, LOG_IN_FOR_SYNC)
    }


    // Callback to import a file -- adding it to existing collection
    override fun importAdd(importPath: String) {
        viewModel.importAdd(importPath)
    }


    // Callback to import a file -- replacing the existing collection
    override fun importReplace(importPath: String) {
        viewModel.importReplace(importPath)
//        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_IMPORT_REPLACE, mImportReplaceListener, TaskData(importPath))
    }


    override fun exportApkg(path: String?, did: Long?, includeSched: Boolean, includeMedia: Boolean) {
        viewModel.exportApkg(path, did, includeSched, includeMedia)
    }

    override fun resolveIntent(uri: Uri, attachmentName: String) {
        val shareIntent = ShareCompat.IntentBuilder.from(this@DeckPickerActivity)
                .setType("application/apkg")
                .setStream(uri)
                .setSubject(resources.getString(R.string.export_email_subject, attachmentName))
                .setHtmlText(resources.getString(R.string.export_email_text))
                .intent

        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(shareIntent)
        } else {
            Timber.e("Could not find appropriate application to share apkg with")
            UIUtils.showThemedToast(this, resources.getString(R.string.apk_share_error), false)
        }
    }

    /**
     * Load a new studyOptionsFragment. If withDeckOptions is true, the deck options activity will
     * be loaded on top of it. Use this flag when creating a new filtered deck to allow the user to
     * modify the filter settings before being shown the fragment. The fragment itself will handle
     * rebuilding the deck if the settings change.
     */
    override fun loadStudyOptionsFragment(withDeckOptions: Boolean) {
        val details = StudyOptionsFragment.newInstance(withDeckOptions)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.studyoptions_fragment, details)
        ft.commit()
    }


    /**
     * Show a message when the SD card is ejected
     */
    private fun registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == SdCardReceiver.MEDIA_EJECT) {
                        onSdCardNotMounted()
                    } else if (intent.action == SdCardReceiver.MEDIA_MOUNT) {
                        restartActivity()
                    }
                }
            }
            val iFilter = IntentFilter()
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT)
            iFilter.addAction(SdCardReceiver.MEDIA_MOUNT)
            registerReceiver(mUnmountReceiver, iFilter)
        }
    }


    fun addSharedDeck() {
        openUrl(Uri.parse(resources.getString(R.string.shared_decks_url)))
    }


    private fun openStudyOptions(withDeckOptions: Boolean) {
        if (mFragmented) {
            // The fragment will show the study options screen instead of launching a new activity.
            loadStudyOptionsFragment(withDeckOptions)
        } else {
            val intent = Intent()
            intent.putExtra("withDeckOptions", withDeckOptions)
            intent.setClass(this, StudyOptionsActivity::class.java)
            startActivityForResult(intent, SHOW_STUDYOPTIONS)
        }
    }

    override fun openCardBrowser() {
        val cardBrowser = Intent(this, CardBrowser::class.java)
        cardBrowser.putExtra("selectedDeck", col!!.decks.selected())
        val lastDeckId = AnkiDroidApp.getSharedPrefs(this).getLong("browserDeckIdFromDeckPicker", -1L)
        cardBrowser.putExtra("defaultDeckId", lastDeckId)
        startActivityForResult(cardBrowser, REQUEST_BROWSE_CARDS)
    }


    override fun handleDeckSelection(deckId: Long, dontSkipStudyOptions: Boolean) {
        // Clear the undo history when selecting a new deck
        if (col!!.decks.selected() != deckId) {
            col!!.clearUndo()
        }
        // Select the deck
        col!!.decks.select(deckId)
        // Reset the schedule so that we get the counts for the currently selected deck
        col!!.sched.reset()
        viewModel.focusedDeck = deckId
        // Get some info about the deck to handle special cases
        val pos = mDeckListAdapter.findDeckPosition(deckId)
        val deckDueTreeNode = mDeckListAdapter.deckList[pos]
        val studyOptionsCounts = col!!.sched.counts()
        // Figure out what action to take
        if (deckDueTreeNode.newCount + deckDueTreeNode.lrnCount + deckDueTreeNode.revCount > 0) {
            // If there are cards to study then either go to Reviewer or StudyOptions
            if (mFragmented || dontSkipStudyOptions) {
                // Go to StudyOptions screen when tablet or deck counts area was clicked
                openStudyOptions(false)
            } else {
                // Otherwise jump straight to the reviewer
                openReviewer()
            }
        } else if (studyOptionsCounts[0] + studyOptionsCounts[1] + studyOptionsCounts[2] > 0) {
            // If there are cards due that can't be studied yet (due to the learn ahead limit) then go to study options
            openStudyOptions(false)
        } else if (col!!.sched.newDue() || col!!.sched.revDue()) {
            // If there are no cards to review because of the daily study limit then give "Study more" option
            UIUtils.showSnackbar(this, R.string.studyoptions_limit_reached, false, R.string.study_more, OnClickListener {
                val d = CustomStudyDialog.newInstance(
                        CustomStudyDialog.CONTEXT_MENU_LIMITS,
                        col!!.decks.selected(), true)
                showDialogFragment(d)
            }, findViewById(R.id.root_layout), mSnackbarShowHideCallback)
            // Check if we need to update the fragment or update the deck list. The same checks
            // are required for all snackbars below.
            if (mFragmented) {
                // Tablets must always show the study options that corresponds to the current deck,
                // regardless of whether the deck is currently reviewable or not.
                openStudyOptions(false)
            } else {
                // On phones, we update the deck list to ensure the currently selected deck is
                // highlighted correctly.
                updateDeckList()
            }
        } else if (col!!.decks.isDyn(deckId)) {
            // Go to the study options screen if filtered deck with no cards to study
            openStudyOptions(false)
        } else if (deckDueTreeNode.children.size == 0 && col!!.cardCount(arrayOf(deckId)) == 0) {
            // If the deck is empty and has no children then show a message saying it's empty
            val helpUrl = Uri.parse(resources.getString(R.string.link_manual_getting_started))
            mayOpenUrl(helpUrl)
            UIUtils.showSnackbar(this, R.string.empty_deck, false, R.string.help, OnClickListener { openUrl(helpUrl) }, findViewById(R.id.root_layout), mSnackbarShowHideCallback)
            if (mFragmented) {
                openStudyOptions(false)
            } else {
                updateDeckList()
            }
        } else {
            // Otherwise say there are no cards scheduled to study, and give option to do custom study
            UIUtils.showSnackbar(this, R.string.studyoptions_empty_schedule, false, R.string.custom_study, OnClickListener {
                val d = CustomStudyDialog.newInstance(
                        CustomStudyDialog.CONTEXT_MENU_EMPTY_SCHEDULE,
                        col!!.decks.selected(), true)
                showDialogFragment(d)
            }, findViewById(R.id.root_layout), mSnackbarShowHideCallback)
            if (mFragmented) {
                openStudyOptions(false)
            } else {
                updateDeckList()
            }
        }
    }


    /**
     * Scroll the deck list so that it is centered on the current deck.
     *
     * @param did The deck ID of the deck to select.
     */
    private fun scrollDecklistToDeck(did: Long) {
        val position = mDeckListAdapter.findDeckPosition(did)
        mRecyclerViewLayoutManager!!.scrollToPositionWithOffset(position, filesRecyclerView!!.height / 2)
    }


    /**
     * Launch an asynchronous task to rebuild the deck list and recalculate the deck counts. Use this
     * after any change to a deck (e.g., rename, collapse, add/delete) that needs to be reflected
     * in the deck list.
     *
     * This method also triggers an update for the widget to reflect the newly calculated counts.
     */
    override fun updateDeckList() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_DECK_COUNTS, object : DeckTask.TaskListener() {

            override fun onPreExecute() {
                if (!colIsOpen()) {
                    showProgressBar()
                }
                Timber.d("Refreshing deck list")
            }

            override fun onPostExecute(result: TaskData?) {
                hideProgressBar()
                // Make sure the fragment is visible
                if (mFragmented) {
                    mStudyoptionsFrame!!.visibility = View.VISIBLE
                }
                if (result == null) {
                    Timber.e("null result loading deck counts")
                    onCollectionLoadError()
                    return
                }
                val nodes = result.objArray?.get(0) as List<Sched.DeckDueTreeNode>
                mDeckListAdapter.buildDeckList(nodes, col!!)

                // Set the "x due in y minutes" subtitle
                try {
                    val eta = mDeckListAdapter.eta
                    val due = mDeckListAdapter.due
                    if (col!!.cardCount() != -1) {
                        var time = "-"
                        if (eta != -1) {
                            time = resources.getString(R.string.time_quantity_minutes, eta)
                        }
                        if (supportActionBar != null) {
                            supportActionBar!!.subtitle = resources.getQuantityString(R.plurals.deckpicker_title, due, due, time)
                        }
                    }
                } catch (e: RuntimeException) {
                    Timber.e(e, "RuntimeException setting time remaining")
                }

                val current = col!!.decks.current().optLong("id")
                if (viewModel.focusedDeck != current) {
                    scrollDecklistToDeck(current)
                    viewModel.focusedDeck = current
                }

                // Update the mini statistics bar as well
                AnkiStatsTaskHandler.createReviewSummaryStatistics(col, todayStatsTextView)
            }

            override fun onProgressUpdate(vararg values: TaskData) {}

            override fun onCancelled() {}

        })
    }


    // Callback to show study options for currently selected deck
    fun showContextMenuDeckOptions() {
        // open deck options
        if (viewModel.col!!.decks.isDyn(viewModel.contextMenuDeckId)) {
            // open cram options if filtered deck
            val i = Intent(this@DeckPickerActivity, FilteredDeckOptions::class.java)
            i.putExtra("did", viewModel.contextMenuDeckId)
            startActivity(i)
        } else {
            // otherwise open regular options
            val i = Intent(this@DeckPickerActivity, DeckOptions::class.java)
            i.putExtra("did", viewModel.contextMenuDeckId)
            startActivity(i)
        }
    }


    // Callback to show export dialog for currently selected deck
    fun showContextMenuExportDialog() {
        exportDeck(viewModel.contextMenuDeckId)
    }

    fun exportDeck(did: Long) {
        val msg: String
        try {
            msg = resources.getString(R.string.confirm_apkg_export_deck, col!!.decks.get(did).get("name"))
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        ExportDialog.show(supportFragmentManager, msg, did)
    }

    @JvmOverloads
    fun renameDeckDialog(deckId: Long = viewModel.contextMenuDeckId) {
        val currentName = viewModel.col!!.decks.name(deckId)
        val mDialogEditText = EditText(this@DeckPickerActivity).apply {
            setSingleLine()
            setText(currentName)
            setSelection(currentName.length)
        }

        AlertDialog.Builder(this@DeckPickerActivity)
                .setTitle(resources.getString(R.string.rename_deck))
                .setView(mDialogEditText)
                .setPositiveButton(resources.getString(R.string.rename)) { _, _ ->
                    val newName = mDialogEditText!!.text.toString().replace("\"".toRegex(), "")
                    val col = col
                    if (!TextUtils.isEmpty(newName) && newName != currentName) {
                        try {
                            col!!.decks.rename(col.decks.get(deckId), newName)
                        } catch (e: DeckRenameException) {
                            // We get a localized string from libanki to explain the errorSnackbar
                            UIUtils.showThemedToast(this@DeckPickerActivity, e.getLocalizedMessage(resources), false)
                        }

                    }
                    dismissAllDialogFragments()
                    mDeckListAdapter.notifyDataSetChanged()
                    updateDeckList()
                    if (mFragmented) {
                        loadStudyOptionsFragment(false)
                    }
                }
                .setNegativeButton(resources.getString(R.string.dialog_cancel)) { _, _ ->
                    dismissAllDialogFragments()
                }
                .create()
                .show()
    }

    @JvmOverloads
    fun confirmDeckDeletion(deckId: Long = viewModel.contextMenuDeckId) {
        if (!colIsOpen()) {
            return
        }
        if (deckId == 1L) {
            UIUtils.showSimpleSnackbar(this, R.string.delete_deck_default_deck, true)
            dismissAllDialogFragments()
            return
        }
        // Get the number of cards contained in this deck and its subdecks
        val children = col!!.decks.children(deckId)
        val dids = LongArray(children.size + 1)
        dids[0] = deckId
        var i = 1
        for (l in children.values) {
            dids[i++] = l!!
        }
        val ids = Utils.ids2str(dids)
        val cnt = col!!.db.queryScalar(
                "select count() from cards where deckId in $ids or odid in $ids")
        // Delete empty decks without warning
        if (cnt == 0) {
            deleteDeck(deckId)
            dismissAllDialogFragments()
            return
        }
        // Otherwise we show a warning and require confirmation
        val msg: String
        val deckName = "\'" + col!!.decks.name(deckId) + "\'"
        val isDyn = col!!.decks.isDyn(deckId)
        msg = if (isDyn) {
            resources.getString(R.string.delete_cram_deck_message, deckName)
        } else {
            resources.getQuantityString(R.plurals.delete_deck_message, cnt, deckName, cnt)
        }
        showDialogFragment(DeckPickerConfirmDeleteDeckDialog.newInstance(msg))
    }


    // Callback to delete currently selected deck
    fun deleteContextMenuDeck() {
        deleteDeck(viewModel.contextMenuDeckId)
    }


    private fun deleteDeck(did: Long) {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_DELETE_DECK, object : DeckTask.TaskListener() {
            // Flag to indicate if the deck being deleted is the current deck.
            private var removingCurrent: Boolean = false

            override fun onPreExecute() {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, "",
                        resources.getString(R.string.delete_deck), false)
                if (did == col!!.decks.current().optLong("id")) {
                    removingCurrent = true
                }
            }


            override fun onPostExecute(result: TaskData?) {
                if (result == null) {
                    return
                }
                // In fragmented mode, if the deleted deck was the current deck, we need to reload
                // the study options fragment with a valid deck and re-center the deck list to the
                // new current deck. Otherwise we just update the list normally.
                if (mFragmented && removingCurrent) {
                    updateDeckList()
                    openStudyOptions(false)
                } else {
                    updateDeckList()
                }

                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    try {
                        mProgressDialog!!.dismiss()
                    } catch (e: Exception) {
                        Timber.e(e, "onPostExecute - Exception dismissing dialog")
                    }

                }
                // TODO: if we had "undo delete note" like desktop client then we won't need this.
                col!!.clearUndo()
            }


            override fun onProgressUpdate(vararg values: TaskData) {}


            override fun onCancelled() {}
        }, TaskData(did))
    }


    fun rebuildFiltered() {
        viewModel.deckTaskRebuildCram(mFragmented)
//        viewModel.col!!.decks.select(viewModel.contextMenuDeckId)
//        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_REBUILD_CRAM, mSimpleProgressListener,
//                TaskData(mFragmented))
    }


    fun emptyFiltered() {
        col!!.decks.select(viewModel.contextMenuDeckId)
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_EMPTY_CRAM, mSimpleProgressListener,
                TaskData(mFragmented))
    }


    override fun onRequireDeckListUpdate() {
        updateDeckList()
    }


    private fun openReviewer() {
        val reviewer = Intent(this, Reviewer::class.java)
        startActivityForResult(reviewer, REQUEST_REVIEW)
        viewModel.col!!.startTimebox()
    }

    override fun onCreateCustomStudySession() {
        updateDeckList()
        openStudyOptions(false)
    }

    override fun onExtendStudyLimits() {
        if (mFragmented) {
            fragment!!.refreshInterface(true)
        }
        updateDeckList()
    }

    private fun handleEmptyCards() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_FIND_EMPTY_CARDS, object : DeckTask.Listener {
            override fun onPreExecute(task: DeckTask) {
                mProgressDialog = StyledProgressDialog.show(this@DeckPickerActivity, "",
                        resources.getString(R.string.empty_cards_finding), false)
            }

            override fun onPostExecute(task: DeckTask, result: TaskData?) {
                val cids = result!!.objArray?.get(0) as List<Long>
                if (cids.isEmpty()) {
                    showSimpleMessageDialog(resources.getString(R.string.empty_cards_none))
                } else {
                    val msg = String.format(resources.getString(R.string.empty_cards_count), cids.size)
                    val dialog = ConfirmationDialog()
                    dialog.setArgs(msg)
                    val confirm = Runnable {
                        col!!.remCards(Utils.arrayList2array(cids))
                        UIUtils.showSimpleSnackbar(this@DeckPickerActivity, String.format(
                                resources.getString(R.string.empty_cards_deleted), cids.size), false)
                    }
                    dialog.setConfirm(confirm)
                    showDialogFragment(dialog)
                }

                if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
            }

            override fun onProgressUpdate(task: DeckTask, vararg values: TaskData) {

            }

            override fun onCancelled() {

            }
        })
    }

    companion object {

        /**
         * Result codes from other activities
         */
        val RESULT_MEDIA_EJECTED = 202
        val RESULT_DB_ERROR = 203


        /**
         * Available options performed by other activities (request codes for onActivityResult())
         */
        val REQUEST_STORAGE_PERMISSION = 0
        private val REQUEST_PATH_UPDATE = 1
        private val LOG_IN_FOR_SYNC = 6
        private val SHOW_INFO_WELCOME = 8
        private val SHOW_INFO_NEW_VERSION = 9
        private val REPORT_ERROR = 10
        val SHOW_STUDYOPTIONS = 11
        private val ADD_NOTE = 12

        // For automatic syncing
        // 10 minutes in milliseconds.
        val AUTOMATIC_SYNC_MIN_INTERVAL: Long = 600000

        private val SWIPE_TO_SYNC_TRIGGER_DISTANCE = 400
    }
}