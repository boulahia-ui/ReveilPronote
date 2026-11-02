package com.reveilpronote.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Sync {
    private const val DAYS = 8
    private const val PERIODIC = "pronote_sync"
    private const val ONCE = "pronote_sync_now"

    // Une seule connexion à la fois : Pronote renouvelle le jeton à chaque connexion
    private val mutex = Mutex()

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    suspend fun run(ctx: Context) {
        mutex.withLock {
            withContext(Dispatchers.IO) { doSync(ctx.applicationContext) }
        }
    }

    private fun doSync(ctx: Context) {
        val prefs = Prefs(ctx)
        try {
            val raw = Bridge.fetch(ctx, DAYS)
            val children = Lessons.parseChildren(raw)
            prefs.lessonsJson = raw
            Lessons.accountName(raw)?.let { prefs.accountName = it }

            // Profs absents / cours annulés : une notification par cours et par enfant suivi
            val now = LocalDateTime.now()
            val already = prefs.notified
            val added = HashSet<String>()
            val showName = children.size > 1
            for (child in children) {
                if (!prefs.followed(child.id)) continue
                for (l in child.lessons) {
                    val key = "${child.id}|${l.key}"
                    if (l.notHappening && l.end.isAfter(now) && key !in already) {
                        Notifier.absence(ctx, if (showName) child.name else "", l, key)
                        added += key
                    }
                }
            }
            val limit = now.toLocalDate().minusDays(1).toString()
            prefs.notified = (already + added).filter { it.split('|').getOrElse(1) { "" } >= limit }.toSet()

            applyAlarms(ctx, children)
            prefs.lastSync = System.currentTimeMillis()
            prefs.lastError = null
        } catch (e: Exception) {
            prefs.lastError = e.message ?: e.javaClass.simpleName
            throw e
        }
    }

    /** Calcule et programme le réveil de chaque enfant à partir des cours connus (sans réseau). */
    fun applyAlarms(ctx: Context, children: List<ChildData> = Lessons.parseChildren(Prefs(ctx).lessonsJson)) {
        val prefs = Prefs(ctx)
        val now = System.currentTimeMillis()
        for (child in children) {
            val id = child.id
            if (prefs.snoozeUntil(id) > now) continue
            val plan = if (prefs.followed(id) && prefs.alarmEnabled(id)) {
                Planner.nextAlarm(child.lessons, prefs.offsetMinutes(id), LocalDateTime.now())
            } else {
                null
            }
            if (plan == null) {
                AlarmScheduler.cancel(ctx, id)
                prefs.setAlarmAt(id, 0L)
                prefs.setAlarmLabel(id, null)
                continue
            }
            val millis = Format.toMillis(plan.alarmAt)
            val label = "${plan.first.subject.ifBlank { "Premier cours" }} à ${Format.time(plan.first.start)}"
            AlarmScheduler.schedule(ctx, id, child.name, millis, label)
            prefs.setAlarmAt(id, millis)
            prefs.setAlarmLabel(id, label)
        }
    }

    fun cancelAll(ctx: Context) {
        for (child in Lessons.parseChildren(Prefs(ctx).lessonsJson)) AlarmScheduler.cancel(ctx, child.id)
    }

    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).setConstraints(network).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun enqueueOnce(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(network).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, req)
    }

    fun stop(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(ctx).cancelUniqueWork(ONCE)
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!Bridge.isLoggedIn(applicationContext)) return Result.success()
        return try {
            Sync.run(applicationContext)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
