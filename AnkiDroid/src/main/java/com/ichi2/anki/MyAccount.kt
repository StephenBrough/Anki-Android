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

package com.ichi2.anki

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anim.ActivityTransitionAnimation
import com.ichi2.async.Connection
import com.ichi2.async.Connection.Payload
import com.ichi2.themes.StyledProgressDialog

import timber.log.Timber

class MyAccount : AnkiActivity() {

    private var mLoginToMyAccountView: View? = null
    private var mLoggedIntoMyAccountView: View? = null

    private var mUsername: EditText? = null
    private var mPassword: EditText? = null

    private var mUsernameLoggedIn: TextView? = null

    private var mProgressDialog: MaterialDialog? = null
    internal var mToolbar: Toolbar? = null


    /**
     * Listeners
     */
    internal var loginListener: Connection.TaskListener = object : Connection.TaskListener {

        override fun onProgressUpdate(vararg values: Any) {
            // Pass
        }


        override fun onPreExecute() {
            Timber.d("loginListener.onPreExcecute()")
            if (mProgressDialog == null || !mProgressDialog!!.isShowing) {
                mProgressDialog = StyledProgressDialog.show(this@MyAccount, "",
                        resources.getString(R.string.alert_logging_message), false)
            }
        }


        override fun onPostExecute(data: Payload) {
            if (mProgressDialog != null) {
                mProgressDialog!!.dismiss()
            }

            if (data.success) {
                Timber.i("User successfully logged in!")
                saveUserInformation(data.data[0] as String, data.data[1] as String)

                val i = this@MyAccount.intent
                if (i.hasExtra("notLoggedIn") && i.extras!!.getBoolean("notLoggedIn", false)) {
                    this@MyAccount.setResult(Activity.RESULT_OK, i)
                    finishWithAnimation(ActivityTransitionAnimation.FADE)
                } else {
                    // Show logged view
                    mUsernameLoggedIn!!.text = data.data[0] as String
                    switchToState(STATE_LOGGED_IN)
                }
            } else {
                Timber.e("Login failed, error code %d", data.returnType)
                if (data.returnType == 403) {
                    UIUtils.showSimpleSnackbar(this@MyAccount, R.string.invalid_username_password, true)
                } else {
                    UIUtils.showSimpleSnackbar(this@MyAccount, R.string.connection_error_message, true)
                }
            }
        }


        override fun onDisconnected() {
            UIUtils.showSimpleSnackbar(this@MyAccount, R.string.youre_offline, true)
        }
    }


    private fun switchToState(newState: Int) {
        when (newState) {
            STATE_LOGGED_IN -> {
                val username = AnkiDroidApp.getSharedPrefs(baseContext).getString("username", "")
                mUsernameLoggedIn!!.text = username
                mToolbar = mLoggedIntoMyAccountView!!.findViewById<View>(R.id.toolbar) as Toolbar
                if (mToolbar != null) {
                    mToolbar!!.title = getString(R.string.sync_account)  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                    setSupportActionBar(mToolbar)
                }
                setContentView(mLoggedIntoMyAccountView)
            }

            STATE_LOG_IN -> {
                mToolbar = mLoginToMyAccountView!!.findViewById<View>(R.id.toolbar) as Toolbar
                if (mToolbar != null) {
                    mToolbar!!.title = getString(R.string.sync_account)  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                    setSupportActionBar(mToolbar)
                }
                setContentView(mLoginToMyAccountView)
            }
        }


        supportInvalidateOptionsMenu()  // Needed?
    }

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
        // Hide soft keyboard
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(mUsername!!.windowToken, 0)

        val username = mUsername!!.text.toString().trim { it <= ' ' } // trim spaces, issue 1586
        val password = mPassword!!.text.toString()

        /*
         * Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
         * if(isUsernameAndPasswordValid(username, password)) { Connection.login(loginListener, new
         * Connection.Payload(new Object[] {username, password})); } else { mInvalidUserPassAlert.show(); }
         */

        if (!"".equals(username, ignoreCase = true) && !"".equals(password, ignoreCase = true)) {
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
        col.media.forceResync()
        switchToState(STATE_LOG_IN)
    }


    private fun resetPassword() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(resources.getString(R.string.resetpw_url))
        startActivity(intent)
    }


    private fun initAllContentViews() {
        mLoginToMyAccountView = layoutInflater.inflate(R.layout.my_account, null)
        mUsername = mLoginToMyAccountView!!.findViewById<View>(R.id.username) as EditText
        mPassword = mLoginToMyAccountView!!.findViewById<View>(R.id.password) as EditText

        val loginButton = mLoginToMyAccountView!!.findViewById<View>(R.id.login_button) as Button
        loginButton.setOnClickListener { login() }

        val resetPWButton = mLoginToMyAccountView!!.findViewById<View>(R.id.reset_password_button) as Button
        resetPWButton.setOnClickListener { resetPassword() }

        val signUpButton = mLoginToMyAccountView!!.findViewById<View>(R.id.sign_up_button) as Button
        signUpButton.setOnClickListener { openUrl(Uri.parse(resources.getString(R.string.register_url))) }

        mLoggedIntoMyAccountView = layoutInflater.inflate(R.layout.my_account_logged_in, null)
        mUsernameLoggedIn = mLoggedIntoMyAccountView!!.findViewById<View>(R.id.username_logged_in) as TextView
        val logoutButton = mLoggedIntoMyAccountView!!.findViewById<View>(R.id.logout_button) as Button
        logoutButton.setOnClickListener { logout() }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            Timber.i("MyAccount - onBackPressed()")
            finishWithAnimation(ActivityTransitionAnimation.FADE)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private val STATE_LOG_IN = 1
        private val STATE_LOGGED_IN = 2
    }

}
