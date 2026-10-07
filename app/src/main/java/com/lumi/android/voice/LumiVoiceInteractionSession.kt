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
    private lateinit var status: TextView
    private lateinit var transcript: TextView

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            status.text = "Lumi está escuchando…"
        }

        override fun onBeginningOfSpeech() {
            status.text = "Te escucho…"
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onPartialResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) transcript.text = text
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
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
            status.text = "Lumi sigue disponible"
            scheduleListen(600L)
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
        if (listening || recognizer == null || speaking) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
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
        handler.postDelayed({ startListening() }, delay)
    }

    private fun handleUserTurn(rawText: String) {
        val text = rawText.trim()
        val normalized = text.lowercase(Locale("es", "ES"))

        if (normalized.contains("lumi") || normalized.startsWith("oye") || normalized.startsWith("hola")) {
            val answer = when {
                normalized.contains("espera") || normalized.contains("cállate") -> "Claro. Me quedo en silencio."
                normalized.contains("cómo estás") -> "Estoy aquí contigo. ¿Qué necesitas?"
                normalized.contains("gracias") -> "Siempre."
                else -> "Te escucho. Cuéntame."
            }
            respond(answer)
        } else {
            // During an active Lumi session we keep listening without interrupting.
            status.text = "Te sigo escuchando…"
            scheduleListen(250L)
        }
    }

    private fun respond(text: String) {
        speaking = true
        listening = false
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        status.text = "Lumi está hablando…"
        transcript.text = text

        val voice = LumiVoiceManager(context)
        voice.speak(text)
        handler.postDelayed({
            voice.shutdown()
            speaking = false
            status.text = "Lumi está escuchando…"
            startListening()
        }, estimateSpeechDuration(text))
    }

    private fun estimateSpeechDuration(text: String): Long =
        (900L + text.length * 48L).coerceIn(1400L, 7000L)

    override fun onHide() {
        super.onHide()
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        handler.removeCallbacksAndMessages(null)
        listening = false
    }

    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: RuntimeException) { }
        recognizer = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
