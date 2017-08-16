/***************************************************************************************
 * Copyright (c) 2012 Norbert Nagold <norbert.nagold></norbert.nagold>@gmail.com>                         *
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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View

import com.ichi2.anim.ActivityTransitionAnimation
import com.ichi2.anki.StudyOptionsFragment.StudyOptionsListener
import com.ichi2.anki.dialogs.CustomStudyDialog
import com.ichi2.widget.WidgetStatus

import timber.log.Timber

class StudyOptionsActivity : NavigationDrawerActivity(), StudyOptionsListener, CustomStudyDialog.CustomStudyListener {


    private val currentFragment: StudyOptionsFragment
        get() = supportFragmentManager.findFragmentById(R.id.studyoptions_frame) as StudyOptionsFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The empty frame layout is a workaround for fragments not showing when they are added
        // to android.R.id.content when an action bar is used in Android 2.1 (and potentially
        // higher) with the appcompat package.
        val mainView = layoutInflater.inflate(R.layout.studyoptions, null)
        setContentView(mainView)
        // create inherited navigation drawer layout here so that it can be used by parent class
        initNavigationDrawer(mainView)
        if (savedInstanceState == null) {
            loadStudyOptionsFragment()
        }
    }

    private fun loadStudyOptionsFragment() {
        var withDeckOptions = false
        if (intent.extras != null) {
            withDeckOptions = intent.extras!!.getBoolean("withDeckOptions")
        }
        val currentFragment = StudyOptionsFragment.newInstance(withDeckOptions)
        supportFragmentManager.beginTransaction().replace(R.id.studyoptions_frame, currentFragment).commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return when (item.itemId) {

            android.R.id.home -> {
                closeStudyOptions()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult (requestCode = %d, resultCode = %d)", requestCode, resultCode)
        currentFragment.restorePreferences()
    }


    private fun closeStudyOptions(result: Int = Activity.RESULT_OK) {
        // mCompat.invalidateOptionsMenu(this);
        setResult(result)
        finishWithAnimation(ActivityTransitionAnimation.RIGHT)
    }


    override fun onBackPressed() {
        if (isDrawerOpen) {
            super.onBackPressed()
        } else {
            Timber.i("Back key pressed")
            closeStudyOptions()
        }
    }


    public override fun onStop() {
        super.onStop()
        if (colIsOpen()) {
            WidgetStatus.update(this)
            UIUtils.saveCollectionInBackground(this)
        }
    }


    public override fun onResume() {
        super.onResume()
        selectNavigationItem(-1)
    }


    override fun onRequireDeckListUpdate() {
        currentFragment.refreshInterface()
    }

    /**
     * Callback methods from CustomStudyDialog
     */
    override fun onCreateCustomStudySession() {
        // Sched already reset by DeckTask in CustomStudyDialog
        currentFragment.refreshInterface()
    }

    override fun onExtendStudyLimits() {
        // Sched needs to be reset so provide true argument
        currentFragment.refreshInterface(true)
    }
}
