package dev.nick.stepcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateChangeReceiver(
    private val onDateChanged: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_DATE_CHANGED ||
            intent?.action == Intent.ACTION_TIME_CHANGED ||
            intent?.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            onDateChanged()
        }
    }
}