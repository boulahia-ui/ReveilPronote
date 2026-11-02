package com.reveilpronote.app

import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Sonnerie d'un enfant (plusieurs enfants peuvent sonner en même temps). */
data class AlarmEntry(val childId: String, val childName: String, val label: String)

/** Écran de sonnerie, affiché même téléphone verrouillé. */
class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val entries = mutableStateListOf<AlarmEntry>()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        addEntry(intent)
        startRinging()

        setContent {
            ReveilTheme {
                AlarmScreen(entries = entries, onStop = { stopAndClose() }, onSnooze = { snooze() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        addEntry(intent)
        if (ringtone == null) startRinging()
    }

    private fun addEntry(intent: Intent?) {
        val childId = intent?.getStringExtra(AlarmScheduler.EXTRA_CHILD) ?: "test"
        val childName = intent?.getStringExtra(AlarmScheduler.EXTRA_CHILD_NAME) ?: ""
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "Premier cours"
        Notifier.cancelAlarm(this, childId)
        entries.removeAll { it.childId == childId }
        entries += AlarmEntry(childId, childName, label)
    }

    private fun startRinging() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= 28) isLooping = true
            play()
        }
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 700), 0))
    }

    private fun stopRinging() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        ringtone = null
        vibrator = null
    }

    private fun stopAndClose() {
        stopRinging()
        finish()
    }

    private fun snooze() {
        stopRinging()
        val at = System.currentTimeMillis() + 10 * 60_000L
        val prefs = Prefs(this)
        for (e in entries) {
            if (e.childId == "test") continue
            prefs.setSnoozeUntil(e.childId, at)
            prefs.setAlarmAt(e.childId, at)
            prefs.setAlarmLabel(e.childId, e.label)
            AlarmScheduler.schedule(this, e.childId, e.childName, at, e.label)
        }
        finish()
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}

@Composable
private fun AlarmScreen(entries: List<AlarmEntry>, onStop: () -> Unit, onSnooze: () -> Unit) {
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1E3A))
            .systemBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(now, color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Light)
            entries.forEach { e ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (e.childName.isNotBlank()) "Réveil de ${e.childName}" else "Premier cours",
                        color = Color(0xFFB9C6DD), fontSize = 18.sp,
                    )
                    Text(e.label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0E1E3A)),
            ) { Text("Arrêter", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Text("Répéter dans 10 min", fontSize = 17.sp) }
        }
    }
}
