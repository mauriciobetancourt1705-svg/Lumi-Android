package com.lumi.android.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.util.Locale

class LumiBackgroundVoiceService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private lateinit var voice: LumiVoiceManager
    private lateinit var actionExecutor: LumiAndroidActionExecutor
    private val intentEngine = LumiIntentEngine()
    private lateinit var memory: LumiMemoryStore
    private val personality = com.lumi.android.wellbeing.LumiPersonalityEngine()
    private var active = false
    private var speaking = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.lumi.android.R.drawable.lumi_logo)
            .setContentTitle("Lumi está activa")
            .setContentText("Escucha en segundo plano autorizada por ti")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        voice = LumiVoiceManager(this)
        actionExecutor = LumiAndroidActionExecutor(this)
        memory = LumiMemoryStore(this)
        prepareRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        active = true
        listen()
        return START_STICKY
    }

    private fun prepareRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(this)
        } catch (_: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else null
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onPartialResults(results: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (text.isBlank()) {
                    listen()
                    return
                }
                handle(text)
            }
            override fun onError(error: Int) {
                if (active && !speaking) listen()
            }
        })
    }

    private fun listen() {
        if (!active || speaking || recognizer == null) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2600L)
        }
        try {
            recognizer?.startListening(intent)
        } catch (_: RuntimeException) {
            if (active) android.os.Handler(mainLooper).postDelayed({ listen() }, 1000L)
        }
    }

    private fun handle(raw: String) {
        memory.addTurn("usuario", raw)
        when (val intent = intentEngine.parse(raw)) {
            LumiIntent.Silence -> {
                speak("Listo. Dejo de escuchar en segundo plano.")
                active = false
            }
            LumiIntent.Greeting -> speak("Hola. Aquí estoy contigo.")
            LumiIntent.Thanks -> speak("Siempre.")
            LumiIntent.Status -> speak(personality.greeting(moment = com.lumi.android.wellbeing.LumiPersonalityEngine.Moment.CHECK_IN))
            is LumiIntent.OpenApp,
            is LumiIntent.WebSearch,
            is LumiIntent.PlayContent,
            LumiIntent.OpenSettings,
            LumiIntent.VolumeUp,
            LumiIntent.VolumeDown,
            LumiIntent.PlayPause -> actionExecutor.execute(intent) { message, _ ->
                if (message.isBlank()) listen() else speak(message)
            }
            else -> speak(personality.reply(raw))
        }
    }

    private fun speak(text: String) {
        if (!active) return
        speaking = true
        try { recognizer?.cancel() } catch (_: RuntimeException) {}
        voice.onReady {
            voice.speak(
                text,
                onStart = {},
                onDone = {
                    speaking = false
                    if (active) listen()
                },
                onError = {
                    speaking = false
                    if (active) listen()
                }
            )
        }
    }

    override fun onDestroy() {
        active = false
        try { recognizer?.cancel(); recognizer?.destroy() } catch (_: RuntimeException) {}
        recognizer = null
        if (::voice.isInitialized) voice.shutdown()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lumi · escucha en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "lumi_background_voice"
        private const val NOTIFICATION_ID = 26001
    }
}
