package com.ichi2.anki.dialogs

import android.os.Bundle
import android.os.Message
import android.support.v4.app.FragmentManager

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.R
import com.ichi2.libanki.Collection
import com.ichi2.utils.IntArg
import com.ichi2.utils.StringArg
import com.ichi2.utils.dismissExisting

class SyncErrorDialog : AsyncDialogFragment() {

    var dialogType: Int by IntArg()
    var dialogMessage: String by StringArg()


    private val title: String
        get() = when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC -> resources.getString(R.string.not_logged_in_title)
            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE, DIALOG_SYNC_CONFLICT_RESOLUTION -> resources.getString(R.string.sync_conflict_title)
            else -> resources.getString(R.string.sync_error)
        }

    /**
     * Get the title which is shown in notification bar when dialog fragment can't be shown
     *
     * @return tile to be shown in notification in bar
     */
    override val notificationTitle: String
        get() = when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC -> resources.getString(R.string.sync_error)
            else -> title
        }


    private val message: String
        get() = when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC -> resources.getString(R.string.login_create_account_message)
            DIALOG_CONNECTION_ERROR -> resources.getString(R.string.connection_error_message)
            DIALOG_SYNC_CONFLICT_RESOLUTION -> resources.getString(R.string.sync_conflict_message)
            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL -> resources.getString(R.string.sync_conflict_local_confirm)
            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE -> resources.getString(R.string.sync_conflict_remote_confirm)
            DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL -> resources.getString(R.string.sync_conflict_local_confirm)
            DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE -> resources.getString(R.string.sync_conflict_remote_confirm)
            else -> dialogMessage
        }

    /**
     * Get the message which is shown in notification bar when dialog fragment can't be shown
     *
     * @return message to be shown in notification in bar
     */
    override val notificationMessage: String
        get() = when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC -> resources.getString(R.string.not_logged_in_title)
            else -> message
        }

    interface SyncErrorDialogListener {

        val col: Collection?
        fun showSyncErrorDialog(dialogType: Int)
        fun showSyncErrorDialog(dialogType: Int, message: String)
        fun loginToSyncServer()
        fun sync()
        fun sync(conflict: String?)
        fun mediaCheck()
        fun dismissAllDialogFragments()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        super.onCreate(savedInstanceState)
        val builder = MaterialDialog.Builder(activity)
                .title(title)
                .content(message)
                .cancelable(true)

        when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC ->
                // User not logged in; take them to login screen
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(R.string.log_in)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener).loginToSyncServer()
                            }
                        })
                        .show()

            DIALOG_CONNECTION_ERROR ->
                // Connection errorSnackbar; allow user to retry or cancel
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(R.string.retry)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener).sync()
                                dismissAllDialogFragments()
                            }

                            override fun onNegative(dialog: MaterialDialog?) {
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_SYNC_CONFLICT_RESOLUTION ->
                // Sync conflict; allow user to cancel, or choose between local and remote versions
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(R.string.sync_conflict_local)
                        .negativeText(R.string.sync_conflict_remote)
                        .neutralText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener)
                                        .showSyncErrorDialog(DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL)
                            }

                            override fun onNegative(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener)
                                        .showSyncErrorDialog(DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE)
                            }

                            override fun onNeutral(dialog: MaterialDialog?) {
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL ->
                // Confirmation before pushing local collection to server after sync conflict
                return builder.positiveText(R.string.dialog_positive_overwrite)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                val activity = activity as SyncErrorDialogListener
                                activity.sync("upload")
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE ->
                // Confirmation before overwriting local collection with server collection after sync conflict
                return builder.positiveText(R.string.dialog_positive_overwrite)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                val activity = activity as SyncErrorDialogListener
                                activity.sync("download")
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_SYNC_SANITY_ERROR ->
                // Sync sanity check errorSnackbar; allow user to cancel, or choose between local and remote versions
                return builder.positiveText(R.string.sync_sanity_local)
                        .neutralText(R.string.sync_sanity_remote)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener)
                                        .showSyncErrorDialog(DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL)
                            }

                            override fun onNeutral(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener)
                                        .showSyncErrorDialog(DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE)
                            }
                        })
                        .show()

            DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL ->
                // Confirmation before pushing local collection to server after sanity check errorSnackbar
                return builder.positiveText(R.string.dialog_positive_overwrite)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener).sync("upload")
                                dismissAllDialogFragments()
                            }
                        })
                        .show()

            DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE ->
                // Confirmation before overwriting local collection with server collection after sanity check errorSnackbar
                return builder.positiveText(R.string.dialog_positive_overwrite)
                        .negativeText(R.string.cancel)
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog?) {
                                (activity as SyncErrorDialogListener).sync("download")
                                dismissAllDialogFragments()
                            }
                        })
                        .show()
            DIALOG_MEDIA_SYNC_ERROR -> return builder.positiveText(R.string.check_media)
                    .negativeText(R.string.cancel)
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog?) {
                            (activity as SyncErrorDialogListener).mediaCheck()
                            dismissAllDialogFragments()
                        }
                    })
                    .show()
            else -> return builder.show()
        }
    }

    override fun getDialogHandlerMessage(): Message? {
        val msg = Message.obtain()
        msg.what = DialogHandler.MSG_SHOW_SYNC_ERROR_DIALOG
        val b = Bundle()
        b.putInt("dialogType", dialogType)
        b.putString("dialogMessage", dialogMessage)
        msg.data = b
        return msg
    }

    fun dismissAllDialogFragments() = (activity as SyncErrorDialogListener).dismissAllDialogFragments()

    companion object {
        const val DIALOG_USER_NOT_LOGGED_IN_SYNC = 0
        const val DIALOG_CONNECTION_ERROR = 1
        const val DIALOG_SYNC_CONFLICT_RESOLUTION = 2
        const val DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL = 3
        const val DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE = 4
        const val DIALOG_SYNC_SANITY_ERROR = 6
        const val DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL = 7
        const val DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE = 8
        const val DIALOG_MEDIA_SYNC_ERROR = 9

        /**
         * A set of dialogs belonging to AnkiActivity which deal with sync problems
         *
         * @param dialogType An integer which specifies which of the sub-dialogs to show
         * @param dialogMessage A string which can be optionally used to set the dialog message
         */
        fun newInstance(dialogType: Int, dialogMessage: String): SyncErrorDialog {
            val f = SyncErrorDialog()
            val args = Bundle()
            args.putInt("dialogType", dialogType)
            args.putString("dialogMessage", dialogMessage)
            f.arguments = args
            return f
        }

        fun show(fm: FragmentManager, dialogType: Int, dialogMessage: String) {
            fm.dismissExisting<SyncErrorDialog>()
            SyncErrorDialog().apply {
                this.dialogType = dialogType
                this.dialogMessage = dialogMessage
            }.show(fm, SyncErrorDialog::class.java.simpleName)
        }
    }
}
