package com.basewarriors.androidguardian

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.lazy.items
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
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val reminders = mutableStateListOf<String>()
    private var reminderText by mutableStateOf("")
    private var statusText by mutableStateOf("Estoy aquí para ayudarte a usar menos el móvil, no más. 💜")
    private var batteryText by mutableStateOf("Batería: consultando…")
    private var weatherText by mutableStateOf("El clima se consulta solo cuando tú lo pides.")
    private val executor = Executors.newSingleThreadExecutor()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadWeather()
        } else {
            weatherText = "Ubicación denegada. Puedes activarla en Ajustes; no se guarda tu posición."
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusText = if (granted) "Notificaciones habilitadas." else
            "Notificaciones desactivadas. Android puede impedir los avisos."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateBattery()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Android Guardian", style = MaterialTheme.typography.headlineMedium)
                            Text("Tu compañera de bienestar · MVP 0.1")
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("💜  Hola, senpai. ¿Qué pequeño paso te ayudaría hoy?")
                                    Spacer(Modifier.height(8.dp))
                                    Text("Modo equilibrado: recordatorios discretos, sin insistir si no respondes.")
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🔋 Batería")
                                    Text(batteryText)
                                    Text("Avisos previstos al bajar de 30% y 20%. La monitorización de fondo se ampliará en el siguiente incremento.")
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("⏰ Recordatorio local")
                                    OutlinedTextField(
                                        value = reminderText,
                                        onValueChange = { reminderText = it },
                                        label = { Text("¿Qué quieres recordar?") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { addReminder(60) }, enabled = reminderText.isNotBlank()) {
                                            Text("En 1 hora")
                                        }
                                        Button(onClick = { addReminder(5) }, enabled = reminderText.isNotBlank()) {
                                            Text("Prueba en 5 min")
                                        }
                                    }
                                    Text("Se guarda localmente en la alarma del sistema; no se envía a servidores.")
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🌦️ Clima por GPS")
                                    Text(weatherText)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { requestWeatherPermission() }) { Text("Consultar clima con permiso") }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🧠 Privacidad y estado")
                                    Text(statusText)
                                    Text("No es una herramienta médica. No diagnostica ni cambia tratamientos.")
                                }
                            }
                        }
                        if (reminders.isNotEmpty()) {
                            item { Text("Recordatorios creados", style = MaterialTheme.typography.titleMedium) }
                            items(reminders) { Text("• $it") }
                        }
                    }
                }
            }
        }
    }

    private fun addReminder(delayMinutes: Long) {
        val title = reminderText.trim()
        if (title.isEmpty()) return
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, "Android Guardian")
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
        statusText = "Recordatorio programado. Android puede retrasarlo por ahorro de batería."
    }

    private fun updateBattery() {
        val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra("level", -1) ?: -1
        val scale = battery?.getIntExtra("scale", 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        batteryText = if (percent >= 0) "$percent%" else "No disponible"
        if (percent in 0..30) statusText = "La batería está al 30% o menos. ¡Conéctala cuando te venga bien!"
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
                runOnUiThread { weatherText = "Ahora: ${temp}°C · precipitación: ${rain} mm. Proveedor: Open-Meteo." }
            } catch (e: Exception) {
                runOnUiThread { weatherText = "No pude consultar el clima: comprueba la conexión e inténtalo de nuevo." }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
