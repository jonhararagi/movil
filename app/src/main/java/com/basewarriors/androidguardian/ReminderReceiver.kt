package com.basewarriors.androidguardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            ReminderStore.restorePending(context)
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Pausa amable"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Es hora de revisar tu recordatorio."
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        val id = intent.getIntExtra(EXTRA_ID, 1)
        manager.notify(id, notification)
        ReminderStore.remove(context, id)
    }

    companion object {
        const val CHANNEL_ID = "guardian_reminders"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_ID = "notification_id"
    }
}
