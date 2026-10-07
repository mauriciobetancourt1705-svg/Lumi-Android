package com.lumi.android.voice

import android.content.Context
import android.content.Intent
import android.net.Uri

class LumiEmailActionExecutor(private val context: Context) {
    fun prepare(request: LumiEmailRequest): String =
        "Voy a preparar un correo para \${request.recipient}, con asunto «\${request.subject}». ¿Quieres que lo haga?"

    fun send(request: LumiEmailRequest): String {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:\${Uri.encode(request.recipient)}")
            putExtra(Intent.EXTRA_SUBJECT, request.subject)
            putExtra(Intent.EXTRA_TEXT, request.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Abrí tu aplicación de correo con el destinatario, asunto y mensaje preparados. Revisa y pulsa enviar."
        } catch (_: RuntimeException) {
            "No encontré una aplicación de correo disponible en este teléfono."
        }
    }
}
