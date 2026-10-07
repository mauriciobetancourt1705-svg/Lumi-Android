package com.lumi.android.voice

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.lumi.android.autonomy.AutonomyStatus
import com.lumi.android.autonomy.Privacy
import com.lumi.android.intent.LumiIntent
import com.lumi.android.personality.LumiPersonalityEngine

class LumiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private var silenceMode = false

    private fun handleIntent(intent: LumiIntent) {
        when (intent) {
            LumiIntent.Silence -> {
                silenceMode = true
                stopSpeaking()
                status.text = "Lumi está en silencio"
                transcript.text = "Cuando quieras continuar, di «Lumi, sigue»."
            }
            LumiIntent.Resume -> {
                silenceMode = false
                respond("Claro, aquí estoy. Te escucho.")
            }
            LumiIntent.Greeting -> respond("Hola. Aquí estoy contigo.")
            LumiIntent.Thanks -> respond("Siempre.")
            LumiIntent.Status -> respond(
                LumiPersonalityEngine(this).greeting(
                    moment = LumiPersonalityEngine.Moment.CHECK_IN
                )
            )
            is LumiIntent.OpenApp,
            is LumiIntent.WebSearch,
            LumiIntent.OpenSettings,
            LumiIntent.VolumeUp,
            LumiIntent.VolumeDown,
            LumiIntent.PlayPause -> executeAndroidAction(intent)
            is LumiIntent.Conversation -> respond(
                LumiPersonalityEngine(this).reply(intent.text)
            )
            is LumiIntent.AutonomyStatus -> respond(intent.message)
            is LumiIntent.Privacy -> respond(intent.message)
        }
    }

    private fun executeAndroidAction(intent: LumiIntent) {
        respond("Acción preparada.")
    }

    override fun onHandleAssist(
        data: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, content)
    }
}
