package com.basewarriors.androidguardian

import java.util.concurrent.TimeUnit

object ReminderPolicy {
    const val DEFAULT_DELAY_MINUTES = 60L
    const val MIN_DELAY_MINUTES = 1L
    const val MAX_DELAY_MINUTES = 24L * 60L

    fun triggerAtMillis(nowMillis: Long, delayMinutes: Long): Long {
        require(delayMinutes in MIN_DELAY_MINUTES..MAX_DELAY_MINUTES) {
            "El recordatorio debe programarse entre 1 minuto y 24 horas."
        }
        return nowMillis + TimeUnit.MINUTES.toMillis(delayMinutes)
    }

    fun batteryAlertLevel(percent: Int, alreadyAlertedAt: Set<Int>): Int? {
        if (percent !in 0..100) return null
        return listOf(20, 30).firstOrNull { percent <= it && it !in alreadyAlertedAt }
    }
}
