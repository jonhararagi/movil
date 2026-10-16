package com.basewarriors.androidguardian

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, on-device command routing.
 *
 * This parser never performs device actions. It classifies unambiguous requests;
 * MainActivity (and later an explicit action policy) owns all side effects.
 * Unknown, negated and multi-intent phrases intentionally remain UNKNOWN.
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

    private val rules = listOf(
        AssistantIntent.CHECK_BATTERY to listOf("bateria"),
        AssistantIntent.CHECK_WEATHER to listOf("clima", "lluvia", "llover", "llueve", "lloviendo", "paraguas", "temperatura"),
        AssistantIntent.WATER to listOf("agua", "hidratar", "beber"),
        AssistantIntent.STUDY to listOf("estudiar", "estudio", "estudi"),
        AssistantIntent.BREAK to listOf("pausa", "descansar", "descanso"),
        AssistantIntent.GO_OUT to listOf("salir", "caminar", "pasear")
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
        if (isNegatedOrCancellation(normalized)) {
            return AssistantDecision(AssistantIntent.UNKNOWN)
        }

        if (normalized.contains("ayuda") ||
            normalized.contains("que puedes hacer") ||
            normalized.contains("que haces")
        ) {
            return AssistantDecision(AssistantIntent.HELP)
        }

        val matchedRules = rules.filter { (_, phrases) -> phrases.any(normalized::contains) }
        return if (matchedRules.size == 1) {
            AssistantDecision(matchedRules.single().first)
        } else {
            // More than one possible action is ambiguous. Never guess which to run.
            AssistantDecision(AssistantIntent.UNKNOWN)
        }
    }

    private fun isNegatedOrCancellation(value: String): Boolean =
        value.startsWith("no ") ||
            value.startsWith("no me ") ||
            value.startsWith("no hagas ") ||
            value.startsWith("deja de ") ||
            value.startsWith("cancela ") ||
            value.startsWith("para ") ||
            value.contains(" no quiero ") ||
            value.contains(" no necesito ")

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
