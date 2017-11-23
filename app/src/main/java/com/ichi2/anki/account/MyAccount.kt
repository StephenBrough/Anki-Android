/***************************************************************************************
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

package com.ichi2.anki.account

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View

import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.utils.async.Connection
import com.ichi2.utils.async.Connection.Payload
import com.ichi2.utils.themes.StyledProgressDialog
import com.ichi2.utils.hideSoftKeyboard
import kotlinx.android.synthetic.main.my_account.*
import kotlinx.android.synthetic.main.my_account_logged_in.view.*
import kotlinx.android.synthetic.main.toolbar.view.*

import timber.log.Timber

class MyAccount : AnkiActivity() {

    lateinit private var loginView: View
    lateinit private var loggedInView: View

    private var mProgressDialog: ProgressDialog? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mayOpenUrl(Uri.parse(resources.getString(R.string.register_url)))
        initAllContentViews()

        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
        if (preferences.getString("hkey", "").isNotEmpty()) {
            switchToState(STATE_LOGGED_IN)
        } else {
            switchToState(STATE_LOG_IN)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            Timber.i("MyAccount - onBackPressed()")
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun initAllContentViews() {
        loginView = layoutInflater.inflate(R.layout.my_account, null)
        with(loginView) {
            loginButton.setOnClickListener { login() }
            resetPasswordButton.setOnClickListener { resetPassword() }
            signUpButton.setOnClickListener { openUrl(Uri.parse(resources.getString(R.string.register_url))) }
        }

        loggedInView = layoutInflater.inflate(R.layout.my_account_logged_in, null)
        loggedInView.logoutButton.setOnClickListener { logout() }
    }


    private fun switchToState(newState: Int) {
        when (newState) {
            STATE_LOGGED_IN -> {
                val username = AnkiDroidApp.getSharedPrefs(baseContext).getString("username", "")
                loggedInView.usernameLoggedIn.text = username
                loggedInView.toolbar.title = getString(R.string.sync_account)  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                setSupportActionBar(loggedInView.toolbar)
                setContentView(loggedInView)
            }

            STATE_LOG_IN -> {
                loginView.toolbar.title = getString(R.string.sync_account)  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                setSupportActionBar(loginView.toolbar)
                setContentView(loginView)
            }
        }


        supportInvalidateOptionsMenu()  // Needed?
    }


    // Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
    // private boolean isUsernameAndPasswordValid(String username, String password) {
    // return isLoginFieldValid(username) && isLoginFieldValid(password);
    // }
    //
    //
    // private boolean isLoginFieldValid(String loginField) {
    // boolean loginFieldValid = false;
    //
    // if (loginField.length() >= 2 && loginField.matches("[A-Za-z0-9]+")) {
    // loginFieldValid = true;
    // }
    //
    // return loginFieldValid;
    // }

    private fun saveUserInformation(username: String, hkey: String) {
        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
        val editor = preferences.edit()
        editor.putString("username", username)
        editor.putString("hkey", hkey)
        editor.commit()
    }


    private fun login() {
        hideSoftKeyboard()

        val username = username.text.toString().trim { it <= ' ' } // trim spaces, issue 1586
        val password = password.text.toString()

        /*
         * Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
         * if(isUsernameAndPasswordValid(username, password)) { Connection.login(loginListener, new
         * Connection.Payload(new Object[] {username, password})); } else { mInvalidUserPassAlert.show(); }
         */

        if (!username.isBlank() && !password.isBlank()) {
            Connection.login(loginListener, Connection.Payload(arrayOf<Any>(username, password)))
        } else {
            UIUtils.showSimpleSnackbar(this, R.string.invalid_username_password, true)
        }
    }


    private fun logout() {
        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
        val editor = preferences.edit()
        editor.putString("username", "")
        editor.putString("hkey", "")
        editor.commit()
        //  force media resync on deauth
        col!!.media.forceResync()
        switchToState(STATE_LOG_IN)
    }


    private fun resetPassword() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(resources.getString(R.string.resetpw_url))
        startActivity(intent)
    }


    companion object {
        val STATE_LOG_IN = 1
        val STATE_LOGGED_IN = 2
    }

}
