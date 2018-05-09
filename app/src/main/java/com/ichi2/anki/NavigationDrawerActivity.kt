/****************************************************************************************
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
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
package com.ichi2.anki

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import android.view.View

import com.ichi2.anki.cardbrowser.CardBrowser
import com.ichi2.anki.deckpicker.DeckPickerActivity
import com.ichi2.anki.flashcardviewer.Reviewer
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.stats.Statistics
import com.ichi2.utils.compat.CompatHelper
import com.ichi2.anki.preferences.Preferences
import com.ichi2.utils.themes.Themes
import kotlinx.android.synthetic.main.navigation_drawer.view.*

import timber.log.Timber


abstract class NavigationDrawerActivity : AnkiActivity(), NavigationView.OnNavigationItemSelectedListener {

    /** Navigation Drawer  */
    private var mTitle: CharSequence = ""
    protected var mFragmented: Boolean = false
    private var mNavButtonGoesBack = false
    // Other members
    private var mOldColPath: String? = null
    private var mOldTheme: Int = 0
    // Navigation drawer list item entries
//    private var drawerLayout: DrawerLayout? = null
    lateinit var drawerToggle: ActionBarDrawerToggle
    private var mNightModeSwitch: SwitchCompat? = null

    /**
     * runnable that will be executed after the drawer has been closed.
     */
    private var pendingRunnable: Runnable? = null

    val isDrawerOpen: Boolean
        get() = drawerLayout?.isDrawerOpen(GravityCompat.START) ?: false


    //region Lifecycle Overrides
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState()
    }

    //endregion

    //region Other Activity Overrides
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val preferences = AnkiDroidApp.getSharedPrefs(this)
        // Update language
        AnkiDroidApp.setLanguage(preferences.getString(Preferences.LANGUAGE, ""))
        // Restart the activity on preference change
        if (requestCode == REQUEST_PREFERENCES_UPDATE) {
            if (mOldColPath != null && CollectionHelper.getCurrentAnkiDroidDirectory(this) == mOldColPath) {
                // collection path hasn't been changed so just restart the current activity
                if (this is Reviewer && preferences.getBoolean("tts", false)) {
                    // Workaround to kick user back to StudyOptions after opening settings from Reviewer
                    // because onDestroy() of old Activity interferes with TTS in new Activity
                    finish();
                } else if (mOldTheme != Themes.getCurrentTheme(applicationContext)) {
                    // The current theme was changed, so need to reload the stack with the new theme
                    CompatHelper.compat.restartActivityInvalidateBackstack(this@NavigationDrawerActivity)
                } else {
                    restartActivity()
                }
            } else {
                // collection path has changed so kick the user back to the DeckPickerActivity
                CollectionHelper.getInstance().closeCollection(true)
                CompatHelper.compat.restartActivityInvalidateBackstack(this)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (isDrawerOpen) {
            Timber.i("Back key pressed")
            drawerLayout!!.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig)

    }

    //endregion

    //region Navigation Item Selected Listener Override
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Don't do anything if user selects already selected position
        if (item.isChecked) {
            return true
        }

        /*
         * This runnable will be executed in onDrawerClosed(...)
         * to make the animation more fluid on older devices.
         */
        pendingRunnable = Runnable {
            // Take action if a different item selected
            when (item.itemId) {
                R.id.nav_decks -> {
                    val deckPicker = Intent(this@NavigationDrawerActivity, DeckPickerActivity::class.java)
                    deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)    // opening DeckPickerActivity should clear back history
                    startActivity(deckPicker)
                }
                R.id.nav_browser -> openCardBrowser()
                R.id.nav_stats -> {
                    val intent = Intent(this@NavigationDrawerActivity, Statistics::class.java)
                    intent.putExtra("selectedDeck", col!!.decks.selected())
                    startActivityForResult(intent, REQUEST_STATISTICS)
                }
                R.id.nav_night_mode -> mNightModeSwitch!!.performClick()
                R.id.nav_settings -> {
                    mOldColPath = CollectionHelper.getCurrentAnkiDroidDirectory(this@NavigationDrawerActivity)
                    // Remember the theme we started with so we can restart the Activity if it changes
                    mOldTheme = Themes.getCurrentTheme(applicationContext)
                    startActivityForResult(Intent(this@NavigationDrawerActivity, Preferences::class.java), REQUEST_PREFERENCES_UPDATE)
                }
                R.id.nav_help -> openUrl(Uri.parse(AnkiDroidApp.getManualUrl()))
                R.id.nav_feedback -> openUrl(Uri.parse(AnkiDroidApp.getFeedbackUrl()))
                else -> {
                }
            }
        }

        drawerLayout!!.closeDrawers()
        return true
    }
    //endregion


    /**
     * This function locks the navigation drawer closed in regards to swipes,
     * but continues to allowed it to be opened via it's indicator button. This
     * function in a noop if the drawer hasn't been initialized.
     */
    protected fun disableDrawerSwipe() = drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

    /**
     * This function allows swipes to open the navigation drawer. This
     * function in a noop if the drawer hasn't been initialized.
     */
    protected fun enableDrawerSwipe() = drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

    /**
     * Open the card browser. Override this method to pass it custom arguments
     */
    protected open fun openCardBrowser() {
        val cardBrowser = Intent(this, CardBrowser::class.java)
        cardBrowser.putExtra("selectedDeck", col!!.decks.selected())
        startActivityForResult(cardBrowser, REQUEST_BROWSE_CARDS)
    }

    protected fun showBackIcon() {
        drawerToggle.isDrawerIndicatorEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mNavButtonGoesBack = true
    }

    // Navigation drawer initialisation
    protected fun initNavigationDrawer(mainView: View) {
        // Create inherited navigation drawer layout here so that it can be used by parent class
//        drawerLayout = mainView.findViewById<View>(R.id.drawerLayout) as DrawerLayout
        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        drawerLayout.navdrawerItemsContainer!!.setNavigationItemSelectedListener(this)
        val toolbar = mainView.findViewById<View>(R.id.toolbar) as Toolbar?
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            // enable ActionBar app icon to behave as action to toggle nav drawer
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)

            // Decide which action to take when the navigation button is tapped.
            toolbar.setNavigationOnClickListener {
                if (mNavButtonGoesBack) {
                    finish();
                } else {
                    drawerLayout!!.openDrawer(Gravity.START)
                }
            }
        }

        // Configure night-mode switch
        val preferences = AnkiDroidApp.getSharedPrefs(this@NavigationDrawerActivity)
        val actionLayout = MenuItemCompat.getActionView(drawerLayout!!.navdrawerItemsContainer!!.menu.findItem(R.id.nav_night_mode))
        mNightModeSwitch = actionLayout.findViewById<View>(R.id.switch_compat) as SwitchCompat
        mNightModeSwitch!!.isChecked = preferences.getBoolean("invertedColors", false)
        mNightModeSwitch!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Timber.i("StudyOptionsFragment:: Night mode was enabled")
                preferences.edit().putBoolean("invertedColors", true).apply()
            } else {
                Timber.i("StudyOptionsFragment:: Night mode was disabled")
                preferences.edit().putBoolean("invertedColors", false).apply()
            }
            CompatHelper.compat.restartActivityInvalidateBackstack(this@NavigationDrawerActivity)
        }
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, 0, 0) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                supportInvalidateOptionsMenu()

                if (pendingRunnable != null) {
                    Handler().post(pendingRunnable)
                    pendingRunnable = null
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                supportInvalidateOptionsMenu()
            }
        }

        drawerLayout!!.addDrawerListener(drawerToggle)
    }


    /** Sets selected navigation drawer item  */
    protected fun selectNavigationItem(itemId: Int) {
        if (drawerLayout?.navdrawerItemsContainer == null) {
            Timber.e("Could not select item in navigation drawer as NavigationView null")
            return
        }
        val menu = drawerLayout!!.navdrawerItemsContainer!!.menu
        if (itemId == -1) {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isChecked = false
            }
        } else {
            val item = menu.findItem(itemId)
            if (item != null) {
                item.isChecked = true
            } else {
                Timber.e("Could not find item %d", itemId)
            }
        }
    }

    override fun setTitle(title: CharSequence) {
        mTitle = title
        supportActionBar?.title = mTitle
    }

    companion object {
        // Intent request codes
        const val REQUEST_PREFERENCES_UPDATE = 100
        const val REQUEST_BROWSE_CARDS = 101
        const val REQUEST_STATISTICS = 102
    }
}