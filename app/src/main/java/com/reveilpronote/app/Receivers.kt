package com.reveilpronote.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childId = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD) ?: return
        val childName = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD_NAME) ?: ""
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "C'est l'heure !"
        val prefs = Prefs(context)
        prefs.setAlarmAt(childId, 0L)
        prefs.setSnoozeUntil(childId, 0L)
        Notifier.alarm(context, childId, childName, label)
        // Planifie déjà le réveil suivant
        Sync.enqueueOnce(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = Prefs(context)
        val now = System.currentTimeMillis()
        for (child in Lessons.parseChildren(prefs.lessonsJson)) {
            val at = prefs.alarmAt(child.id)
            if (at > now) AlarmScheduler.schedule(context, child.id, child.name, at, prefs.alarmLabel(child.id) ?: "Réveil")
        }
        if (Bridge.isLoggedIn(context)) {
            Sync.schedulePeriodic(context)
            Sync.enqueueOnce(context)
        }
    }
}
