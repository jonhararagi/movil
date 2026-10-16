package com.basewarriors.androidguardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCommandParserTest {

    @Test
    fun recognizesLocalCommandsWithoutAccentSensitivity() {
        assertEquals(AssistantIntent.CHECK_BATTERY, AssistantCommandParser.parse("¿Cómo está la batería?").intent)
        assertEquals(AssistantIntent.CHECK_WEATHER, AssistantCommandParser.parse("¿Va a llover?").intent)
        assertEquals(AssistantIntent.WATER, AssistantCommandParser.parse("Quiero hidratarme").intent)
        assertEquals(AssistantIntent.STUDY, AssistantCommandParser.parse("Quiero estudiar").intent)
        assertEquals(AssistantIntent.BREAK, AssistantCommandParser.parse("Necesito descansar").intent)
        assertEquals(AssistantIntent.GO_OUT, AssistantCommandParser.parse("Quiero salir a caminar").intent)
    }

    @Test
    fun extractsReminderTextButDoesNotScheduleIt() {
        val decision = AssistantCommandParser.parse("Recuérdame comprar leche")
        assertEquals(AssistantIntent.PREPARE_REMINDER, decision.intent)
        assertEquals("comprar leche", decision.reminderText)
    }

    @Test
    fun reminderPhraseTakesPriorityOverKeywordRouting() {
        val decision = AssistantCommandParser.parse("Ponme un recordatorio para estudiar mañana")
        assertEquals(AssistantIntent.PREPARE_REMINDER, decision.intent)
        assertEquals("estudiar mañana", decision.reminderText)
    }

    @Test
    fun unknownOrEmptyInputNeverBecomesAnAction() {
        assertEquals(AssistantIntent.UNKNOWN, AssistantCommandParser.parse("").intent)
        assertEquals(AssistantIntent.UNKNOWN, AssistantCommandParser.parse("manda un mensaje").intent)
        assertTrue(AssistantCommandParser.parse("manda un mensaje").reminderText.isEmpty())
    }

    @Test
    fun helpIsRecognizedLocally() {
        assertEquals(AssistantIntent.HELP, AssistantCommandParser.parse("¿Qué puedes hacer?").intent)
    }
}
