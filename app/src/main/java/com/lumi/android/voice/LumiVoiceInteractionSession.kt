package com.lumi.android.voice

import android.content.Context
import android.service.voice.VoiceInteractionSession
import android.widget.TextView

class LumiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onCreateContentView(): android.view.View {
        return TextView(context).apply {
            text = "Lumi está activa. Bloque 1: sesión de voz nativa preparada."
            textSize = 20f
            setPadding(40, 60, 40, 60)
        }
    }
}
