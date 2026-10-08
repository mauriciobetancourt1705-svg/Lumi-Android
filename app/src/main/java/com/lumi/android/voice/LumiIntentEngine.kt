package com.lumi.android.voice

import java.text.Normalizer
import java.util.Locale

sealed class LumiIntent {
    data class OpenApp(val query: String) : LumiIntent()
    data class WebSearch(val query: String) : LumiIntent()
    data class PlayContent(val query: String) : LumiIntent()
    data object OpenSettings : LumiIntent()
    data object VolumeUp : LumiIntent()
    data object VolumeDown : LumiIntent()
    data object PlayPause : LumiIntent()
    data object GoBack : LumiIntent()
    data object GoHome : LumiIntent()
    data object OpenRecents : LumiIntent()
    data object ScrollForward : LumiIntent()
    data object ScrollBackward : LumiIntent()
    data object DescribeScreen : LumiIntent()
    data class ClickText(val text: String) : LumiIntent()
    data class TypeText(val text: String) : LumiIntent()
    data object Silence : LumiIntent()
    data object Resume : LumiIntent()
    data object Greeting : LumiIntent()
    data object Thanks : LumiIntent()
    data object Status : LumiIntent()
    data object AutonomyStatus : LumiIntent()
    data object Privacy : LumiIntent()
    data class Conversation(val text: String) : LumiIntent()
}

class LumiIntentEngine {
    private val locale = Locale("es", "VE")

    fun parse(text: String): LumiIntent {
        val normalized = normalize(text)

        if (containsAny(normalized, "callate", "cállate", "silencio", "espera")) return LumiIntent.Silence
        if (containsAny(normalized, "sigue", "continua", "continúa", "despierta")) return LumiIntent.Resume
        if (containsAny(normalized, "gracias", "muchas gracias")) return LumiIntent.Thanks
        if (containsAny(normalized, "como estas", "cómo estas", "cómo estás", "como te sientes", "estas bien", "estás bien")) return LumiIntent.Status
        if (containsAny(normalized, "privacidad", "que permisos tienes", "qué permisos tienes", "que puedes ver", "qué puedes ver")) return LumiIntent.Privacy
        if (containsAny(normalized, "estas activa", "estás activa", "modo autonomia", "modo autonomía")) return LumiIntent.AutonomyStatus

        extractAfter(normalized, listOf("quiero abrir la app ", "quiero abrir el app ", "abre la app ", "abre el app ", "quiero abrir ", "abre ", "abrir ", "quiero ir a ", "ir a ", "llévame a ", "llevame a ", "ve a ", "ve al "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.OpenApp(it) }

        extractAfter(normalized, listOf("ponme ", "pon ", "reproduce ", "reproduceme ", "reprodúceme ", "quiero escuchar ", "quiero oír ", "quiero oir ", "escucha ", "pon a sonar "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.PlayContent(it) }

        extractAfter(normalized, listOf("busca en internet ", "busca en google ", "google ", "busca "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.WebSearch(it) }

        if (containsAny(normalized, "ajustes", "configuracion", "configuración", "configuracion del telefono")) return LumiIntent.OpenSettings
        if (containsAny(normalized, "sube el volumen", "sube volumen", "aumenta el volumen")) return LumiIntent.VolumeUp
        if (containsAny(normalized, "baja el volumen", "baja volumen", "disminuye el volumen")) return LumiIntent.VolumeDown
        if (containsAny(normalized, "pausa la musica", "pausa la música", "reproduce la musica", "reproduce la música", "pon musica", "pon música", "play", "pausa")) return LumiIntent.PlayPause

        if (containsAny(normalized, "vuelve atras", "vuelve atrás", "regresa", "retrocede")) return LumiIntent.GoBack
        if (containsAny(normalized, "ve al inicio", "ir al inicio", "pulsa inicio", "inicio")) return LumiIntent.GoHome
        if (containsAny(normalized, "abre recientes", "muestra recientes", "aplicaciones recientes", "apps recientes")) return LumiIntent.OpenRecents
        if (containsAny(normalized, "desplazate hacia abajo", "desplázate hacia abajo", "baja la pantalla", "haz scroll hacia abajo")) return LumiIntent.ScrollForward
        if (containsAny(normalized, "desplazate hacia arriba", "desplázate hacia arriba", "sube la pantalla", "haz scroll hacia arriba")) return LumiIntent.ScrollBackward
        if (containsAny(normalized, "que hay en pantalla", "qué hay en pantalla", "lee la pantalla", "describe la pantalla", "que aparece en pantalla", "qué aparece en pantalla")) return LumiIntent.DescribeScreen

        extractAfter(normalized, listOf("pulsa ", "presiona ", "toca ", "haz clic en "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.ClickText(it) }

        extractAfter(normalized, listOf("escribe ", "escribe esto ", "introduce ", "introduce esto "))
            ?.takeIf { it.isNotBlank() }
            ?.let { return LumiIntent.TypeText(it) }

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
