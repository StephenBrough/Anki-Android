package com.ichi2.anki.account

import android.app.Activity
import android.arch.lifecycle.ViewModel
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.utils.SingleLiveEvent
import com.ichi2.utils.async.Connection
import com.ichi2.utils.themes.StyledProgressDialog
import timber.log.Timber

class MyAccountViewModel : ViewModel() {

    // TODO: LiveData for invalid login creds
    var errorSnackbar = SingleLiveEvent<Int>()
    // TODO: LiveDate for stateSwitch
    var stateSwitch = SingleLiveEvent<Int>()

    private var loginListener: Connection.TaskListener = object : Connection.TaskListener {

        override fun onProgressUpdate(vararg values: Any) {
            // Pass
        }


        override fun onPreExecute() {
//            Timber.d("loginListener.onPreExcecute()")
//            if (mProgressDialog == null || !mProgressDialog!!.isShowing) {
//                mProgressDialog = StyledProgressDialog.show(this@MyAccountActivity, "",
//                        resources.getString(R.string.alert_logging_message), false)
//            }
        }


        override fun onPostExecute(data: Connection.Payload) {
//            if (mProgressDialog != null) {
//                mProgressDialog!!.dismiss()
//            }
//
//            if (data.success) {
//                Timber.i("User successfully logged in!")
//                saveUserInformation(data.data[0] as String, data.data[1] as String)
//
//                val i = this@MyAccountActivity.intent
//                if (i.hasExtra("notLoggedIn") && i.extras!!.getBoolean("notLoggedIn", false)) {
//                    this@MyAccountActivity.setResult(Activity.RESULT_OK, i)
//                    finish()
//                } else {
//                    // Show logged view
//                    loggedInView.usernameLoggedIn.text = data.data[0] as String
//                    switchToState(MyAccountActivity.STATE_LOGGED_IN)
//                }
//            } else {
//                Timber.e("Login failed, errorSnackbar code %d", data.returnType)
//                if (data.returnType == 403) {
//                    errorSnackbar.value = R.string.invalid_username_password
////                    UIUtils.showSimpleSnackbar(this@MyAccountActivity, R.string.invalid_username_password, true)
//                } else {
//                    errorSnackbar.value = R.string.connection_error_message
////                    UIUtils.showSimpleSnackbar(this@MyAccountActivity, R.string.connection_error_message, true)
//                }
//            }
        }


        override fun onDisconnected() {
            errorSnackbar.value = R.string.youre_offline
//            UIUtils.showSimpleSnackbar(this@MyAccountActivity, R.string.youre_offline, true)
        }
    }

    private fun login(username: String, password: String) {

        /*
         * Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
         * if(isUsernameAndPasswordValid(username, password)) { Connection.login(loginListener, new
         * Connection.Payload(new Object[] {username, password})); } else { mInvalidUserPassAlert.show(); }
         */

        if (!username.isBlank() && !password.isBlank()) {
            // TODO: Coroutine
            Connection.login(loginListener, Connection.Payload(arrayOf<Any>(username.trim { it <= ' ' }, password)))
        } else {
            errorSnackbar.value = R.string.invalid_username_password
//            UIUtils.showSimpleSnackbar(this, R.string.invalid_username_password, true)
        }
    }


    private fun logout() {
//        val preferences = AnkiDroidApp.getSharedPrefs(baseContext)
//        val editor = preferences.edit()
//        editor.putString("username", "")
//        editor.putString("hkey", "")
//        editor.commit()
//        //  force media resync on deauth
//        col!!.media.forceResync()
//        stateSwitch.value = MyAccountActivity.STATE_LOG_IN
//        switchToState(MyAccountActivity.STATE_LOG_IN)
    }

}