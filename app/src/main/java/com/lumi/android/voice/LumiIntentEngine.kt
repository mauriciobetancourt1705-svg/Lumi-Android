package com.lumi.android.voice

import java.text.Normalizer
import java.util.Locale

sealed class LumiIntent {
    data class OpenApp(val query: String) : LumiIntent()
    data class WebSearch(val query: String) : LumiIntent()
    data object OpenSettings : LumiIntent()
    data object VolumeUp : LumiIntent()
    data object VolumeDown : LumiIntent()
    data object PlayPause : LumiIntent()
    data object Silence : LumiIntent()
    data object Resume : LumiIntent()
    data object Greeting : LumiIntent()
    data object Thanks : LumiIntent()
    data object Status : LumiIntent()
    data class Conversation(val text: String) : LumiIntent()
}

class LumiIntentEngine {
    private val locale = Locale("es", "VE")

    fun parse(text: String): LumiIntent {
        val normalized = normalize(text)

        if (containsAny(normalized, "callate", "cállate", "silencio", "espera")) return LumiIntent.Silence
        if (containsAny(normalized, "sigue", "continua", "continúa", "despierta")) return LumiIntent.Resume
        if (containsAny(normalized, "gracias", "muchas gracias")) return LumiIntent.Thanks
        if (containsAny(normalized, "como estas", "cómo estas", "cómo estás", "como te sientes")) return LumiIntent.Status

        extractAfter(normalized, listOf("abre ", "abrir ", "abre la app ", "abre el app "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.OpenApp(it) }

        extractAfter(normalized, listOf("busca en internet ", "busca en google ", "google ", "busca "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.WebSearch(it) }

        if (containsAny(normalized, "ajustes", "configuracion", "configuración", "configuracion del telefono")) return LumiIntent.OpenSettings
        if (containsAny(normalized, "sube el volumen", "sube volumen", "aumenta el volumen")) return LumiIntent.VolumeUp
        if (containsAny(normalized, "baja el volumen", "baja volumen", "disminuye el volumen")) return LumiIntent.VolumeDown
        if (containsAny(normalized, "pausa la musica", "pausa la música", "reproduce la musica", "reproduce la música", "pon musica", "pon música", "play", "pausa")) return LumiIntent.PlayPause

        return LumiIntent.Conversation(text.trim())
    }

    private fun extractAfter(text: String, prefixes: List<String>): String? =
        prefixes.firstNotNullOfOrNull { prefix ->
            if (text.startsWith(prefix)) text.removePrefix(prefix).trim() else null
        }

    private fun containsAny(text: String, vararg values: String): Boolean = values.any { text.contains(it) }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(locale), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
}
