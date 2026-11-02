@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.reveilpronote.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReveilTheme { MainScreen(tick = tick, refresh = { tick++ }) } }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

@Composable
private fun MainScreen(tick: Int, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(ctx) }
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val loggedIn = remember(tick) { Bridge.isLoggedIn(ctx) }
    val lessons = remember(tick) { Lessons.parse(prefs.lessonsJson) }

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        snackbar.showSnackbar(m, withDismissAction = true, duration = SnackbarDuration.Long)
        message = null
    }

    fun syncNow() {
        scope.launch {
            busy = "Synchronisation avec Pronote…"
            try {
                Sync.run(ctx)
                message = "Emploi du temps à jour."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Échec de la synchronisation"
            } finally {
                busy = null
                refresh()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réveil Pronote", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            busy?.let { b ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(b)
                }
            }

            if (!loggedIn) {
                SetupCard(
                    enabled = busy == null,
                    onLogin = { qr, pin ->
                        scope.launch {
                            busy = "Connexion à Pronote…"
                            try {
                                val name = withContext(Dispatchers.IO) { Bridge.qrLogin(ctx, qr, pin, prefs.uuid) }
                                prefs.accountName = name
                                Sync.schedulePeriodic(ctx)
                                busy = "Récupération de l'emploi du temps…"
                                Sync.run(ctx)
                                message = "Connecté${if (name.isNotBlank()) " : $name" else ""}."
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = e.message ?: "Connexion impossible"
                            } finally {
                                busy = null
                                refresh()
                            }
                        }
                    },
                )
            } else {
                prefs.accountName?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                AlarmCard(prefs, lessons, refresh)
                AbsencesCard(lessons)
                PermissionsCard(tick, refresh)
                SyncCard(prefs, busy == null, onSync = { syncNow() })
                TextButton(onClick = {
                    Bridge.logout(ctx)
                    AlarmScheduler.cancel(ctx)
                    Sync.stop(ctx)
                    prefs.clearAccount()
                    refresh()
                }) { Text("Se déconnecter") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SetupCard(enabled: Boolean, onLogin: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var qr by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }
    var qrError by remember { mutableStateOf<String?>(null) }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { qr = it.trim(); qrError = null }
    }
    // Lecture du QR code depuis une capture d'écran (tout sur le même téléphone)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.Default) { QrDecoder.decode(ctx, uri) }
                if (text != null) {
                    qr = text.trim()
                    qrError = null
                } else {
                    qrError = "Aucun QR code trouvé dans cette image. Refaites une capture où le QR code est entier et net."
                }
            }
        }
    }

    Section("Connexion à Pronote") {
        Text("Depuis ce téléphone :", fontWeight = FontWeight.SemiBold)
        Text(
            "1. Ouvrez Pronote dans Chrome, puis menu ⋮ → cochez « Version pour ordinateur ».\n" +
                "2. Connectez-vous, puis touchez l'icône QR code en haut de la page (connexion à l'application mobile).\n" +
                "3. Choisissez un code à 4 chiffres : le QR code s'affiche.\n" +
                "4. Faites une capture d'écran, revenez ici et importez-la.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { imagePicker.launch("image/*") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Importer la capture d'écran du QR code")
        }
        OutlinedButton(
            onClick = {
                scanner.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt("Scannez le QR code affiché par Pronote")
                        .setBeepEnabled(false)
                        .setOrientationLocked(false),
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Ou scanner un QR code sur un autre écran") }

        if (qr.isNotBlank()) Text("QR code lu ✓", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        qrError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        TextButton(onClick = { manual = !manual }) { Text(if (manual) "Masquer la saisie manuelle" else "Coller le contenu du QR code") }
        if (manual) {
            OutlinedTextField(
                value = qr, onValueChange = { qr = it },
                label = { Text("Contenu du QR code") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
            )
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
            label = { Text("Code à 4 chiffres") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onLogin(qr.trim(), pin) },
            enabled = enabled && qr.isNotBlank() && pin.length == 4,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Se connecter") }
        Text(
            "Le QR code n'est valable que quelques minutes : enchaînez capture, import et connexion. " +
                "La connexion reste ensuite active sans le ressaisir. Vous pouvez supprimer la capture après.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AlarmCard(prefs: Prefs, lessons: List<Lesson>, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val alarmAt = prefs.alarmAt
    val active = prefs.alarmEnabled && alarmAt > System.currentTimeMillis()

    Section("Prochain réveil") {
        if (active) {
            val at = Format.fromMillis(alarmAt)
            Text(Format.dayName(at.toLocalDate()), style = MaterialTheme.typography.bodyLarge)
            Text(Format.time(at), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            prefs.alarmLabel?.let { Text("Premier cours : $it") }
        } else {
            Text(
                when {
                    !prefs.alarmEnabled -> "Réveil automatique désactivé."
                    lessons.isEmpty() -> "Synchronisez pour calculer le réveil."
                    else -> "Aucun cours trouvé dans les 8 prochains jours."
                },
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Réveil automatique", Modifier.weight(1f))
            Switch(checked = prefs.alarmEnabled, onCheckedChange = {
                prefs.alarmEnabled = it
                Sync.applyAlarm(ctx, lessons)
                refresh()
            })
        }

        Text("Sonner avant le premier cours", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 45, 60, 75, 90, 120).forEach { m ->
                FilterChip(
                    selected = prefs.offsetMinutes == m,
                    onClick = {
                        prefs.offsetMinutes = m
                        Sync.applyAlarm(ctx, lessons)
                        refresh()
                    },
                    label = { Text(if (m < 60) "$m min" else if (m % 60 == 0) "${m / 60} h" else "${m / 60} h ${m % 60}") },
                )
            }
        }
        Text(
            "Si le prof de la première heure est absent ou le cours annulé, le réveil se cale sur le cours suivant.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = {
            ctx.startActivity(
                Intent(ctx, AlarmActivity::class.java)
                    .putExtra(AlarmScheduler.EXTRA_LABEL, "Test de la sonnerie")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }) { Text("Tester la sonnerie") }
    }
}

@Composable
private fun AbsencesCard(lessons: List<Lesson>) {
    val now = LocalDateTime.now()
    val upcoming = lessons.filter { it.notHappening && it.end.isAfter(now) }
    Section("Profs absents et cours annulés") {
        if (upcoming.isEmpty()) {
            Text("Rien de signalé sur les 8 prochains jours.")
        } else {
            upcoming.forEach { l ->
                Column {
                    Text(
                        "${l.subject.ifBlank { "Cours" }} · ${Format.dayTime(l.start)}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    val details = listOf(l.teacher, l.status.ifBlank { if (l.canceled) "Cours annulé" else "" })
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PermissionsCard(tick: Int, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    val needNotif = remember(tick) { !Notifier.canNotify(ctx) }
    val needExact = remember(tick) { !AlarmScheduler.canExact(ctx) }
    val needFullScreen = remember(tick) {
        Build.VERSION.SDK_INT >= 34 && !ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val needBattery = remember(tick) {
        !ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    }
    if (!needNotif && !needExact && !needFullScreen && !needBattery) return

    Section("Autorisations à vérifier") {
        if (needNotif && Build.VERSION.SDK_INT >= 33) {
            PermissionRow("Notifications (profs absents, sonnerie)") {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needExact && Build.VERSION.SDK_INT >= 31) {
            PermissionRow("Alarmes à l'heure exacte") {
                openSettings(ctx, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            }
        }
        if (needFullScreen && Build.VERSION.SDK_INT >= 34) {
            PermissionRow("Affichage plein écran de la sonnerie") {
                openSettings(ctx, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
        }
        if (needBattery) {
            PermissionRow("Ne pas limiter la batterie (synchronisation fiable)") {
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

private fun openSettings(ctx: Context, action: String) {
    runCatching {
        ctx.startActivity(Intent(action, Uri.parse("package:${ctx.packageName}")))
    }.onFailure {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }
}

@Composable
private fun PermissionRow(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text("Autoriser") }
    }
}

@Composable
private fun SyncCard(prefs: Prefs, enabled: Boolean, onSync: () -> Unit) {
    Section("Synchronisation") {
        val last = prefs.lastSync
        Text(
            if (last > 0) "Dernière mise à jour : ${Format.dayTime(Format.fromMillis(last))}" else "Pas encore synchronisé.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Vérification automatique toutes les heures.", style = MaterialTheme.typography.bodySmall)
        prefs.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(onClick = onSync, enabled = enabled) { Text("Synchroniser maintenant") }
    }
}
