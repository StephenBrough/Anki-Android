package com.ichi2.anki.junkdrawer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.ichi2.anki.junkdrawer.services.ReminderService

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ReminderService::class.java)

        serviceIntent.putExtra(ReminderService.EXTRA_DECK_ID, intent.getLongExtra(ReminderService.EXTRA_DECK_ID, 0))
        context.startService(serviceIntent)
    }
}
