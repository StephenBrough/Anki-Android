package com.ichi2.anki.dialogs

import android.os.Bundle
import android.os.Message
import android.support.v4.app.FragmentManager
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.*
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.junkdrawer.BackupManager
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting
import java.io.File
import java.io.IOException
import java.util.*

class DatabaseErrorDialog : AsyncDialogFragment() {
    var dialogMessage: String by StringArg()
    var dialogType: Int by IntArg(default = 0)

    private// Before honeycomb there's no way to know if the db has actually been corrupted
            // so we show a non-specific message.
            // The sqlite database has been corrupted (DatabaseErrorHandler.onCorrupt() was called)
            // Show a specific message appropriate for the situation
            // Generic message shown when a libanki task failed
    val message: String
        get() = when (dialogType) {
            DIALOG_LOAD_FAILED -> if (databaseCorruptFlag) {
                resources.getString(R.string.corrupt_db_message, resources.getString(R.string.repair_deck))
            } else {
                resources.getString(R.string.access_collection_failed_message, resources.getString(R.string.link_help))
            }
            DIALOG_DB_ERROR -> resources.getString(R.string.answering_error_message)
            DIALOG_REPAIR_COLLECTION -> resources.getString(R.string.repair_deck_dialog, BackupManager.BROKEN_DECKS_SUFFIX)
            DIALOG_RESTORE_BACKUP -> resources.getString(R.string.backup_restore_no_backups)
            DIALOG_NEW_COLLECTION -> resources.getString(R.string.backup_del_collection_question)
            DIALOG_CONFIRM_DATABASE_CHECK -> resources.getString(R.string.check_db_warning)
            DIALOG_CONFIRM_RESTORE_BACKUP -> resources.getString(R.string.restore_backup)
            DIALOG_FULL_SYNC_FROM_SERVER -> resources.getString(R.string.backup_full_sync_from_server_question)
            DIALOG_CURSOR_SIZE_LIMIT_EXCEEDED -> resources.getString(R.string.cursor_size_limit_exceeded)
            else -> dialogMessage
        }


    private val title: String
        get() = when (dialogType) {
            DIALOG_LOAD_FAILED -> resources.getString(R.string.open_collection_failed_title)
            DIALOG_DB_ERROR -> resources.getString(R.string.answering_error_title)
            DIALOG_ERROR_HANDLING -> resources.getString(R.string.error_handling_title)
            DIALOG_REPAIR_COLLECTION -> resources.getString(R.string.backup_repair_deck)
            DIALOG_RESTORE_BACKUP -> resources.getString(R.string.backup_restore)
            DIALOG_NEW_COLLECTION -> resources.getString(R.string.backup_new_collection)
            DIALOG_CONFIRM_DATABASE_CHECK -> resources.getString(R.string.check_db_title)
            DIALOG_CONFIRM_RESTORE_BACKUP -> resources.getString(R.string.restore_backup_title)
            DIALOG_FULL_SYNC_FROM_SERVER -> resources.getString(R.string.backup_full_sync_from_server)
            DIALOG_CURSOR_SIZE_LIMIT_EXCEEDED -> resources.getString(R.string.open_collection_failed_title)
            else -> resources.getString(R.string.answering_error_title)
        }


    override val notificationMessage: String by lazy { message }

    override val notificationTitle: String by lazy { resources.getString(R.string.answering_error_title) }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val builder = MaterialDialog.Builder(activity!!)
        builder.cancelable(true)
                .title(title)

        var sqliteInstalled = false
        try {
            sqliteInstalled = Runtime.getRuntime().exec("sqlite3 --version").waitFor() == 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        when (dialogType) {
            DIALOG_CURSOR_SIZE_LIMIT_EXCEEDED, DIALOG_LOAD_FAILED ->
                // Collection failed to load; give user the option of either choosing from repair options, or closing
                // the activity
                return builder.cancelable(false)
                        .content(message)
                        .iconAttr(R.attr.dialogErrorIcon)
                        .positiveText(resources.getString(R.string.error_handling_options))
                        .negativeText(resources.getString(R.string.close))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity)
                                        .showDatabaseErrorDialog(DIALOG_ERROR_HANDLING)
                            }

                            override fun onNegative(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity).exit()
                            }
                        })
                        .show()

            DIALOG_DB_ERROR -> {
                // Database Check failed to execute successfully; give user the option of either choosing from repair
                // options, submitting an errorSnackbar report, or closing the activity
                val dialog = builder
                        .cancelable(false)
                        .content(message)
                        .iconAttr(R.attr.dialogErrorIcon)
                        .positiveText(resources.getString(R.string.error_handling_options))
                        .negativeText(resources.getString(R.string.answering_error_report))
                        .neutralText(resources.getString(R.string.close))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity)
                                        .showDatabaseErrorDialog(DIALOG_ERROR_HANDLING)
                            }

                            override fun onNegative(dialog: MaterialDialog?) {
                                //                                ((DeckPickerActivity) getActivity()).sendErrorReport();
                                dismissAllDialogFragments()
                            }

                            override fun onNeutral(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity).exit()
                            }
                        })
                        .show()
                dialog.customView!!.findViewById<View>(R.id.buttonDefaultNegative).isEnabled = (activity as DeckPickerActivity).hasErrorFiles()
                return dialog
            }

            DIALOG_ERROR_HANDLING -> {
                // The user has asked to see repair options; allow them to choose one of the repair options or go back
                // to the previous dialog
                val options = ArrayList<String>()
                val values = ArrayList<Int>()
                if (!(activity as AnkiActivity).colIsOpen()) {
                    // retry
                    options.add(resources.getString(R.string.backup_retry_opening))
                    values.add(0)
                } else {
                    // fix integrity
                    options.add(resources.getString(R.string.check_db))
                    values.add(1)
                }
                // repair db with sqlite
                if (sqliteInstalled) {
                    options.add(resources.getString(R.string.backup_error_menu_repair))
                    values.add(2)
                }
                // // restore from backup
                options.add(resources.getString(R.string.backup_restore))
                values.add(3)
                // delete old collection and build new one
                options.add(resources.getString(R.string.backup_full_sync_from_server))
                values.add(4)
                // delete old collection and build new one
                options.add(resources.getString(R.string.backup_del_collection))
                values.add(5)

                val titles = arrayOfNulls<String>(options.size)
                val mRepairValues = IntArray(options.size)
                for (i in options.indices) {
                    titles[i] = options[i]
                    mRepairValues[i] = values[i]
                }

                return builder.iconAttr(R.attr.dialogErrorIcon)
                        .negativeText(R.string.cancel)
                        .items(*titles)
                        .itemsCallback(MaterialDialog.ListCallback { _, _, which, _ ->
                            when (mRepairValues[which]) {
                                0 -> {
                                    (activity as DeckPickerActivity).restartActivity()
                                    return@ListCallback
                                }
                                1 -> {
                                    (activity as DeckPickerActivity)
                                            .showDatabaseErrorDialog(DIALOG_CONFIRM_DATABASE_CHECK)
                                    return@ListCallback
                                }
                                2 -> {
                                    (activity as DeckPickerActivity)
                                            .showDatabaseErrorDialog(DIALOG_REPAIR_COLLECTION)
                                    return@ListCallback
                                }
                                3 -> {
                                    (activity as DeckPickerActivity)
                                            .showDatabaseErrorDialog(DIALOG_RESTORE_BACKUP)
                                    return@ListCallback
                                }
                                4 -> {
                                    (activity as DeckPickerActivity)
                                            .showDatabaseErrorDialog(DIALOG_FULL_SYNC_FROM_SERVER)
                                    return@ListCallback
                                }
                                5 -> (activity as DeckPickerActivity)
                                        .showDatabaseErrorDialog(DIALOG_NEW_COLLECTION)
                            }
                        })
                        .show()
            }

            DIALOG_REPAIR_COLLECTION ->
                // Allow user to runImport BackupManager.repairCollection()
                return builder.content(message!!)
                        .iconAttr(R.attr.dialogErrorIcon)
                        .positiveText(resources.getString(R.string.dialog_positive_repair))
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity).repairDeck()
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_RESTORE_BACKUP -> {
                // Allow user to restore one of the backups
                val path = CollectionHelper.getCollectionPath(activity)
                val mBackups = BackupManager.getBackups(File(path)).apply { reverse() }
                if (mBackups.isEmpty()) {
                    builder.title(resources.getString(R.string.backup_restore))
                            .content(message)
                            .positiveText(R.string.ok)
                            .callback(object : MaterialDialog.ButtonCallback() {
                                override fun onPositive(dialog: MaterialDialog?) {
                                    (activity as DeckPickerActivity)
                                            .showDatabaseErrorDialog(DIALOG_ERROR_HANDLING)
                                }
                            })
                } else {
                    val dates = arrayOfNulls<String>(mBackups.size)
                    for (i in mBackups.indices) {
                        dates[i] = mBackups[i].name.replace(".*-(\\d{4}-\\d{2}-\\d{2})-(\\d{2})-(\\d{2}).apkg".toRegex(), "$1 ($2:$3 h)")
                    }
                    builder.title(resources.getString(R.string.backup_restore_select_title))
                            .negativeText(R.string.cancel)
                            .callback(object : MaterialDialog.ButtonCallback() {
                                override fun onNegative(dialog: MaterialDialog?) {
                                    dismissAllDialogFragments()
                                }
                            })
                            .items(*dates)
                            .itemsCallbackSingleChoice(dates.size
                            ) { _, _, which, _ ->
                                if (mBackups[which].length() > 0) {
                                    // restore the backup if it's valid
                                    (activity as DeckPickerActivity)
                                            .restoreFromBackup(mBackups[which]
                                                    .path)
                                    dismissAllDialogFragments()
                                } else {
                                    // otherwise show an errorSnackbar dialog
                                    MaterialDialog.Builder(activity!!)
                                            .title(R.string.backup_error)
                                            .content(R.string.backup_invalid_file_error)
                                            .positiveText(R.string.dialog_ok)
                                            .build().show()
                                }
                                true
                            }
                }
                return builder.show()
            }

            DIALOG_NEW_COLLECTION ->
                // Allow user to create a new empty collection
                return builder.content(message)
                        .positiveText(resources.getString(R.string.dialog_positive_create))
                        .negativeText(resources.getString(R.string.dialog_cancel))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                CollectionHelper.getInstance().closeCollection(false)
                                val path = CollectionHelper.getCollectionPath(activity)
                                if (BackupManager.moveDatabaseToBrokenFolder(path, false)) {
                                    (activity as DeckPickerActivity).restartActivity()
                                } else {
                                    (activity as DeckPickerActivity).showDatabaseErrorDialog(DIALOG_LOAD_FAILED)
                                }
                            }
                        })
                        .show()

            DIALOG_CONFIRM_DATABASE_CHECK ->
                // Confirmation dialog for database check
                return builder.content(message)
                        .positiveText(resources.getString(R.string.dialog_ok))
                        .negativeText(resources.getString(R.string.dialog_cancel))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity).integrityCheck()
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_CONFIRM_RESTORE_BACKUP ->
                // Confirmation dialog for backup restore
                return builder.content(message)
                        .positiveText(resources.getString(R.string.dialog_continue))
                        .negativeText(resources.getString(R.string.dialog_cancel))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity)
                                        .showDatabaseErrorDialog(DIALOG_RESTORE_BACKUP)
                            }
                        })
                        .show()

            DIALOG_FULL_SYNC_FROM_SERVER ->
                // Allow user to do a full-sync from the server
                return builder.content(message)
                        .positiveText(resources.getString(R.string.dialog_positive_overwrite))
                        .negativeText(resources.getString(R.string.dialog_cancel))
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as DeckPickerActivity).sync("download")
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            else -> return builder.show()
        }
    }


    override fun getDialogHandlerMessage(): Message? {
        val msg = Message.obtain()
        msg.what = DialogHandler.MSG_SHOW_DATABASE_ERROR_DIALOG
        val b = Bundle()
        b.putInt("dialogType", dialogType)
        msg.data = b
        return msg
    }


    fun dismissAllDialogFragments() {
        (activity as DeckPickerActivity).dismissAllDialogFragments()
    }

    companion object {

        const val DIALOG_LOAD_FAILED = 0
        const val DIALOG_DB_ERROR = 1
        const val DIALOG_ERROR_HANDLING = 2
        const val DIALOG_REPAIR_COLLECTION = 3
        const val DIALOG_RESTORE_BACKUP = 4
        const val DIALOG_NEW_COLLECTION = 5
        const val DIALOG_CONFIRM_DATABASE_CHECK = 6
        const val DIALOG_CONFIRM_RESTORE_BACKUP = 7
        const val DIALOG_FULL_SYNC_FROM_SERVER = 8
        const val DIALOG_CURSOR_SIZE_LIMIT_EXCEEDED = 9

        // public flag which lets us distinguish between inaccessible and corrupt database
        var databaseCorruptFlag = false


        /**
         * A set of dialogs which deal with problems with the database when it can't load
         *
         * @param dialogType An integer which specifies which of the sub-dialogs to show
         */
        fun newInstance(dialogType: Int): DatabaseErrorDialog {
            val f = DatabaseErrorDialog()
            val args = Bundle()
            args.putInt("dialogType", dialogType)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogType: Int) {
            fm.dismissExisting<DatabaseErrorDialog>()
            DatabaseErrorDialog().apply {
                this.dialogType = dialogType
            }.show(fm, DatabaseErrorDialog::class.java.simpleName)
        }
    }
}
