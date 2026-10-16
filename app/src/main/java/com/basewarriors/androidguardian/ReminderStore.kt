package com.basewarriors.androidguardian

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class GuardianReminder(
    val id: Int,
    val body: String,
    val triggerAtMillis: Long
)

object ReminderStore {
    private const val PREFS = "guardian_reminders"
    private const val KEY_ITEMS = "items"

    fun load(context: Context): List<GuardianReminder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optInt("id", -1)
                val body = item.optString("body", "").trim()
                val trigger = item.optLong("triggerAtMillis", 0L)
                if (id < 0 || body.isEmpty() || trigger <= 0L) null
                else GuardianReminder(id, body, trigger)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, reminder: GuardianReminder) {
        val current = load(context).filterNot { it.id == reminder.id }
        val json = JSONArray()
        (current + reminder).forEach {
            json.put(JSONObject().put("id", it.id).put("body", it.body)
                .put("triggerAtMillis", it.triggerAtMillis))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, json.toString()).apply()
    }

    fun remove(context: Context, id: Int) {
        val json = JSONArray()
        load(context).filterNot { it.id == id }.forEach {
            json.put(JSONObject().put("id", it.id).put("body", it.body)
                .put("triggerAtMillis", it.triggerAtMillis))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, json.toString()).apply()
    }

    fun cancel(context: Context, id: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
        pending.cancel()
        remove(context, id)
    }

    fun restorePending(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        load(context).forEach { reminder ->
            if (reminder.triggerAtMillis <= now) {
                remove(context, reminder.id)
            } else {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(ReminderReceiver.EXTRA_TITLE, "Android Guardian 💜")
                    putExtra(ReminderReceiver.EXTRA_BODY, reminder.body)
                    putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
                }
                val pending = PendingIntent.getBroadcast(
                    context, reminder.id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending
                )
            }
        }
    }
}
