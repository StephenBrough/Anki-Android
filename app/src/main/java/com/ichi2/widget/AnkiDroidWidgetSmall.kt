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

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.IntentHandlerActivity
import com.ichi2.anki.R
import com.ichi2.utils.compat.CompatHelper

import timber.log.Timber

class AnkiDroidWidgetSmall : AppWidgetProvider() {


    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("SmallWidget: onUpdate")
        WidgetStatus.update(context)
    }


    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Timber.d("SmallWidget: Widget enabled")
        val preferences = AnkiDroidApp.getSharedPrefs(context)
        preferences.edit().putBoolean("widgetSmallEnabled", true).commit()
    }


    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Timber.d("SmallWidget: Widget disabled")
        val preferences = AnkiDroidApp.getSharedPrefs(context)
        preferences.edit().putBoolean("widgetSmallEnabled", false).commit()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action!!.contentEquals("com.sec.android.widgetapp.APPWIDGET_RESIZE")) {
            CompatHelper.compat.updateWidgetDimensions(context, RemoteViews(context.packageName, R.layout.widget_small), AnkiDroidWidgetSmall::class.java)
        }
        super.onReceive(context, intent)
    }

    class UpdateService : Service() {

        /** The cached number of total due cards.  */
        private var dueCardsCount: Int = 0

        /** The cached estimated reviewing time.  */
        private var eta: Int = 0

        override fun onStart(intent: Intent, startId: Int) {
            Timber.i("SmallWidget: OnStart")

            val updateViews = buildUpdate(this, true)

            val thisWidget = ComponentName(this, AnkiDroidWidgetSmall::class.java)
            val manager = AppWidgetManager.getInstance(this)
            manager.updateAppWidget(thisWidget, updateViews)
        }

        private fun buildUpdate(context: Context, updateDueDecksNow: Boolean): RemoteViews {
            Timber.d("buildUpdate")

            val updateViews = RemoteViews(context.packageName, R.layout.widget_small)

            val mounted = AnkiDroidApp.isSdCardMounted()
            if (!mounted) {
                updateViews.setViewVisibility(R.id.widget_due, View.INVISIBLE)
                updateViews.setViewVisibility(R.id.widget_eta, View.INVISIBLE)
                updateViews.setViewVisibility(R.id.ankidroid_widget_small_finish_layout, View.GONE)

                if (mMountReceiver == null) {
                    mMountReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val action = intent.action
                            if (action == Intent.ACTION_MEDIA_MOUNTED) {
                                Timber.d("mMountReceiver - Action = Media Mounted")
                                if (remounted) {
                                    WidgetStatus.update(baseContext)
                                    remounted = false
                                    if (mMountReceiver != null) {
                                        unregisterReceiver(mMountReceiver)
                                    }
                                } else {
                                    remounted = true
                                }
                            }
                        }
                    }
                    val iFilter = IntentFilter()
                    iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
                    iFilter.addDataScheme("file")
                    registerReceiver(mMountReceiver, iFilter)
                }
            } else {
                // If we do not have a cached version, always update.
                if (dueCardsCount == 0 || updateDueDecksNow) {
                    // Compute the total number of cards due.
                    val counts = WidgetStatus.fetchSmall(context)
                    dueCardsCount = counts[0]
                    eta = counts[1]
                    if (dueCardsCount <= 0) {
                        if (dueCardsCount == 0) {
                            updateViews.setViewVisibility(R.id.ankidroid_widget_small_finish_layout, View.VISIBLE)
                        } else {
                            updateViews.setViewVisibility(R.id.ankidroid_widget_small_finish_layout, View.INVISIBLE)
                        }
                        updateViews.setViewVisibility(R.id.widget_due, View.INVISIBLE)
                    } else {
                        updateViews.setViewVisibility(R.id.ankidroid_widget_small_finish_layout, View.INVISIBLE)
                        updateViews.setViewVisibility(R.id.widget_due, View.VISIBLE)
                        updateViews.setTextViewText(R.id.widget_due, Integer.toString(dueCardsCount))
                        updateViews.setContentDescription(R.id.widget_due, resources.getQuantityString(R.plurals.widget_cards_due, dueCardsCount, dueCardsCount))
                    }
                    if (eta <= 0 || dueCardsCount <= 0) {
                        updateViews.setViewVisibility(R.id.widget_eta, View.INVISIBLE)
                    } else {
                        updateViews.setViewVisibility(R.id.widget_eta, View.VISIBLE)
                        updateViews.setTextViewText(R.id.widget_eta, Integer.toString(eta))
                        updateViews.setContentDescription(R.id.widget_eta, resources.getQuantityString(R.plurals.widget_eta, eta, eta))
                    }
                }
            }

            // Add a click listener to open Anki from the icon.
            // This should be always there, whether there are due cards or not.
            val ankiDroidIntent = Intent(context, IntentHandlerActivity::class.java)
            ankiDroidIntent.action = Intent.ACTION_MAIN
            ankiDroidIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pendingAnkiDroidIntent = PendingIntent.getActivity(context, 0, ankiDroidIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            updateViews.setOnClickPendingIntent(R.id.ankidroid_widget_small_button, pendingAnkiDroidIntent)

            CompatHelper.compat.updateWidgetDimensions(context, updateViews, AnkiDroidWidgetSmall::class.java)

            return updateViews
        }

        override fun onBind(arg0: Intent): IBinder? {
            Timber.d("onBind")
            return null
        }

    }

    companion object {

        private var mMountReceiver: BroadcastReceiver? = null
        private var remounted = false
    }

}
