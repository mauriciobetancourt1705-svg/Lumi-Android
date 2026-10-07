package com.lumi.android.wellbeing

import java.util.Locale

class LumiPersonalityEngine {
    enum class Moment { MORNING, MIDDAY, EVENING, CHECK_IN }

    fun greeting(name: String? = null, moment: Moment = Moment.CHECK_IN): String {
        val who = name?.trim()?.takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
        return when (moment) {
            Moment.MORNING -> "Buenos días$who. Soy Lumi. ¿Cómo amaneciste? Cuéntame cómo estás y qué te gustaría sacar adelante hoy."
            Moment.MIDDAY -> "Hola$who. Paso por aquí para saber cómo vas. ¿Cómo te está tratando el día? Si tienes algo pendiente, puedo ayudarte a organizarlo."
            Moment.EVENING -> "Ya va terminando el día$who. ¿Cómo te fue? Si quieres, hacemos un pequeño cierre y dejamos mañana más claro."
            Moment.CHECK_IN -> "Aquí estoy$who. Quería saber cómo estás. ¿Cómo va tu día y hay algo en lo que pueda ayudarte?"
        }
    }

    fun reply(text: String): String {
        val t = text.lowercase(Locale("es", "VE")).trim()
        return when {
            t.contains("mal") || t.contains("cansad") || t.contains("estres") || t.contains("agobiad") ->
                "Gracias por contármelo. No tienes que resolverlo todo de una vez. Si quieres, dime qué es lo que más te está pesando y lo vemos juntos."
            t.contains("bien") || t.contains("excelente") || t.contains("feliz") ->
                "Me alegra escucharlo. ¿Qué ha sido lo mejor de tu día hasta ahora?"
            t.contains("ayuda") || t.contains("ayudame") || t.contains("ayúdame") ->
                "Claro. Dime qué necesitas y buscamos la manera más sencilla de hacerlo."
            t.contains("estudi") || t.contains("tarea") || t.contains("examen") ->
                "Vamos con eso. Podemos dividirlo en pasos pequeños para que no se sienta tan pesado."
            t.contains("gracias") ->
                "Siempre que me necesites, aquí estoy."
            else ->
                "Te escucho. Cuéntame un poco más; quiero entender qué necesitas antes de proponerte algo."
        }
    }

    fun shouldCheckIn(text: String): Boolean {
        val t = text.lowercase(Locale("es", "VE"))
        return t.contains("cómo estás") || t.contains("como estas") ||
            t.contains("cómo voy") || t.contains("como voy") ||
            t.contains("mi día") || t.contains("mi dia") ||
            t.contains("bienestar") || t.contains("hablar con lumi")
    }
}
