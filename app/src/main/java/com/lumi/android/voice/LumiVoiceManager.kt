package com.lumi.android.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class LumiVoiceManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var initialized = false
    private val utteranceCounter = AtomicInteger(0)
    private var pending: PendingSpeech? = null

    private data class PendingSpeech(
        val text: String,
        val onStart: () -> Unit,
        val onDone: () -> Unit,
        val onError: () -> Unit
    )

    var selectedVoiceName: String?
        get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_VOICE, null)
        set(value) { appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_VOICE, value).apply() }

    var pitch: Float
        get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_PITCH, 1.0f)
        set(value) { appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_PITCH, value).apply() }

    var speechRate: Float
        get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_RATE, 1.0f)
        set(value) { appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_RATE, value).apply() }

    private val preferredLocale = Locale("es", "VE")

    init {
        tts = TextToSpeech(appContext, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                pendingCallbacks(utteranceId)?.first?.invoke()
            }

            override fun onDone(utteranceId: String) {
                pendingCallbacks(utteranceId)?.second?.invoke()
            }

            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String) {
                pendingCallbacks(utteranceId)?.third?.invoke()
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                pendingCallbacks(utteranceId)?.third?.invoke()
            }
        })
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            pending?.onError?.invoke()
            pending = null
            return
        }
        initialized = true
        tts?.setLanguage(preferredLocale)
        applySettings()
        val request = pending
        pending = null
        request?.let { speakInternal(it) }
    }

    fun availableSpanishVoices(): List<Voice> =
        tts?.voices
            ?.filter { it.locale.language == "es" }
            ?.sortedWith(
                compareByDescending<Voice> { it.locale.country.equals("VE", ignoreCase = true) }
                    .thenBy { voiceLabel(it) }
            )
            .orEmpty()

    fun voiceLabel(voice: Voice): String {
        val country = voice.locale.displayCountry.takeIf { it.isNotBlank() }
        return if (country != null) voice.locale.displayLanguage + " · " + country else voice.locale.displayLanguage
    }

    fun selectVoice(voice: Voice) {
        selectedVoiceName = voice.name
        if (initialized) tts?.voice = voice
    }

    fun applySettings() {
        if (!initialized) return
        val selected = availableSpanishVoices().firstOrNull { it.name == selectedVoiceName }
        if (selected != null) {
            tts?.voice = selected
        } else {
            val venezuelan = availableSpanishVoices().firstOrNull {
                it.locale.country.equals("VE", ignoreCase = true)
            }
            if (venezuelan != null) tts?.voice = venezuelan else tts?.language = preferredLocale
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speechRate)
    }

    fun speak(text: String) {
        speak(text, {}, {}, {})
    }

    fun speak(text: String, onStart: () -> Unit, onDone: () -> Unit, onError: () -> Unit) {
        val request = PendingSpeech(text, onStart, onDone, onError)
        if (!initialized) {
            pending?.onError?.invoke()
            pending = request
            return
        }
        speakInternal(request)
    }

    private fun speakInternal(request: PendingSpeech) {
        applySettings()
        val utteranceId = "lumi-" + utteranceCounter.incrementAndGet()
        val result = tts?.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            request.onError()
        }
    }

    private fun pendingCallbacks(utteranceId: String): Triple<() -> Unit, () -> Unit, () -> Unit>? {
        return null
    }

    fun stop() {
        pending = null
        tts?.stop()
    }

    fun shutdown() {
        pending = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }

    companion object {
        private const val PREFS = "lumi_voice_preferences"
        private const val KEY_VOICE = "voice_name"
        private const val KEY_PITCH = "pitch"
        private const val KEY_RATE = "speech_rate"
    }
}
