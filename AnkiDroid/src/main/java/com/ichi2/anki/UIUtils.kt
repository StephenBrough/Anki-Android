package com.ichi2.anki

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.ichi2.async.DeckTask
import com.ichi2.async.DeckTask.TaskData

import java.util.Calendar

import timber.log.Timber

object UIUtils {


    val dayStart: Long
        get() {
            val cal = Calendar.getInstance()
            if (cal.get(Calendar.HOUR_OF_DAY) < 4) {
                cal.roll(Calendar.DAY_OF_YEAR, -1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 4)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    fun showThemedToast(context: Context, text: String, shortLength: Boolean) {
        Toast.makeText(context, text, if (shortLength) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }


    /**
     * Show a simple Toast-like Snackbar with no actions.
     * To enable swipe-to-dismiss, the Activity layout should include a CoordinatorLayout with id "root_layout"
     * @param mainTextResource
     * @param shortLength
     */
    fun showSimpleSnackbar(activity: Activity, mainTextResource: Int, shortLength: Boolean) {
        val root = activity.findViewById<View>(R.id.root_layout)
        showSnackbar(activity, mainTextResource, shortLength, -1, null, root)
    }

    fun showSimpleSnackbar(activity: Activity, mainText: String, shortLength: Boolean) {
        val root = activity.findViewById<View>(R.id.root_layout)
        showSnackbar(activity, mainText, shortLength, -1, null, root, null)
    }


    @JvmOverloads
    fun showSnackbar(activity: Activity, mainTextResource: Int, shortLength: Boolean,
                     actionTextResource: Int, listener: View.OnClickListener?, root: View,
                     callback: Snackbar.Callback? = null) {
        val mainText = activity.resources.getString(mainTextResource)
        showSnackbar(activity, mainText, shortLength, actionTextResource, listener, root, callback)
    }


    fun showSnackbar(activity: Activity, mainText: String, shortLength: Boolean,
                     actionTextResource: Int, listener: View.OnClickListener?, root: View?,
                     callback: Snackbar.Callback?) {
        var root = root
        if (root == null) {
            root = activity.findViewById(android.R.id.content)
            if (root == null) {
                Timber.e("Could not show Snackbar due to null View")
                return
            }
        }
        val length = if (shortLength) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
        val sb = Snackbar.make(root, mainText, length)
        if (listener != null) {
            sb.setAction(actionTextResource, listener)
        }
        if (callback != null) {
            sb.setCallback(callback)
        }
        // Make the text white to avoid interference from our theme colors.
        val view = sb.view
        val tv = view.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
        val action = view.findViewById<View>(android.support.design.R.id.snackbar_action) as TextView
        if (tv != null && action != null) {
            tv.setTextColor(Color.WHITE)
            action.setTextColor(ContextCompat.getColor(activity, R.color.material_light_blue_500))
            tv.maxLines = 2  // prevent tablets from truncating to 1 line
        }
        sb.show()
    }


    fun getDensityAdjustedValue(context: Context, value: Float): Float =
            context.resources.displayMetrics.density * value


    fun saveCollectionInBackground(context: Context) {
        if (CollectionHelper.getInstance().colIsOpen()) {
            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_SAVE_COLLECTION, object : DeckTask.TaskListener() {
                override fun onPreExecute() {
                    Timber.d("saveCollectionInBackground: start")
                }


                override fun onPostExecute(result: TaskData) {
                    Timber.d("saveCollectionInBackground: finished")
                }


                override fun onProgressUpdate(vararg values: TaskData) {}


                override fun onCancelled() {}
            })
        }
    }
}
/**
 * Show a snackbar with an action
 * @param mainTextResource resource for the main text string
 * @param shortLength whether or not to use long length
 * @param actionTextResource resource for the text string shown as the action
 * @param listener listener for the action (if null no action shown)
 * @oaram root View Snackbar will attach to. Should be CoordinatorLayout for swipe-to-dismiss to work.
 */
