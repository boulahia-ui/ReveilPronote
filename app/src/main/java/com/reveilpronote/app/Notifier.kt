package com.reveilpronote.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CH_ALARM = "alarm"
    const val CH_ABSENCE = "absences"
    private const val ALARM_ID = 1000

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val alarm = NotificationChannel(CH_ALARM, "Réveil", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Sonnerie avant le premier cours"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
        }
        val absences = NotificationChannel(CH_ABSENCE, "Profs absents et cours annulés", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prévient quand un professeur est absent ou qu'un cours est annulé"
        }
        nm.createNotificationChannels(listOf(alarm, absences))
    }

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun absence(ctx: Context, l: Lesson) {
        if (!canNotify(ctx)) return
        val subject = l.subject.ifBlank { "Cours" }
        val title = if (l.teacherAbsent) "Prof absent : $subject" else "Cours annulé : $subject"
        val details = buildList {
            add(Format.dayTime(l.start))
            if (l.teacher.isNotBlank()) add(l.teacher)
            if (l.status.isNotBlank()) add(l.status)
        }.joinToString(" · ")
        val n = NotificationCompat.Builder(ctx, CH_ABSENCE)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(l.key.hashCode(), n)
    }

    fun alarm(ctx: Context, label: String) {
        val screen = Intent(ctx, AlarmActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_LABEL, label)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val full = PendingIntent.getActivity(
            ctx, 2, screen, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Réveil Pronote")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .setOngoing(true)
            .build()
        if (canNotify(ctx)) ctx.getSystemService(NotificationManager::class.java).notify(ALARM_ID, n)
    }

    fun cancelAlarm(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ALARM_ID)
    }
}
