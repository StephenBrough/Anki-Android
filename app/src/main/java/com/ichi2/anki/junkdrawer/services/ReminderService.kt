package com.ichi2.anki.junkdrawer.services

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat

import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.IntentHandlerActivity
import com.ichi2.anki.R
import com.ichi2.anki.junkdrawer.receiver.ReminderReceiver
import com.ichi2.libanki.Sched

class ReminderService : IntentService("ReminderService") {

    override fun onHandleIntent(intent: Intent?) {
        val deckId = intent!!.getLongExtra(ReminderService.EXTRA_DECK_ID, 0)

        if (null == CollectionHelper.getInstance().getCol(this)!!.decks.get(deckId, false)) {
            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val reminderIntent = PendingIntent.getBroadcast(
                    this,
                    deckId.toInt(),
                    Intent(this, ReminderReceiver::class.java).putExtra(ReminderService.EXTRA_DECK_ID, deckId),
                    0
            )

            alarmManager.cancel(reminderIntent)
        }

        val deckDue: Sched.DeckDueTreeNode = CollectionHelper.getInstance().getCol(this)!!.sched.deckDueTree().firstOrNull { it.did == deckId } ?: return

        val total = deckDue.revCount + deckDue.lrnCount + deckDue.newCount

        if (total <= 0) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)

        if (notificationManager.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(this)
                    .setContentTitle(this.getString(R.string.reminder_title))
                    .setContentText(this.resources.getQuantityString(
                            R.plurals.reminder_text,
                            deckDue.newCount,
                            CollectionHelper.getInstance().getCol(this)!!.decks.name(deckId),
                            total
                    ))
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.material_light_blue_700))
                    .setContentIntent(PendingIntent.getActivity(
                            this,
                            deckId.toInt(),
                            Intent(this, IntentHandlerActivity::class.java).putExtra(EXTRA_DECK_ID, deckId),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    ))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(deckId.toInt(), notification)
        }
    }

    companion object {
        const val EXTRA_DECK_ID = "EXTRA_DECK_ID"
    }
}
