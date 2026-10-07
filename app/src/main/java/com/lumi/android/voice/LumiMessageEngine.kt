package com.lumi.android.voice

data class LumiMessageRequest(
    val channel: Channel,
    val recipient: String,
    val body: String
) { enum class Channel { WHATSAPP, SMS } }

class LumiMessageEngine {
    fun parse(text: String): LumiMessageRequest? {
        val normalized = text.trim().replace(Regex("\\s+"), " ")
        val lower = normalized.lowercase()

        val channel = when {
            "whatsapp" in lower -> LumiMessageRequest.Channel.WHATSAPP
            Regex("\\b(sms|mensaje|mensaje de texto)\\b").containsMatchIn(lower) -> LumiMessageRequest.Channel.SMS
            else -> null
        } ?: return null

        val recipient =
            Regex("""(?:a|para)\\s+(.+?)\\s+(?:por|v[ií]a)\\s+(?:whatsapp|sms|mensaje)""", RegexOption.IGNORE_CASE)
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("""(?:redacta|escribe|manda|env[ií]a)\\s+(?:un\\s+)?(?:mensaje|texto)\\s+(?:a|para)\\s+(.+?)(?:\\s+(?:por|v[ií]a)\\s+(?:whatsapp|sms|mensaje))?\\s*[:,-]""", RegexOption.IGNORE_CASE)
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?: return null

        val body =
            Regex(""":\\s*(.+)$""").find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("""(?:dile|dec[ií]le|diciendo|que)\\s+(.+)$""", RegexOption.IGNORE_CASE)
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?: return null

        if (recipient.isBlank() || body.isBlank()) return null
        return LumiMessageRequest(channel, recipient, body)
    }

    fun looksLikeMessageRequest(text: String): Boolean {
        val lower = text.lowercase()
        return ("whatsapp" in lower || Regex("\\b(sms|mensaje)\\b").containsMatchIn(lower)) &&
            Regex("\\b(a|para)\\s+").containsMatchIn(lower)
    }
}
