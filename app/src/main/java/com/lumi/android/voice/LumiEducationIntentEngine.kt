package com.lumi.android.voice

import java.text.Normalizer
import java.util.Locale

sealed class LumiEducationIntent {
    data class Tutor(val text: String) : LumiEducationIntent()
    data class Oraculo(val text: String) : LumiEducationIntent()
    data object BcvRate : LumiEducationIntent()
}

class LumiEducationIntentEngine {
    private val locale = Locale("es", "VE")

    fun parse(text: String): LumiEducationIntent? {
        val n = normalize(text)
        listOf("preguntale al tutor ", "preguntale a education ", "dile al tutor ", "tutor ")
            .firstOrNull { n.startsWith(it) }
            ?.let { return LumiEducationIntent.Tutor(text.trim().substring(it.length).trim()) }
        listOf("preguntale a oraculo ", "oraculo ")
            .firstOrNull { n.startsWith(it) }
            ?.let { return LumiEducationIntent.Oraculo(text.trim().substring(it.length).trim()) }
        if (listOf("como voy en la universidad", "como voy", "que me falta en la universidad", "que me falta", "mis notas", "mi progreso").any { n.contains(it) })
            return LumiEducationIntent.Oraculo(text.trim())
        if (listOf("tasa del bcv", "tasa oficial del bcv", "dolar oficial").any { n.contains(it) })
            return LumiEducationIntent.BcvRate
        return null
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(locale), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
}
