package com.reveilpronote.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    const val EXTRA_LABEL = "label"
    const val EXTRA_CHILD = "child"
    const val EXTRA_CHILD_NAME = "child_name"

    /** Une alarme distincte par enfant (action et code propres à l'enfant). */
    private fun fireIntent(ctx: Context, childId: String, childName: String?, label: String?): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, childId.hashCode(),
            Intent(ctx, AlarmReceiver::class.java)
                .setAction("com.reveilpronote.ALARM.$childId")
                .putExtra(EXTRA_CHILD, childId)
                .putExtra(EXTRA_CHILD_NAME, childName)
                .putExtra(EXTRA_LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(ctx: Context, childId: String, childName: String, atMillis: Long, label: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val fire = fireIntent(ctx, childId, childName, label)
        val show = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), fire)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, fire)
        }
    }

    fun cancel(ctx: Context, childId: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(fireIntent(ctx, childId, null, null))
    }
}
