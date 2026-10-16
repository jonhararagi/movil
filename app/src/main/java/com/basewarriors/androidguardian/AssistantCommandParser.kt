package com.basewarriors.androidguardian

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, on-device command routing.
 *
 * This parser must never perform device actions itself. It only classifies the
 * user's words; MainActivity (and later an explicit action policy) owns effects.
 * Unknown phrases intentionally remain UNKNOWN instead of being guessed by an AI.
 */
internal enum class AssistantIntent {
    CHECK_BATTERY,
    CHECK_WEATHER,
    WATER,
    STUDY,
    BREAK,
    GO_OUT,
    PREPARE_REMINDER,
    HELP,
    UNKNOWN
}

internal data class AssistantDecision(
    val intent: AssistantIntent,
    val reminderText: String = ""
)

internal object AssistantCommandParser {
    private val reminderPrefix = Regex(
        "(?i)^\\s*(?:(?:por favor)\\s+)?(?:(?:pon(?:me)?|crea(?:me)?|añade|agrega|programa)\\s+)?" +
            "(?:un\\s+)?(?:recordatorio|recuérdame|recuerdame)(?:\\s+(?:de|para|que))?\\s*"
    )

    fun parse(raw: String): AssistantDecision {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return AssistantDecision(AssistantIntent.UNKNOWN)

        val prefixMatch = reminderPrefix.find(trimmed)
        if (prefixMatch != null) {
            return AssistantDecision(
                AssistantIntent.PREPARE_REMINDER,
                trimmed.removeRange(prefixMatch.range).trim()
            )
        }

        val normalized = normalize(trimmed)
        return when {
            normalized.contains("ayuda") ||
                normalized == "que puedes hacer" ||
                normalized == "que haces" ->
                AssistantDecision(AssistantIntent.HELP)

            normalized.contains("bateria") ->
                AssistantDecision(AssistantIntent.CHECK_BATTERY)

            listOf("clima", "lluvia", "paraguas", "temperatura").any(normalized::contains) ->
                AssistantDecision(AssistantIntent.CHECK_WEATHER)

            listOf("agua", "hidratar", "beber").any(normalized::contains) ->
                AssistantDecision(AssistantIntent.WATER)

            listOf("estudiar", "estudio", "estudi").any(normalized::contains) ->
                AssistantDecision(AssistantIntent.STUDY)

            listOf("pausa", "descansar", "descanso").any(normalized::contains) ->
                AssistantDecision(AssistantIntent.BREAK)

            listOf("salir", "caminar", "pasear").any(normalized::contains) ->
                AssistantDecision(AssistantIntent.GO_OUT)

            else -> AssistantDecision(AssistantIntent.UNKNOWN)
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
