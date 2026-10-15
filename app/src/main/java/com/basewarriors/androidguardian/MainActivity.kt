package com.basewarriors.androidguardian

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val reminders = mutableStateListOf<String>()
    private var reminderText by mutableStateOf("")
    private var statusText by mutableStateOf("Estoy aquí para ayudarte a usar menos el móvil, no más. 💜")
    private var batteryText by mutableStateOf("Batería: consultando…")
    private var weatherText by mutableStateOf("Pulsa «Consultar clima» cuando quieras saber si llevar paraguas.")
    private var speechText by mutableStateOf("Puedes hablarme con el botón «Hablar» o elegir una acción.")
    private var ttsReady = false
    private var tts: TextToSpeech? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra("level", -1)
            val scale = intent.getIntExtra("scale", 100)
            if (level < 0 || scale <= 0) return
            val percent = level * 100 / scale
            batteryText = "$percent%"
            val prefs = getSharedPreferences("guardian_battery", Context.MODE_PRIVATE)
            if (percent > 33) prefs.edit().putBoolean("alert30", false).apply()
            if (percent > 23) prefs.edit().putBoolean("alert20", false).apply()
            if (percent <= 30 && !prefs.getBoolean("alert30", false)) {
                notifyUser("Batería al 30%", "Cuando te venga bien, conecta el móvil para no quedarte sin batería.", 300)
                prefs.edit().putBoolean("alert30", true).apply()
            }
            if (percent <= 20 && !prefs.getBoolean("alert20", false)) {
                notifyUser("Batería al 20%", "La batería está baja. Busca un cargador cuando puedas.", 200)
                prefs.edit().putBoolean("alert20", true).apply()
            }
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadWeather()
        } else {
            weatherText = "Ubicación denegada. Puedes cambiarlo en Ajustes; no guardamos tu posición."
            speak("No tengo permiso para consultar tu ubicación. Puedes cambiarlo en ajustes.")
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusText = if (granted) "Notificaciones habilitadas." else
            "Las notificaciones están desactivadas. Puedes habilitarlas en Ajustes."
    }

    private val speechInput = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (spoken.isNullOrBlank()) {
            speechText = "No te entendí esta vez. Puedes probar de nuevo o tocar una opción."
        } else {
            speechText = "Te escuché: «$spoken»"
            handleVoiceCommand(spoken)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        updateBattery()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Android Guardian 💜", style = MaterialTheme.typography.headlineMedium)
                            Text("Tu compañera de bienestar · versión 0.2")
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("¡Hola! Vamos de a poco.", style = MaterialTheme.typography.titleLarge)
                                    Text("Estoy para ayudarte a hacer pausas y organizarte, no para retenerte en el teléfono.")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { startVoiceInput() }) { Text("🎙 Hablar") }
                                        Button(onClick = { speak("Hola. ¿Qué necesitas? Puedes decir recordatorio, agua, estudiar, salir, batería o clima.") }) { Text("🔊 Escucharme") }
                                    }
                                    Text(speechText)
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✨ Acciones rápidas", style = MaterialTheme.typography.titleLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { quickReminder("Tomar agua", 60) }) { Text("💧 Agua") }
                                        Button(onClick = { quickReminder("Pausa de descanso", 90) }) { Text("🌿 Pausa") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { quickReminder("Hora de estudiar", 30) }) { Text("📚 Estudiar") }
                                        Button(onClick = { quickReminder("Prepararme para salir", 45) }) { Text("🚶 Salir") }
                                    }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⏰ Mi recordatorio", style = MaterialTheme.typography.titleLarge)
                                    OutlinedTextField(
                                        value = reminderText,
                                        onValueChange = { reminderText = it },
                                        label = { Text("¿Qué quieres recordar?") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { addReminder(5) }, enabled = reminderText.isNotBlank()) { Text("En 5 min") }
                                        Button(onClick = { addReminder(60) }, enabled = reminderText.isNotBlank()) { Text("En 1 hora") }
                                    }
                                    Text("Los avisos se programan en este teléfono. Android puede retrasarlos por ahorro de batería.")
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🔋 Batería", style = MaterialTheme.typography.titleLarge)
                                    Text(batteryText)
                                    Text("Avisos discretos al cruzar 30% y 20% mientras Android entrega actualizaciones a la app.")
                                    Button(onClick = { updateBattery(); speak("La batería está en $batteryText") }) { Text("Actualizar batería") }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🌦️ Clima local", style = MaterialTheme.typography.titleLarge)
                                    Text(weatherText)
                                    Button(onClick = { requestWeatherPermission() }) { Text("Consultar clima con GPS") }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🛡️ Estado y privacidad", style = MaterialTheme.typography.titleLarge)
                                    Text(statusText)
                                    Text("La voz usa el servicio de reconocimiento disponible en tu Android y puede depender de Internet. El clima envía coordenadas a Open-Meteo solo al consultarlo.")
                                    Text("No diagnostico problemas médicos ni cambio tratamientos.")
                                }
                            }
                        }
                        if (reminders.isNotEmpty()) {
                            item { Text("Recordatorios de esta sesión", style = MaterialTheme.typography.titleMedium) }
                            items(reminders) { Text("• $it") }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(batteryReceiver, filter)
    }

    override fun onStop() {
        try { unregisterReceiver(batteryReceiver) } catch (_: IllegalArgumentException) { }
        super.onStop()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale("es", "AR")
    }

    private fun speak(message: String) {
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "guardian-speech")
        else statusText = "La voz todavía no está disponible en este dispositivo."
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué necesitas")
        }
        try {
            speechInput.launch(intent)
        } catch (_: Exception) {
            speechText = "Este teléfono no tiene un servicio de reconocimiento de voz disponible. Puedes usar los botones."
        }
    }

    private fun handleVoiceCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        when {
            "bater" in command -> { updateBattery(); speak("La batería está en $batteryText") }
            "clima" in command || "lluvia" in command || "paraguas" in command -> { requestWeatherPermission(); speak("Voy a consultar el clima con tu permiso.") }
            "agua" in command -> quickReminder("Tomar agua", 60)
            "estudi" in command -> quickReminder("Hora de estudiar", 30)
            "pausa" in command || "descans" in command -> quickReminder("Pausa de descanso", 60)
            "salir" in command || "caminar" in command -> quickReminder("Prepararme para salir", 45)
            "recordatorio" in command || "recuerd" in command -> {
                val cleaned = raw.replace(Regex("(?i)^(pon(me)?|crea(me)?|añade|agrega)?\\s*(un )?(recordatorio|recuérdame)\\s*(de|para)?\\s*"), "").trim()
                reminderText = cleaned.ifBlank { raw }
                speak("He preparado el texto. Elige cinco minutos o una hora para programarlo.")
            }
            else -> {
                reminderText = raw
                speak("He puesto lo que dijiste en el campo de recordatorio. Elige cuándo quieres que te avise.")
            }
        }
    }

    private fun quickReminder(title: String, minutes: Long) {
        reminderText = title
        addReminder(minutes)
        speak("Listo. Te avisaré de $title en $minutes minutos.")
    }

    private fun addReminder(delayMinutes: Long) {
        val title = reminderText.trim()
        if (title.isEmpty()) return
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, "Android Guardian 💜")
            putExtra(ReminderReceiver.EXTRA_BODY, title)
            putExtra(ReminderReceiver.EXTRA_ID, id)
        }
        val pending = PendingIntent.getBroadcast(
            this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = System.currentTimeMillis() + delayMinutes * 60_000L
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        reminders.add("$title · en $delayMinutes min")
        reminderText = ""
        statusText = "Recordatorio programado: $title. Android puede retrasarlo por ahorro de batería."
        notifyUser("Recordatorio programado", "$title · en $delayMinutes minutos", id)
    }

    private fun notifyUser(title: String, body: String, id: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(ReminderReceiver.CHANNEL_ID, "Recordatorios y bienestar", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(this, ReminderReceiver.CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION") android.app.Notification.Builder(this)
            }.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()
            manager.notify(id, notification)
        }
    }

    private fun updateBattery() {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra("level", -1) ?: -1
        val scale = battery?.getIntExtra("scale", 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        batteryText = if (percent >= 0) "$percent%" else "No disponible"
    }

    private fun requestWeatherPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) loadWeather()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @Suppress("MissingPermission")
    private fun loadWeather() {
        executor.execute {
            try {
                val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = manager.getProviders(true)
                var location: Location? = null
                for (provider in providers) {
                    val candidate = try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
                    if (candidate != null && (location == null || candidate.time > location!!.time)) location = candidate
                }
                val current = location
                if (current == null) {
                    runOnUiThread { weatherText = "GPS no entregó una ubicación reciente. Activa ubicación e inténtalo otra vez." }
                    return@execute
                }
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${current.latitude}&longitude=${current.longitude}&current=temperature_2m,precipitation&timezone=auto")
                val connection = url.openConnection()
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                val json = connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
                val currentWeather = json.getJSONObject("current")
                val temp = currentWeather.getDouble("temperature_2m")
                val rain = currentWeather.getDouble("precipitation")
                runOnUiThread {
                    weatherText = "Ahora: ${temp}°C · precipitación: ${rain} mm. Proveedor: Open-Meteo."
                    speak("Ahora hay ${temp} grados y precipitación de ${rain} milímetros.")
                }
            } catch (_: Exception) {
                runOnUiThread { weatherText = "No pude consultar el clima. Comprueba la conexión e inténtalo de nuevo." }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
