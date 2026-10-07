package com.lumi.android.voice

data class LumiEmailRequest(val recipient: String, val subject: String, val body: String)

class LumiEmailEngine {
    fun parse(text: String): LumiEmailRequest? {
        val raw = text.trim()
        val lower = raw.lowercase()
        if (!(lower.contains("correo") || lower.contains("email") || lower.contains("e-mail"))) return null
        val address = Regex("""[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE).find(raw)?.value ?: return null
        val subject = Regex("""(?i)(?:asunto|tema)\s*[:\-]\s*(.+?)(?:\s+(?:cuerpo|mensaje)\s*[:\-]|$)""").find(raw)?.groupValues?.get(1)?.trim() ?: "Mensaje de Lumi"
        val body = Regex("""(?i)(?:cuerpo|mensaje)\s*[:\-]\s*(.+)$""").find(raw)?.groupValues?.get(1)?.trim()
            ?: raw.substringAfter(address).trim().removePrefix(":").trim()
        if (body.isBlank()) return null
        return LumiEmailRequest(address, subject, body)
    }
}
