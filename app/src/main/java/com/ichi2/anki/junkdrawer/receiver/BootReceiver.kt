package com.ichi2.anki.junkdrawer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.ichi2.anki.junkdrawer.services.BootService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, BootService::class.java)

        context.startService(serviceIntent)
    }
}
