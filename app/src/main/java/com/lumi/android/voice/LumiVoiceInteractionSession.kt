package com.lumi.android.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.widget.TextView
import java.util.Locale
import com.lumi.android.wellbeing.LumiPersonalityEngine

class LumiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var speaking = false
    private var silenceMode = false
    private var listenGeneration = 0L
    private var listenScheduled = false
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private var voiceManager: LumiVoiceManager? = null
    private var neuralTtsPlayer: LumiNeuralTtsPlayer? = null
    private val intentEngine = LumiIntentEngine()
    private lateinit var actionExecutor: LumiAndroidActionExecutor
    private lateinit var messageExecutor: LumiMessageActionExecutor
    private lateinit var memoryStore: LumiMemoryStore
    private lateinit var taskPlanner: LumiTaskPlanner
    private lateinit var taskExecutor: LumiTaskExecutor
    private lateinit var agendaExecutor: LumiAgendaActionExecutor
    private lateinit var emailExecutor: LumiEmailActionExecutor
    private lateinit var agentEngine: LumiAgentEngine
    private lateinit var agentExecutor: LumiAgentExecutor
    private lateinit var educationBridge: LumiEducationBridge
    private val educationIntentEngine = LumiEducationIntentEngine()
    private val personality = LumiPersonalityEngine()
    private var pendingAgentTasks: MutableList<LumiTask> = mutableListOf()
    private var pendingAgentIndex = 0
    private lateinit var notificationStore: LumiNotificationStore
    private val contextEngine = LumiContextEngine()
    private val agendaEngine = LumiAgendaEngine()
    private val messageEngine = LumiMessageEngine()
    private val emailEngine = LumiEmailEngine()
    private var pendingMessage: LumiMessageRequest? = null
    private var pendingContact: LumiContact? = null
    private var pendingAgenda: LumiAgendaRequest? = null
    private var pendingEmail: LumiEmailRequest? = null
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
            if (!silenceMode && !speaking) {
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

        actionExecutor = LumiAndroidActionExecutor(context)
        val contactResolver = LumiContactResolver(context)
        messageExecutor = LumiMessageActionExecutor(context, contactResolver)
        memoryStore = LumiMemoryStore(context)
        taskPlanner = LumiTaskPlanner(messageEngine)
        taskExecutor = LumiTaskExecutor(messageExecutor, actionExecutor)
        agendaExecutor = LumiAgendaActionExecutor(context)
        emailExecutor = LumiEmailActionExecutor(context)
        notificationStore = LumiNotificationStore(context)
        agentEngine = LumiAgentEngine(taskPlanner)
        agentExecutor = LumiAgentExecutor(taskExecutor, messageExecutor, contactResolver)
        educationBridge = LumiEducationBridge(context)
        prepareRecognizer()
        voiceManager = LumiVoiceManager(context)
        neuralTtsPlayer = LumiNeuralTtsPlayer(context)
        handler.postDelayed({ startListening() }, 300L)
        return root
    }

    private fun prepareRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status.text = "El reconocimiento de voz no está disponible"
            return
        }
        recognizer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
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
        if (silenceMode || listening || recognizer == null || speaking || listenScheduled) return
        listenScheduled = false
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
        if (silenceMode || speaking || listening || recognizer == null) return
        val generation = ++listenGeneration
        listenScheduled = true
        handler.postDelayed({
            if (generation != listenGeneration) { listenScheduled = false; return@postDelayed }
            listenScheduled = false
            startListening()
        }, delay)
    }

    private fun handleUserTurn(rawText: String) {
        memoryStore.addTurn("usuario", rawText)

        val normalized = rawText.trim().lowercase()
        val educationIntent = educationIntentEngine.parse(rawText)
        if (educationIntent != null) {
            when (educationIntent) {
                is LumiEducationIntent.Tutor ->
                    educationBridge.tutor(educationIntent.text, "aprender", memoryStore.recentTurns()) { result ->
                        handler.post { respond(result) }
                    }
                is LumiEducationIntent.Oraculo ->
                    educationBridge.oraculo(educationIntent.text, memoryStore.recentTurns()) { result ->
                        handler.post { respond(result) }
                    }
                LumiEducationIntent.BcvRate ->
                    educationBridge.bcvRate { result -> handler.post { respond(result) } }
            }
            return
        }

        val agentPlan = agentEngine.plan(rawText)
        if (agentPlan != null && agentPlan.tasks.size > 1) {
            pendingAgentTasks = agentPlan.tasks.toMutableList()
            pendingAgentIndex = 0
            executeNextAgentTask()
            return
        }
        val contextIntent = contextEngine.parse(rawText)
        if (contextIntent != null) {
            when (contextIntent) {
                LumiContextIntent.RecentNotifications -> respond(notificationStore.summary())
                LumiContextIntent.ClearRecentNotifications -> {
                    notificationStore.clear()
                    respond("Listo. Borré de la memoria local las notificaciones que Lumi había guardado.")
                }
            }
            return
        }

        if (pendingEmail != null) {
            if (normalized in setOf("sí", "si", "sí, hazlo", "si hazlo", "hazlo", "adelante", "confirmo", "confirma")) {
                val request = pendingEmail!!
                pendingEmail = null
                respond(emailExecutor.send(request))
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo hagas")) {
                pendingEmail = null
                respond("Listo, no preparé el correo.")
                return
            }
        }

        if (pendingAgenda != null) {
            if (normalized in setOf("sí", "si", "sí, hazlo", "si hazlo", "hazlo", "adelante", "confirmo", "confirma")) {
                val request = pendingAgenda!!
                pendingAgenda = null
                if (request.type == LumiAgendaRequest.Type.REMINDER) {
                    respond(agendaExecutor.createReminder(request))
                } else {
                    respond(agendaExecutor.openCalendar(request))
                }
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo hagas")) {
                pendingAgenda = null
                respond("Listo, no creé el recordatorio ni el evento.")
                return
            }
        }

        if (pendingMessage != null && pendingContact != null) {
            if (normalized in setOf("sí", "si", "sí, envíalo", "si envialo", "envíalo", "envialo", "hazlo", "adelante")) {
                val request = pendingMessage!!
                val contact = pendingContact!!
                pendingMessage = null
                pendingContact = null
                val result = messageExecutor.send(request, contact)
                if (pendingAgentTasks.isNotEmpty()) {
                    pendingAgentIndex++
                    respond(result + " Continúo con el siguiente paso.")
                    handler.postDelayed({ executeNextAgentTask() }, 250L)
                } else {
                    respond(result)
                }
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo envíes", "no lo envies")) {
                pendingMessage = null
                pendingContact = null
                respond("Listo, no envié nada.")
                return
            }
        }

        val emailRequest = emailEngine.parse(rawText)
        if (emailRequest != null) {
            silenceMode = false
            pendingEmail = emailRequest
            respond(emailExecutor.prepare(emailRequest))
            return
        }

        val agendaRequest = agendaEngine.parse(rawText)
        if (agendaRequest != null) {
            silenceMode = false
            pendingAgenda = agendaRequest
            respond(agendaExecutor.prepare(agendaRequest) + " ¿Quieres que lo haga?")
            return
        }

        val messageRequest = messageEngine.parse(rawText)
        if (messageRequest != null) {
            silenceMode = false
            val (contact, confirmation) = messageExecutor.prepare(messageRequest)
            if (contact == null) {
                respond(confirmation)
            } else {
                pendingMessage = messageRequest
                pendingContact = contact
                respond(confirmation)
            }
            return
        }

        when (val intent = intentEngine.parse(rawText)) {
            LumiIntent.Silence -> {
                silenceMode = true
                stopSpeaking()
                try { recognizer?.cancel() } catch (_: RuntimeException) { }
                status.text = "Lumi está en silencio"
                transcript.text = "Cuando quieras continuar, di «Lumi, sigue»."
            }
            LumiIntent.Resume -> {
                silenceMode = false
                respond("Claro, aquí estoy. Te escucho.")
            }
            LumiIntent.Greeting -> askEducationLumi(rawText)
            LumiIntent.Thanks -> askEducationLumi(rawText)
            LumiIntent.Status -> askEducationLumi(rawText)
            is LumiIntent.OpenApp,
            is LumiIntent.WebSearch,
            is LumiIntent.PlayContent,
            LumiIntent.OpenSettings,
            LumiIntent.VolumeUp,
            LumiIntent.VolumeDown,
            LumiIntent.PlayPause -> executeAndroidAction(intent)
            is LumiIntent.Conversation -> askEducationLumi(rawText)
            else -> askEducationLumi(rawText)
        }
    }

    private fun askEducationLumi(rawText: String) {
        if (!educationBridge.isConfigured()) {
            respond(personality.reply(rawText))
            return
        }
        status.text = "Lumi está pensando…"
        val history = memoryStore.recentTurns()
        educationBridge.lumiConversation(rawText, history) { result ->
            handler.post {
                respond(result)
                educationBridge.syncLumiWellbeingHistory(memoryStore.recentTurns())
            }
        }
    }

    private fun executeNextAgentTask() {
        if (pendingAgentIndex >= pendingAgentTasks.size) {
            val total = pendingAgentTasks.size
            pendingAgentTasks.clear()
            pendingAgentIndex = 0
            respond("Listo. Completé los $total pasos de la tarea.")
            return
        }
        when (val task = pendingAgentTasks[pendingAgentIndex]) {
            is LumiTask.SendMessage -> {
                val (contact, confirmation) = agentExecutor.prepareMessage(task)
                if (contact == null) {
                    pendingAgentTasks.clear()
                    pendingAgentIndex = 0
                    respond(confirmation)
                    return
                }
                pendingMessage = task.request
                pendingContact = contact
                respond("Paso " + (pendingAgentIndex + 1) + ": " + confirmation + " ¿Lo envío?")
            }
            else -> {
                agentExecutor.executeNonMessage(task) { result ->
                    handler.post {
                        pendingAgentIndex++
                        if (result.isNotBlank()) {
                            respond(result)
                        } else {
                            executeNextAgentTask()
                        }
                    }
                }
            }
        }
    }

    private fun executeAndroidAction(intent: LumiIntent) {
        silenceMode = false
        speaking = true
        listening = false
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        status.text = "Lumi está actuando…"
        actionExecutor.execute(intent) { message, success ->
            handler.post {
                if (message.isBlank()) {
                    speaking = false
                    status.text = "Te escucho…"
                    startListening()
                    return@post
                }
                respond(message)
            }
        }
    }

    private fun respond(text: String) {
        memoryStore.addTurn("lumi", text)
        silenceMode = false
        speaking = true
        listening = false
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        status.text = "Lumi está hablando…"
        transcript.text = text

        fun nativeFallback() {
            voiceManager?.speak(
                text,
                onStart = { handler.post { status.text = "Lumi está hablando…" } },
                onDone = { handler.post { if (speaking) { speaking = false; status.text = "Lumi está escuchando…"; startListening() } } },
                onError = { handler.post { speaking = false; status.text = "Lumi sigue disponible"; startListening() } }
            )
        }

        if (educationBridge.isConfigured()) {
            educationBridge.lumiTts(
                text,
                voiceManager?.educationVoiceId ?: "Gacrux",
                callback = { audio, mime ->
                    handler.post {
                        neuralTtsPlayer?.play(
                            audio, mime,
                            onStart = { status.text = "Lumi está hablando…" },
                            onDone = { if (speaking) { speaking = false; status.text = "Lumi está escuchando…"; startListening() } },
                            onError = { nativeFallback() }
                        )
                    }
                },
                onError = { handler.post { nativeFallback() } }
            )
        } else {
            nativeFallback()
        }
    }

    private fun stopSpeaking() {
        if (!speaking) return
        listenGeneration++
        listenScheduled = false
        voiceManager?.stop()
        neuralTtsPlayer?.stop()
        speaking = false
        status.text = "Te escucho…"
        handler.post { startListening() }
    }

    override fun onHide() {
        listenGeneration++
        listenScheduled = false
        super.onHide()
        try { recognizer?.cancel() } catch (_: RuntimeException) { }
        voiceManager?.stop()
        neuralTtsPlayer?.stop()
        handler.removeCallbacksAndMessages(null)
        listening = false
        speaking = false
    }

    override fun onDestroy() {
        listenGeneration++
        listenScheduled = false
        try { recognizer?.destroy() } catch (_: RuntimeException) { }
        recognizer = null
        neuralTtsPlayer?.stop()
        neuralTtsPlayer = null
        voiceManager?.shutdown()
        voiceManager = null
        educationBridge.shutdown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
