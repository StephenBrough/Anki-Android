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

package com.ichi2.widget

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Pair

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.junkdrawer.MetaDB
import com.ichi2.anki.junkdrawer.services.NotificationService
import com.ichi2.utils.async.BaseAsyncTask

import timber.log.Timber

/**
 * The status of the widget.
 */
object WidgetStatus {

    private var sSmallWidgetEnabled = false
    private var sNotificationEnabled = false
    private var sUpdateDeckStatusAsyncTask: AsyncTask<Context, Void, Context>? = null

    /**
     * Request the widget to update its status.
     */
    fun update(context: Context) {
        val preferences = AnkiDroidApp.getSharedPrefs(context)
        sSmallWidgetEnabled = preferences.getBoolean("widgetSmallEnabled", false)
        sNotificationEnabled = Integer.parseInt(preferences.getString("minimumCardsDueForNotification", "1000001")) < 1000000
        if ((sSmallWidgetEnabled || sNotificationEnabled) && (sUpdateDeckStatusAsyncTask == null || sUpdateDeckStatusAsyncTask!!.status == AsyncTask.Status.FINISHED)) {
            Timber.d("WidgetStatus.update(): updating")
            sUpdateDeckStatusAsyncTask = UpdateDeckStatusAsyncTask()
            sUpdateDeckStatusAsyncTask!!.execute(context)
        } else {
            Timber.d("WidgetStatus.update(): already running or not enabled")
        }
    }

    /** Returns the status of each of the decks.  */
    fun fetchSmall(context: Context): IntArray = MetaDB.getWidgetSmallStatus(context)

    fun fetchDue(context: Context): Int = MetaDB.getNotificationStatus(context)

    private class UpdateDeckStatusAsyncTask : BaseAsyncTask<Context, Void, Context>() {

        override fun doInBackground(vararg params: Context): Context? {
            super.doInBackground(*params)
            Timber.d("WidgetStatus.UpdateDeckStatusAsyncTask.doInBackground()")
            val context = params[0]
            if (!AnkiDroidApp.isSdCardMounted()) {
                return context
            }
            try {
                updateCounts(context)
            } catch (e: Exception) {
                Timber.e(e, "Could not update widget")
            }

            return context
        }


        override fun onPostExecute(context: Context) {
            super.onPostExecute(context)
            Timber.d("WidgetStatus.UpdateDeckStatusAsyncTask.onPostExecute()")
            MetaDB.storeSmallWidgetStatus(context, sSmallWidgetStatus)
            if (sSmallWidgetEnabled) {
                val intent = Intent(context, AnkiDroidWidgetSmall.UpdateService::class.java)
                context.startService(intent)
            }
            if (sNotificationEnabled) {
                val intent = Intent(context, NotificationService::class.java)
                context.startService(intent)
            }
        }

        private fun updateCounts(context: Context) {
            val total = intArrayOf(0, 0, 0)
            val col = CollectionHelper.getInstance().getCol(context)
            // Ensure queues are reset if we cross over to the next day.
            col!!.sched._checkDay()

            // Only count the top-level decks in the total
            val nodes = col.sched.deckDueTree()
            for (node in nodes) {
                total[0] += node.newCount
                total[1] += node.lrnCount
                total[2] += node.revCount
            }
            val due = total[0] + total[1] + total[2]
            val eta = col.sched.eta(total, false)
            sSmallWidgetStatus = Pair(due, eta)
        }

        companion object {

            // due, eta
            private var sSmallWidgetStatus = Pair(0, 0)
        }
    }
}
/** This class should not be instantiated.  */
