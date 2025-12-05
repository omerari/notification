package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            Logger.log(context, "BootReceiver tetiklendi, alarm yeniden kuruluyor.")
            MainActivity.setRecurringAlarm(context)
        }
    }
}
