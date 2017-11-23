package com.ichi2.anki

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.LoaderManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.dialogs.AsyncDialogFragment
import com.ichi2.anki.dialogs.DialogHandler
import com.ichi2.anki.dialogs.SimpleMessageDialog
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.utils.async.CollectionLoader
import com.ichi2.utils.compat.CompatHelper
import com.ichi2.utils.compat.customtabs.CustomTabActivityHelper
import com.ichi2.libanki.Collection
import com.ichi2.utils.themes.Themes
import timber.log.Timber

abstract class AnkiActivity : AppCompatActivity(),
        LoaderManager.LoaderCallbacks<Collection>,
        SimpleMessageDialog.SimpleMessageDialogListener {

    private val SIMPLE_NOTIFICATION_ID = 0

    lateinit var dialogHandler: DialogHandler

    // custom tabs
    var customTabActivityHelper: CustomTabActivityHelper? = null
        private set

//    private lateinit var lifecycleRegistry: LifecycleRegistry


    val col: Collection?
        get() = CollectionHelper.getInstance().getCol(this)

//    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
//        lifecycleRegistry = LifecycleRegistry(this)
        dialogHandler = DialogHandler(this)

        // The hardware buttons should control the music volume
        volumeControlStream = AudioManager.STREAM_MUSIC
        // Set the theme
        Themes.setTheme(this)
        super.onCreate(savedInstanceState)
        customTabActivityHelper = CustomTabActivityHelper()
    }

    override fun onStart() {
        super.onStart()
        customTabActivityHelper!!.bindCustomTabsService(this)
    }

    override fun onStop() {
        super.onStop()
        customTabActivityHelper!!.unbindCustomTabsService(this)
    }

    override fun onResume() {
        super.onResume()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(SIMPLE_NOTIFICATION_ID)
        // Show any pending dialogs which were stored persistently
        dialogHandler.readMessage()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            Timber.i("Home button pressed")
            finish();
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    // called when the CollectionLoader finishes... usually will be over-ridden
    protected open fun onCollectionLoaded(col: Collection) {}

    fun colIsOpen(): Boolean = CollectionHelper.getInstance().colIsOpen()

    // Method for loading the collection which is inherited by all AnkiActivitys
    fun startLoadingCollection() {
        // Initialize the open collection loader
        Timber.d("AnkiActivity.startLoadingCollection()")
        if (!colIsOpen()) {
            showProgressBar()
        }
        supportLoaderManager.restartLoader(0, Bundle(), this)
    }


    // Kick user back to DeckPickerActivity on collection load errorSnackbar unless this method is overridden
    protected open fun onCollectionLoadError() {
        val deckPicker = Intent(this, DeckPickerActivity::class.java)
        deckPicker.putExtra("collectionLoadError", true) // don't currently do anything with this
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(deckPicker)
    }


    // CollectionLoader Listener callbacks
    override fun onCreateLoader(id: Int, args: Bundle): Loader<Collection> =// Currently only using one loader, so ignore id
            CollectionLoader(this)


    override fun onLoadFinished(loader: Loader<Collection>, col: Collection?) {
        hideProgressBar()
        if (col != null && colIsOpen()) {
            onCollectionLoaded(col)
        } else {
            onCollectionLoadError()
        }
    }


    override fun onLoaderReset(arg0: Loader<Collection>) {
        // We don't currently retain any references, so no need to free any data here
    }


    open fun showProgressBar() {
        val progressBar = findViewById<View>(R.id.progress_bar)
        if (progressBar != null) {
            progressBar.visibility = View.VISIBLE
        }
    }


    open fun hideProgressBar() {
        val progressBar = findViewById<View>(R.id.progress_bar)
        if (progressBar != null) {
            progressBar.visibility = View.GONE
        }
    }


    protected fun mayOpenUrl(url: Uri) {
        val success = customTabActivityHelper!!.mayLaunchUrl(url, null, null)
        if (!success) {
            Timber.w("Couldn't preload url: %s", url.toString())
        }
    }

    protected fun openUrl(url: Uri) {
        CompatHelper.compat?.openUrl(this, url)
    }


    /**
     * Global method to show dialog fragment including adding it to back stack Note: DO NOT call this from an async
     * task! If you need to show a dialog from an async task, use showAsyncDialogFragment()
     *
     * @param newFragment  the DialogFragment you want to show
     */
    fun showDialogFragment(newFragment: DialogFragment) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        // save transaction to the back stack
        ft.addToBackStack("dialog")
        newFragment.show(ft, "dialog")
        supportFragmentManager.executePendingTransactions()
    }


    /**
     * Global method to show a dialog fragment including adding it to back stack and handling the case where the dialog
     * is shown from an async task, by showing the message in the notification bar if the activity was stopped before the
     * AsyncTask completed
     *
     * @param newFragment  the AsyncDialogFragment you want to show
     */
    fun showAsyncDialogFragment(newFragment: AsyncDialogFragment) {
        try {
            showDialogFragment(newFragment)
        } catch (e: IllegalStateException) {
            // Store a persistent message to SharedPreferences instructing AnkiDroid to show dialog
            DialogHandler.storeMessage(newFragment.getDialogHandlerMessage())
            // Show a basic notification to the user in the notification bar in the meantime
            val title = newFragment.notificationTitle
            val message = newFragment.notificationMessage
            showSimpleNotification(title, message)
        }

    }


    /**
     * Show a simple message dialog, dismissing the message without taking any further action when OK button is pressed.
     * If a DialogFragment cannot be shown due to the Activity being stopped then the message is shown in the
     * notification bar instead.
     *
     * @param message
     * @param reload flag which forces app to be restarted when true
     */
    @JvmOverloads protected fun showSimpleMessageDialog(message: String, reload: Boolean = false) {
        val newFragment = SimpleMessageDialog.newInstance(message, reload)
        showAsyncDialogFragment(newFragment)
    }

    @JvmOverloads protected fun showSimpleMessageDialog(title: String, message: String, reload: Boolean = false) {
        val newFragment = SimpleMessageDialog.newInstance(title, message, reload)
        showAsyncDialogFragment(newFragment)
    }


    fun showSimpleNotification(title: String, message: String) {
        val prefs = AnkiDroidApp.getSharedPrefs(this)
        // Don't show notification if disabled in preferences
        if (Integer.parseInt(prefs.getString("minimumCardsDueForNotification", "0")) <= 1000000) {
            // Use the title as the ticker unless the title is simply "AnkiDroid"
            var ticker = title
            if (title == resources.getString(R.string.app_name)) {
                ticker = message
            }
            // Build basic notification
            val builder = NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setColor(ContextCompat.getColor(this, R.color.material_light_blue_500))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setTicker(ticker)
            // Enable vibrate and blink if set in preferences
            if (prefs.getBoolean("widgetVibrate", false)) {
                builder.setVibrate(longArrayOf(1000, 1000, 1000))
            }
            if (prefs.getBoolean("widgetBlink", false)) {
                builder.setLights(Color.BLUE, 1000, 1000)
            }
            // Creates an explicit intent for an Activity in your app
            val resultIntent = Intent(this, DeckPickerActivity::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(resultPendingIntent)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // mId allows you to update the notification later on.
            notificationManager.notify(SIMPLE_NOTIFICATION_ID, builder.build())
        }

    }

    // Handle closing simple message dialog
    override fun dismissSimpleMessageDialog(reload: Boolean) {
        dismissAllDialogFragments()
        if (reload) {
            val deckPicker = Intent(this, DeckPickerActivity::class.java)
            deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(deckPicker)
        }
    }


    // Dismiss whatever dialog is showing
    fun dismissAllDialogFragments() {
        supportFragmentManager.popBackStack("dialog", FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }


    // Restart the activity
    fun restartActivity() {
        Timber.i("AnkiActivity -- restartActivity()")
        val intent = Intent()
        intent.setClass(this, this.javaClass)
        intent.putExtras(Bundle())
        this.startActivity(intent)
        this.finish();
    }

    companion object {
        val REQUEST_REVIEW = 901
    }
}
/**
 * Show a simple message dialog, dismissing the message without taking any further action when OK button is pressed.
 * If a DialogFragment cannot be shown due to the Activity being stopped then the message is shown in the
 * notification bar instead.
 *
 * @param message
 */

