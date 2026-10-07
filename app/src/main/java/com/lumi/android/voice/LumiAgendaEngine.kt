package com.lumi.android.voice

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class LumiAgendaRequest(
    val type: Type,
    val title: String,
    val triggerAtMillis: Long,
    val durationMinutes: Int = 30
) {
    enum class Type { REMINDER, CALENDAR_EVENT }
}

class LumiAgendaEngine(private val nowMillis: () -> Long = { System.currentTimeMillis() }) {
    fun parse(text: String): LumiAgendaRequest? {
        val raw = text.trim()
        val n = raw.lowercase(Locale("es", "VE"))
        val type = when {
            n.contains("recordatorio") || n.contains("recuérdame") || n.contains("recuerdame") -> LumiAgendaRequest.Type.REMINDER
            n.contains("cita") || n.contains("calendario") || n.contains("evento") -> LumiAgendaRequest.Type.CALENDAR_EVENT
            else -> null
        } ?: return null

        val trigger = parseTime(raw) ?: return null
        val title = raw
            .replace(Regex("(?i)^.*?(recuérdame|recuerdame|recordatorio|cita|evento|calendario)\\s*"), "")
            .replace(Regex("(?i)\\s*(mañana|manana|hoy)\\s+(?:a\\s+las\\s+)?\\d{1,2}(?::\\d{2})?\\s*(am|pm)?\\s*$"), "")
            .trim(' ', '.', ':', '-', '—')
            .ifBlank { if (type == LumiAgendaRequest.Type.REMINDER) "Recordatorio de Lumi" else "Cita de Lumi" }

        return LumiAgendaRequest(type, title, trigger, 30)
    }

    private fun parseTime(text: String): Long? {
        val cal = Calendar.getInstance(Locale("es", "VE")).apply { timeInMillis = nowMillis() }
        val n = text.lowercase(Locale("es", "VE"))
        val tomorrow = n.contains("mañana") || n.contains("manana")
        if (tomorrow) cal.add(Calendar.DAY_OF_YEAR, 1)

        val m = Pattern.compile("""\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""").matcher(n)
        if (!m.find()) return null
        var hour = m.group(1)!!.toInt()
        val minute = m.group(2)?.toInt() ?: 0
        val meridiem = m.group(3)
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        if (meridiem == null && hour in 1..7) hour += 12
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0,23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0,59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (!tomorrow && cal.timeInMillis <= nowMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun describe(request: LumiAgendaRequest): String {
        val fmt = SimpleDateFormat("EEE d 'de' MMMM 'a las' HH:mm", Locale("es", "VE"))
        return if (request.type == LumiAgendaRequest.Type.REMINDER)
            "Puedo recordarte «\${request.title}» el \${fmt.format(request.triggerAtMillis)}."
        else
            "Puedo crear el evento «\${request.title}» el \${fmt.format(request.triggerAtMillis)}."
    }
}
