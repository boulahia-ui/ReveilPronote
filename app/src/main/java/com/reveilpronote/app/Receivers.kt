package com.reveilpronote.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "C'est l'heure !"
        val prefs = Prefs(context)
        prefs.alarmAt = 0L
        prefs.snoozeUntil = 0L
        Notifier.alarm(context, label)
        // Planifie déjà le réveil suivant
        Sync.enqueueOnce(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = Prefs(context)
        if (prefs.alarmAt > System.currentTimeMillis()) {
            AlarmScheduler.schedule(context, prefs.alarmAt, prefs.alarmLabel ?: "Réveil")
        }
        if (Bridge.isLoggedIn(context)) {
            Sync.schedulePeriodic(context)
            Sync.enqueueOnce(context)
        }
    }
}
