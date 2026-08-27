package com.arena.marketradar.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reschedules background price checks after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlertScheduler.schedule(context)
        }
    }
}
