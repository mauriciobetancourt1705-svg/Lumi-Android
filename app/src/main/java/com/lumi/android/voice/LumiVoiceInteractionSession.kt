package com.lumi.android.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.widget.TextView
import java.util.Locale

class LumiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var speaking = false
    private var silenceMode = false
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private var voiceManager: LumiVoiceManager? = null

    private val venezuelanLocale = Locale("es", "VE")

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            status.text = if (silenceMode) "Lumi está en silencio" else "Lumi está escuchando…"
        }

        override fun onBeginningOfSpeech() {
            if (speaking) stopSpeaking()
            status.text = "Te escucho…"
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onPartialResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                transcript.text = text
                if (speaking) stopSpeaking()
            }
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()

            if (text.isNotEmpty()) {
                transcript.text = text
                handleUserTurn(text)
            } else {
                scheduleListen()
            }
        }

        override fun onEndOfSpeech() {
            listening = false
            status.text = "Procesando…"
        }

        override fun onError(error: Int) {
            listening = false
            if (!silenceMode) {
                status.text = "Lumi sigue disponible"
                scheduleListen(600L)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun onCreateContentView(): View {
        val root = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
        }

        status = TextView(context).apply {
            text = "Lumi está lista"
            textSize = 22f
        }
        transcript = TextView(context).apply {
            text = "Háblame cuando quieras."
            textSize = 18f
            setPadding(0, 28, 0, 28)
        }

        root.addView(status)
        root.addView(transcript)

        prepareRecognizer()
        voiceManager = LumiVoiceManager(context)
        handler.postDelayed({ startListening() }, 300L)
        return root
    }

    private fun prepareRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status.text = "El reconocimiento de voz no está disponible"
            return
        }

        recognizer = try {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (_: UnsupportedOperationException) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer?.setRecognitionListener(listener)
    }

    private fun startListening() {
        if (silenceMode || listening || recognizer == null || speaking) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, venezuelanLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, venezuelanLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1900L)
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: RuntimeException) {
            scheduleListen(900L)
        }
    }

    private fun scheduleListen(delay: Long = 450L) {
        if (!silenceMode) handler.postDelayed({ startListening() }, delay)
    }

    private fun handleUserTurn(rawText: String) {
        val normalized = rawText.trim().lowercase(venezuelanLocale)

        if (normalized.contains("lumi") || normalized.startsWith("oye") || normalized.startsWith("hola")) {
            when {
                normalized.contains("espera") || normalized.contains("cállate") ||
                    normalized.contains("callate") -> {
                    silenceMode = true
                    stopSpeaking()
                    try { recognizer?.cancel() } catch (_: RuntimeException) { }
                    status.text = "Lumi está en silencio"
                    transcript.text = "Cuando quieras continuar, di «Lumi, sigue»."
                }
                normalized.contains("sigue") || normalized.contains("continúa") ||
                    normalized.contains("continua") -> {
                    silenceMode = false
                    respond("Claro, aquí estoy. Te escucho.")
                }
                normalized.contains("cómo estás") || normalized.contains("como estas") -> {
                    respond("Estoy aquí contigo. ¿Qué necesitas?")
                }
                normalized.contains("gracias") -> respond("Siempre.")
                else -> respond("Te escucho. Cuéntame.")
            }
        } else if (!silenceMode) {
            status.text = "Te sigo escuchando…"
            scheduleListen(250L)
        }
    }

    private fun respond(text: String) {
        silenceMode = false
        speaking = true
        listening = false
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        status.text = "Lumi está hablando…"
        transcript.text = text
        voiceManager?.speak(text)
        handler.postDelayed({
            if (speaking) {
                speaking = false
                status.text = "Lumi está escuchando…"
                startListening()
            }
        }, estimateSpeechDuration(text))
    }

    private fun stopSpeaking() {
        if (!speaking) return
        voiceManager?.stop()
        speaking = false
        status.text = "Te escucho…"
        startListening()
    }

    private fun estimateSpeechDuration(text: String): Long =
        (900L + text.length * 48L).coerceIn(1400L, 7000L)

    override fun onHide() {
        super.onHide()
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        voiceManager?.stop()
        handler.removeCallbacksAndMessages(null)
        listening = false
        speaking = false
    }

    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: RuntimeException) { }
        recognizer = null
        voiceManager?.shutdown()
        voiceManager = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
