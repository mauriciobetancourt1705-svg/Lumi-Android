package com.lumi.android.voice

import android.content.Context
import android.content.Intent
import android.net.Uri

class LumiMessageActionExecutor(
    private val context: Context,
    private val contacts: LumiContactResolver
) {
    fun prepare(request: LumiMessageRequest): Pair<LumiContact?, String> {
        val contact = contacts.findBestMatch(request.recipient)
        if (contact == null) {
            return null to "No pude encontrar a " + request.recipient + ". Revisa el permiso de contactos."
        }
        val channelName = if (request.channel == LumiMessageRequest.Channel.WHATSAPP) "WhatsApp" else "mensajes"
        return contact to "Encontré a " + contact.name + ". El mensaje dice: «" + request.body + "». ¿Quieres abrir " + channelName + " para enviarlo?"
    }

    fun send(request: LumiMessageRequest, contact: LumiContact): String {
        return try {
            when (request.channel) {
                LumiMessageRequest.Channel.SMS -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:" + Uri.encode(contact.phone))
                        putExtra("sms_body", request.body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Abrí mensajes para " + contact.name + ". Revisa y pulsa enviar."
                }
                LumiMessageRequest.Channel.WHATSAPP -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, request.body)
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Abrí WhatsApp con el mensaje preparado. Selecciona a " + contact.name + " y pulsa enviar."
                }
            }
        } catch (_: Exception) {
            "No pude abrir el canal de mensajería."
        }
    }
}
