package com.ichi2.anki.junkdrawer.services

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.junkdrawer.receiver.ReminderReceiver

import org.json.JSONException

import java.util.Calendar

class BootService : IntentService("BootService") {

    override fun onHandleIntent(intent: Intent?) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            for (deck in CollectionHelper.getInstance().getCol(this)!!.decks.all()) {
                val col = CollectionHelper.getInstance().getCol(this)
                if (col!!.decks.isDyn(deck.getLong("id"))) {
                    continue
                }
                val deckConfigurationId = deck.getLong("conf")
                val deckConfiguration = col.decks.getConf(deckConfigurationId)

                if (deckConfiguration.has("reminder")) {
                    val reminder = deckConfiguration.getJSONObject("reminder")

                    if (reminder.getBoolean("enabled")) {
                        val reminderIntent = PendingIntent.getBroadcast(
                                this,
                                deck.getLong("id").toInt(),
                                Intent(this, ReminderReceiver::class.java).putExtra(ReminderService.EXTRA_DECK_ID,
                                        deck.getLong("id")),
                                0
                        )
                        val calendar = Calendar.getInstance()

                        calendar.set(Calendar.HOUR_OF_DAY, reminder.getJSONArray("time").getInt(0))
                        calendar.set(Calendar.MINUTE, reminder.getJSONArray("time").getInt(1))
                        calendar.set(Calendar.SECOND, 0)

                        alarmManager.setInexactRepeating(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                AlarmManager.INTERVAL_DAY,
                                reminderIntent
                        )
                    }
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }
}
