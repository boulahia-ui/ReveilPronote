package com.reveilpronote.app

import android.content.Context
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.IOException
import java.time.LocalDate

/** Appels au module Python pronote_bridge (pronotepy). Fonctions bloquantes : à appeler hors du thread principal. */
object Bridge {
    private fun module(ctx: Context): PyObject {
        if (!Python.isStarted()) Python.start(AndroidPlatform(ctx.applicationContext))
        return Python.getInstance().getModule("pronote_bridge")
    }

    fun credsFile(ctx: Context): File = File(ctx.filesDir, "pronote_session.json")

    fun isLoggedIn(ctx: Context): Boolean = credsFile(ctx).exists()

    fun qrLogin(ctx: Context, qrText: String, pin: String, uuid: String): String =
        call { module(ctx).callAttr("qr_login", credsFile(ctx).absolutePath, qrText, pin, uuid).toString() }

    fun fetch(ctx: Context, days: Int): String =
        call { module(ctx).callAttr("fetch", credsFile(ctx).absolutePath, LocalDate.now().toString(), days).toString() }

    fun logout(ctx: Context) {
        credsFile(ctx).delete()
    }

    private fun <T> call(block: () -> T): T {
        try {
            return block()
        } catch (e: PyException) {
            throw IOException(explain(e.message))
        }
    }

    private fun explain(raw: String?): String {
        val m = raw ?: return "Erreur inconnue"
        return when {
            m.contains("CryptoError") ->
                "Connexion refusée : code à 4 chiffres incorrect, QR code expiré ou session invalide. Générez un nouveau QR code dans Pronote."
            m.contains("suspend", ignoreCase = true) ->
                "Pronote a temporairement bloqué les connexions. Réessayez plus tard."
            m.contains("ConnectionError") || m.contains("Max retries") || m.contains("NameResolution") ->
                "Impossible de joindre Pronote. Vérifiez la connexion Internet."
            m.contains("FileNotFoundError") -> "Aucun compte Pronote connecté."
            else -> m.lineSequence().firstOrNull { it.isNotBlank() }?.take(240) ?: m.take(240)
        }
    }
}
