package com.reveilpronote.app

import android.content.Context
import java.util.UUID

/** Réglages et état de l'application (stockage privé). */
class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("reveil_pronote", Context.MODE_PRIVATE)

    /** Identifiant fixe de l'appareil, exigé par Pronote pour la connexion par jeton. */
    val uuid: String
        get() = p.getString("uuid", null) ?: UUID.randomUUID().toString().also { p.edit().putString("uuid", it).apply() }

    var accountName: String?
        get() = p.getString("name", null)
        set(v) = p.edit().putString("name", v).apply()

    var alarmEnabled: Boolean
        get() = p.getBoolean("alarm_enabled", true)
        set(v) = p.edit().putBoolean("alarm_enabled", v).apply()

    /** Minutes entre la sonnerie et le début du premier cours. */
    var offsetMinutes: Int
        get() = p.getInt("offset", 60)
        set(v) = p.edit().putInt("offset", v).apply()

    var alarmAt: Long
        get() = p.getLong("alarm_at", 0L)
        set(v) = p.edit().putLong("alarm_at", v).apply()

    var alarmLabel: String?
        get() = p.getString("alarm_label", null)
        set(v) = p.edit().putString("alarm_label", v).apply()

    var snoozeUntil: Long
        get() = p.getLong("snooze_until", 0L)
        set(v) = p.edit().putLong("snooze_until", v).apply()

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

    fun clearAccount() {
        p.edit()
            .remove("name").remove("lessons").remove("notified")
            .remove("alarm_at").remove("alarm_label").remove("snooze_until")
            .remove("last_sync").remove("last_error")
            .apply()
    }
}
