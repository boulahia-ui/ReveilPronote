package com.reveilpronote.app

import android.content.Context
import java.util.UUID

/** Réglages et état de l'application (stockage privé). Réglages du réveil propres à chaque enfant. */
class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("reveil_pronote", Context.MODE_PRIVATE)

    /** Identifiant fixe de l'appareil, exigé par Pronote pour la connexion par jeton. */
    val uuid: String
        get() = p.getString("uuid", null) ?: UUID.randomUUID().toString().also { p.edit().putString("uuid", it).apply() }

    var accountName: String?
        get() = p.getString("name", null)
        set(v) = p.edit().putString("name", v).apply()

    var lastSync: Long
        get() = p.getLong("last_sync", 0L)
        set(v) = p.edit().putLong("last_sync", v).apply()

    var lastError: String?
        get() = p.getString("last_error", null)
        set(v) = p.edit().putString("last_error", v).apply()

    var lessonsJson: String?
        get() = p.getString("lessons", null)
        set(v) = p.edit().putString("lessons", v).apply()

    /** Absences déjà signalées (pour ne pas notifier deux fois). */
    var notified: Set<String>
        get() = p.getStringSet("notified", null)?.toSet() ?: emptySet()
        set(v) = p.edit().putStringSet("notified", v).apply()

    // ----- Par enfant -----
    private fun k(child: String, name: String) = "c.$child.$name"

    /** Enfant suivi : réveil et notifications. */
    fun followed(child: String): Boolean = p.getBoolean(k(child, "followed"), true)
    fun setFollowed(child: String, v: Boolean) = p.edit().putBoolean(k(child, "followed"), v).apply()

    fun alarmEnabled(child: String): Boolean = p.getBoolean(k(child, "alarm"), true)
    fun setAlarmEnabled(child: String, v: Boolean) = p.edit().putBoolean(k(child, "alarm"), v).apply()

    /** Minutes entre la sonnerie et le début du premier cours. */
    fun offsetMinutes(child: String): Int = p.getInt(k(child, "offset"), 60)
    fun setOffsetMinutes(child: String, v: Int) = p.edit().putInt(k(child, "offset"), v).apply()

    fun alarmAt(child: String): Long = p.getLong(k(child, "at"), 0L)
    fun setAlarmAt(child: String, v: Long) = p.edit().putLong(k(child, "at"), v).apply()

    fun alarmLabel(child: String): String? = p.getString(k(child, "label"), null)
    fun setAlarmLabel(child: String, v: String?) = p.edit().putString(k(child, "label"), v).apply()

    fun snoozeUntil(child: String): Long = p.getLong(k(child, "snooze"), 0L)
    fun setSnoozeUntil(child: String, v: Long) = p.edit().putLong(k(child, "snooze"), v).apply()

    fun clearAccount() {
        val e = p.edit()
        p.all.keys.filter { it.startsWith("c.") }.forEach { e.remove(it) }
        e.remove("name").remove("lessons").remove("notified").remove("last_sync").remove("last_error").apply()
    }
}
