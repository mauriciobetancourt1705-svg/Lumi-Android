package com.lumi.android.voice

import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class LumiVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: android.os.Bundle?): VoiceInteractionSession {
        return LumiVoiceInteractionSession(this)
    }
}
