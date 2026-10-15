package com.basewarriors.androidguardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReminderPolicyTest {
    @Test fun schedulesOneHourFromNow() {
        assertEquals(3_600_000L, ReminderPolicy.triggerAtMillis(0L, 60L))
    }

    @Test fun rejectsZeroDelay() {
        try {
            ReminderPolicy.triggerAtMillis(0L, 0L)
            throw AssertionError("Expected invalid delay to be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun batteryThresholdsAreOneShotUntilRearmed() {
        assertEquals(30, ReminderPolicy.batteryAlertLevel(29, emptySet()))
        assertEquals(20, ReminderPolicy.batteryAlertLevel(19, setOf(30)))
        assertNull(ReminderPolicy.batteryAlertLevel(19, setOf(20, 30)))
    }

    @Test fun doesNotAlertAboveThreshold() {
        assertNull(ReminderPolicy.batteryAlertLevel(31, emptySet()))
    }
}
