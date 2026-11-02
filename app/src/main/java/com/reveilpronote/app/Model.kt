package com.reveilpronote.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Lesson(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val subject: String,
    val teacher: String,
    val room: String,
    val canceled: Boolean,
    val status: String,
) {
    val teacherAbsent: Boolean get() = status.contains("absent", ignoreCase = true)

    /** Cours qui n'aura pas lieu : annulé ou professeur absent. */
    val notHappening: Boolean
        get() = canceled || teacherAbsent || status.contains("annul", ignoreCase = true)

    val key: String get() = "$start|$subject|$status"
}

/** Identifiant stable d'un enfant (dérivé de son nom). */
fun childId(name: String): String = "c" + Integer.toHexString(name.trim().lowercase().hashCode())

data class ChildData(val name: String, val lessons: List<Lesson>) {
    val id: String get() = childId(name)
}

object Lessons {
    fun parseChildren(raw: String?): List<ChildData> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("children")
        if (arr == null) {
            // Ancien format : un seul élève
            val lessons = parseLessonArray(root.optJSONArray("lessons"))
            return listOf(ChildData(root.optString("name").ifBlank { "Élève" }, lessons))
        }
        val list = ArrayList<ChildData>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += ChildData(o.optString("name").ifBlank { "Enfant ${i + 1}" }, parseLessonArray(o.optJSONArray("lessons")))
        }
        return list
    }

    private fun parseLessonArray(arr: JSONArray?): List<Lesson> {
        if (arr == null) return emptyList()
        val list = ArrayList<Lesson>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val lesson = runCatching {
                Lesson(
                    start = LocalDateTime.parse(o.getString("start")),
                    end = LocalDateTime.parse(o.getString("end")),
                    subject = o.optString("subject"),
                    teacher = o.optString("teacher"),
                    room = o.optString("room"),
                    canceled = o.optBoolean("canceled"),
                    status = o.optString("status"),
                )
            }.getOrNull()
            if (lesson != null) list += lesson
        }
        return list.sortedBy { it.start }
    }

    fun accountName(raw: String?): String? =
        runCatching { JSONObject(raw ?: "").optString("name") }.getOrNull()?.takeIf { it.isNotBlank() }
}

object Planner {
    data class AlarmPlan(val alarmAt: LocalDateTime, val first: Lesson)

    /** Premier cours réel de la prochaine journée dont l'heure de réveil n'est pas encore passée. */
    fun nextAlarm(lessons: List<Lesson>, offsetMinutes: Int, now: LocalDateTime): AlarmPlan? {
        val byDay = lessons.filter { !it.notHappening }.groupBy { it.start.toLocalDate() }.toSortedMap()
        for ((_, dayLessons) in byDay) {
            val first = dayLessons.minByOrNull { it.start } ?: continue
            val at = first.start.minusMinutes(offsetMinutes.toLong())
            if (at.isAfter(now)) return AlarmPlan(at, first)
        }
        return null
    }

    fun newAbsences(lessons: List<Lesson>, now: LocalDateTime, already: Set<String>): List<Lesson> =
        lessons.filter { it.notHappening && it.end.isAfter(now) && it.key !in already }
}

object Format {
    private val day = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    private val hm = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH)

    fun time(t: LocalDateTime): String = t.format(hm)

    fun dayName(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Aujourd'hui"
            today.plusDays(1) -> "Demain"
            else -> d.format(day).replaceFirstChar { it.uppercase() }
        }
    }

    fun dayTime(t: LocalDateTime): String = "${dayName(t.toLocalDate())} à ${time(t)}"

    fun fromMillis(ms: Long): LocalDateTime =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun toMillis(t: LocalDateTime): Long = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
