package com.lumi.android.voice

import android.service.voice.VoiceInteractionService

/**
 * Native entry point for Lumi when Android selects it as the system voice interactor.
 * Keep this service lightweight; conversational UI belongs to the session service.
 */
class LumiVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
    }
}
