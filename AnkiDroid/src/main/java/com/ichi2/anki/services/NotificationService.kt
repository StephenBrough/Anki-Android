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

package com.ichi2.anki.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.IntentCompat


import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.widget.WidgetStatus

import timber.log.Timber

class NotificationService : Service() {

    /** The notification service to show notifications of due cards.  */
    private var mNotificationManager: NotificationManager? = null


    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.i("NotificationService: OnStartCommand")

        val context = applicationContext
        val preferences = AnkiDroidApp.getSharedPrefs(context)
        val minCardsDue = Integer.parseInt(preferences.getString("minimumCardsDueForNotification", "25"))
        val dueCardsCount = WidgetStatus.fetchDue(context)
        if (dueCardsCount >= minCardsDue) {
            // Build basic notification
            val cardsDueText = getString(R.string.widget_minimum_cards_due_notification_ticker_text, dueCardsCount)
            val builder = NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(context, R.color.material_light_blue_700))
                    .setContentTitle(cardsDueText)
                    .setTicker(cardsDueText)
            // Enable vibrate and blink if set in preferences
            if (preferences.getBoolean("widgetVibrate", false)) {
                builder.setVibrate(longArrayOf(1000, 1000, 1000))
            }
            if (preferences.getBoolean("widgetBlink", false)) {
                builder.setLights(Color.BLUE, 1000, 1000)
            }
            // Creates an explicit intent for an Activity in your app
            val resultIntent = Intent(context, DeckPicker::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or IntentCompat.FLAG_ACTIVITY_CLEAR_TASK
            val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(resultPendingIntent)
            // mId allows you to update the notification later on.
            mNotificationManager!!.notify(WIDGET_NOTIFY_ID, builder.build())
        } else {
            // Cancel the existing notification, if any.
            mNotificationManager!!.cancel(WIDGET_NOTIFY_ID)
        }
        return Service.START_STICKY
    }


    override fun onBind(arg0: Intent): IBinder? = null

    companion object {

        /** The id of the notification for due cards.  */
        private val WIDGET_NOTIFY_ID = 1
    }
}
