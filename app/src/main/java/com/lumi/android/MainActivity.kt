package com.lumi.android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import java.util.Calendar
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lumi.android.voice.LumiEducationBridge
import com.lumi.android.voice.LumiAutonomyController
import com.lumi.android.voice.LumiVoiceManager
import com.lumi.android.voice.LumiIntent
import com.lumi.android.voice.LumiIntentEngine
import com.lumi.android.voice.LumiMemoryStore
import com.lumi.android.voice.LumiContextEngine
import com.lumi.android.voice.LumiContextIntent
import com.lumi.android.voice.LumiMessageEngine
import com.lumi.android.voice.LumiMessageActionExecutor
import com.lumi.android.voice.LumiContactResolver
import com.lumi.android.voice.LumiAgendaEngine
import com.lumi.android.voice.LumiAgendaActionExecutor
import com.lumi.android.voice.LumiEmailEngine
import com.lumi.android.voice.LumiEmailActionExecutor
import com.lumi.android.voice.LumiTask
import com.lumi.android.voice.LumiTaskPlanner
import com.lumi.android.voice.LumiTaskExecutor
import com.lumi.android.voice.LumiAgentEngine
import com.lumi.android.voice.LumiAgentExecutor
import com.lumi.android.voice.LumiNotificationStore
import com.lumi.android.voice.LumiEducationIntentEngine
import com.lumi.android.wellbeing.LumiPersonalityEngine

class MainActivity : AppCompatActivity() {
    private lateinit var voiceManager: LumiVoiceManager
    private lateinit var neuralTtsPlayer: com.lumi.android.voice.LumiNeuralTtsPlayer
    private lateinit var voiceSpinner: Spinner
    private lateinit var educationVoiceSpinner: Spinner
    private lateinit var pitchValue: TextView
    private lateinit var rateValue: TextView
    private lateinit var educationBridge: LumiEducationBridge
    private lateinit var educationStatus: TextView
    private lateinit var educationUrl: EditText
    private lateinit var educationToken: EditText
    private lateinit var autonomy: LumiAutonomyController
    private lateinit var autonomyStatus: TextView
    private lateinit var voiceStatus: TextView
    private lateinit var voiceTranscript: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private val voiceHandler = Handler(Looper.getMainLooper())
    private var conversationActive = false
    private var listening = false
    private var speaking = false
    private var listenGeneration = 0L
    private var listenScheduled = false
    private val personality = LumiPersonalityEngine()
    private val intentEngine = LumiIntentEngine()
    private lateinit var androidActionExecutor: com.lumi.android.voice.LumiAndroidActionExecutor
    private lateinit var memoryStore: LumiMemoryStore
    private lateinit var contextEngine: LumiContextEngine
    private lateinit var notificationStore: LumiNotificationStore
    private lateinit var messageEngine: LumiMessageEngine
    private lateinit var messageExecutor: LumiMessageActionExecutor
    private lateinit var agendaEngine: LumiAgendaEngine
    private lateinit var agendaExecutor: LumiAgendaActionExecutor
    private lateinit var emailEngine: LumiEmailEngine
    private lateinit var emailExecutor: LumiEmailActionExecutor
    private lateinit var taskPlanner: LumiTaskPlanner
    private lateinit var taskExecutor: LumiTaskExecutor
    private lateinit var agentEngine: LumiAgentEngine
    private lateinit var agentExecutor: LumiAgentExecutor
    private val educationIntentEngine = LumiEducationIntentEngine()
    private var pendingAgentTasks: MutableList<LumiTask> = mutableListOf()
    private var pendingAgentIndex = 0
    private var pendingMessage: com.lumi.android.voice.LumiMessageRequest? = null
    private var pendingContact: com.lumi.android.voice.LumiContact? = null
    private var pendingAgenda: com.lumi.android.voice.LumiAgendaRequest? = null
    private var pendingEmail: com.lumi.android.voice.LumiEmailRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = LumiVoiceManager(this)
        neuralTtsPlayer = com.lumi.android.voice.LumiNeuralTtsPlayer(this)
        educationVoiceSpinner = Spinner(this)
        val personality = LumiPersonalityEngine()
        educationBridge = LumiEducationBridge(this)
        scheduleDailyWellbeingCheckIn()
        autonomy = LumiAutonomyController(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setBackgroundColor(android.graphics.Color.rgb(8, 18, 32))
        }

        val logo = ImageView(this).apply {
            setImageResource(com.lumi.android.R.drawable.lumi_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        root.addView(logo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 180))

        val title = TextView(this).apply {
            text = "Lumi"
            textSize = 34f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
        }
        root.addView(title, lp())

        root.addView(TextView(this).apply {
            text = "Tu asistente de IA"
            textSize = 17f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = android.view.Gravity.CENTER
        }, lp())

        voiceStatus = TextView(this).apply {
            text = "Estoy lista. Pulsa el botón y háblame."
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(12, 28, 12, 12)
        }
        root.addView(voiceStatus, lp())

        voiceTranscript = TextView(this).apply {
            text = "Aquí aparecerá lo que me digas."
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(18, 18, 18, 24)
            setBackgroundColor(android.graphics.Color.rgb(18, 35, 54))
        }
        root.addView(voiceTranscript, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150).apply { topMargin = 18 })

        root.addView(Button(this).apply {
            text = "🎙  HABLAR CON LUMI"
            textSize = 18f
            setOnClickListener { toggleConversation() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64).apply { topMargin = 24 })

        root.addView(Button(this).apply {
            text = "⚙  Ajustes de Lumi"
            setOnClickListener { showLumiSettings() }
        }, lp())

        voiceSpinner = Spinner(this)
        pitchValue = TextView(this)
        rateValue = TextView(this)
        educationUrl = EditText(this)
        educationToken = EditText(this)
        educationStatus = TextView(this)
        autonomyStatus = TextView(this).apply { text = autonomy.status(this@MainActivity) }

        root.addView(TextView(this).apply {
            text = "Lumi usa el cerebro conversacional de Education y ejecuta acciones de forma nativa en Android."
            textSize = 13f
            setTextColor(android.graphics.Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(8, 22, 8, 8)
        }, lp())
        androidActionExecutor = com.lumi.android.voice.LumiAndroidActionExecutor(this)
        memoryStore = LumiMemoryStore(this)
        contextEngine = LumiContextEngine()
        notificationStore = LumiNotificationStore(this)
        messageEngine = LumiMessageEngine()
        val contactResolver = LumiContactResolver(this)
        messageExecutor = LumiMessageActionExecutor(this, contactResolver)
        agendaEngine = LumiAgendaEngine()
        agendaExecutor = LumiAgendaActionExecutor(this)
        emailEngine = LumiEmailEngine()
        emailExecutor = LumiEmailActionExecutor(this)
        taskPlanner = LumiTaskPlanner(messageEngine)
        taskExecutor = LumiTaskExecutor(messageExecutor, androidActionExecutor)
        agentEngine = LumiAgentEngine(taskPlanner)
        agentExecutor = LumiAgentExecutor(taskExecutor, messageExecutor, contactResolver)
        prepareInAppRecognizer()
        setContentView(root)
        if (intent?.action == "com.lumi.android.CHECK_IN") {
            root.postDelayed({ voiceManager.speak(personality.greeting(moment = LumiPersonalityEngine.Moment.CHECK_IN)) }, 350L)
        }
        voiceManager.onReady {
            runOnUiThread {
                updateVoiceList()
                voiceStatus.text = if (voiceManager.availableSpanishVoices().isEmpty())
                    "Motor de voz listo. No hay voces españolas instaladas; usa «Instalar / configurar voces españolas»."
                else
                    "Voces españolas disponibles: " + voiceManager.availableSpanishVoices().size
            }
        }
        updateVoiceList()
        updateLabels()
    }

    private fun showLumiSettings() {
        val dialog = android.app.Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
        }
        box.addView(TextView(this).apply { text = "Ajustes de Lumi"; textSize = 24f }, lp())
        box.addView(TextView(this).apply { text = "Voz y conexión"; textSize = 17f; setPadding(0, 18, 0, 6) }, lp())
        box.addView(TextView(this).apply { text = "Voz neural de Lumi (Education)"; textSize = 15f }, lp())
        box.addView(educationVoiceSpinner, lp())
        updateEducationVoiceList()
        box.addView(voiceSpinner, lp())
        box.addView(Button(this).apply {
            text = "Probar voz"
            setOnClickListener {
                if (educationBridge.isConfigured()) {
                    educationBridge.lumiTts("Hola, soy Lumi. Estoy aquí contigo.", voiceManager.educationVoiceId,
                        callback = { audio, mime -> runOnUiThread { neuralTtsPlayer.play(audio, mime, {}, {}, { voiceManager.speak("Hola, soy Lumi. Estoy aquí contigo.") }) } },
                        onError = { runOnUiThread { voiceManager.speak("Hola, soy Lumi. Estoy aquí contigo.") } })
                } else voiceManager.speak("Hola, soy Lumi. Estoy aquí contigo.")
            }
        }, lp())
        box.addView(Button(this).apply { text = "Configurar voces españolas"; setOnClickListener { try { startActivity(Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } catch (_: Exception) {} } }, lp())
        box.addView(Button(this).apply { text = "Conexión con Education"; setOnClickListener { showEducationConnectionDialog() } }, lp())
        box.addView(Button(this).apply { text = "Privacidad y permisos"; setOnClickListener { requestMicrophone(); requestNotifications() } }, lp())
        box.addView(Button(this).apply { text = "Lumi 24/7"; setOnClickListener { autonomy.enabled=!autonomy.enabled; if(autonomy.enabled) startLumiBackgroundService() else stopLumiBackgroundService(); autonomyStatus.text=autonomy.status(this@MainActivity) } }, lp())
        dialog.setContentView(box)
        dialog.setTitle("Lumi")
        dialog.show()
    }

    private fun showEducationConnectionDialog() {
        val dialog=android.app.Dialog(this)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,24)}
        box.addView(TextView(this).apply{text="Education · cerebro de Lumi";textSize=22f},lp())
        educationUrl.setText(educationBridge.config().baseUrl)
        educationToken.setText(educationBridge.config().token)
        educationToken.inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        box.addView(educationUrl,lp())
        box.addView(educationToken,lp())
        educationStatus.text=if(educationBridge.isConfigured()) "Conexión configurada" else "Conexión pendiente"
        box.addView(educationStatus,lp())
        box.addView(Button(this).apply{text="Guardar y probar";setOnClickListener{
            educationBridge.saveConfig(educationUrl.text.toString(),educationToken.text.toString())
            educationBridge.health{result->runOnUiThread{educationStatus.text="Education: $result"}}
        }},lp())
        dialog.setContentView(box);dialog.show()
    }

    private fun scheduleDailyWellbeingCheckIn() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(this, com.lumi.android.voice.LumiReminderReceiver::class.java).apply {
            putExtra("title", "Lumi quiere saber cómo estás. ¿Hablamos un momento?")
            putExtra("lumi_check_in", true)
        }
        val pending = PendingIntent.getBroadcast(this, 13013, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
    }
    private fun updateEducationVoiceList() {
        val profiles = LumiVoiceManager.EDUCATION_VOICES
        educationVoiceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profiles.map { it.second })
        val selected = profiles.indexOfFirst { it.first == voiceManager.educationVoiceId }
        if (selected >= 0) educationVoiceSpinner.setSelection(selected)
        educationVoiceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position in profiles.indices) voiceManager.educationVoiceId = profiles[position].first
            }
        }
    }

    private fun updateVoiceList() {
        val voices = voiceManager.availableSpanishVoices()
        val labels = voices.map { voiceManager.voiceLabel(it) }.ifEmpty { listOf("No hay voces españolas disponibles") }
        voiceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        val selected = voices.indexOfFirst { it.name == voiceManager.selectedVoiceName }
        if (selected >= 0) voiceSpinner.setSelection(selected)
        voiceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { if (position in voices.indices) voiceManager.selectVoice(voices[position]) }
        }
    }
    private fun updateLabels() {
        pitchValue.text = "Tono: " + "%.2f".format(voiceManager.pitch)
        rateValue.text = "Velocidad: " + "%.2f".format(voiceManager.speechRate)
    }
    private fun seekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange(progress)
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }
    private fun lp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 }
    private fun startLumiBackgroundService() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestMicrophone()
            return
        }
        val serviceIntent = Intent(this, com.lumi.android.voice.LumiBackgroundVoiceService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
            autonomy.backgroundListening = true
            autonomyStatus.text = autonomy.status(this)
        } catch (_: RuntimeException) {
            autonomy.backgroundListening = false
            autonomyStatus.text = "Android no permitió iniciar la escucha en segundo plano desde este momento."
        }
    }

    private fun stopLumiBackgroundService() {
        stopService(Intent(this, com.lumi.android.voice.LumiBackgroundVoiceService::class.java))
        autonomy.backgroundListening = false
        autonomyStatus.text = autonomy.status(this)
    }

    private fun requestContacts() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), 1002)
    }
    private fun requestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1003)
    }
    private fun requestMicrophone() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1001)
        } else {
            startConversation()
        }
    }

    private fun toggleConversation() {
        if (conversationActive) {
            stopConversation()
            return
        }
        requestMicrophone()
    }

    private fun prepareInAppRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceStatus = voiceStatus.takeIf { ::voiceStatus.isInitialized } ?: TextView(this)
            return
        }
        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(this)
        } catch (_: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                null
            }
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                voiceStatus.text = "Lumi está escuchando…"
            }
            override fun onBeginningOfSpeech() {
                voiceStatus.text = "Te escucho…"
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onPartialResults(results: Bundle?) {
                val partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) voiceTranscript.text = partial
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (spoken.isBlank()) {
                    voiceStatus.text = "No alcancé a entenderte. Te escucho de nuevo…"
                    scheduleConversationListen(500L)
                    return
                }
                voiceTranscript.text = spoken
                handleInAppCommand(spoken)
            }
            override fun onEndOfSpeech() {
                listening = false
                if (!speaking) voiceStatus.text = "Procesando…"
            }
            override fun onError(error: Int) {
                listening = false
                if (conversationActive && !speaking) {
                    voiceStatus.text = "No pude escucharte. Te vuelvo a escuchar…"
                    scheduleConversationListen(700L)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    private fun startConversation() {
        listenGeneration++
        listenScheduled = false
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1001)
            return
        }
        if (speechRecognizer == null) {
            prepareInAppRecognizer()
        }
        conversationActive = true
        voiceStatus.text = "Preparando el micrófono…"
        scheduleConversationListen(150L)
    }

    private fun scheduleConversationListen(delay: Long) {
        if (!conversationActive || speaking || listening || speechRecognizer == null) return
        voiceHandler.removeCallbacksAndMessages(null)
        val generation = ++listenGeneration
        listenScheduled = true
        voiceHandler.postDelayed({
            if (generation != listenGeneration) { listenScheduled = false; return@postDelayed }
            listenScheduled = false
            startListeningNow()
        }, delay)
    }

    private fun startListeningNow() {
        if (!conversationActive || speaking || listening || speechRecognizer == null || listenScheduled) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
        }
        try {
            voiceStatus.text = "Lumi está escuchando…"
            speechRecognizer?.startListening(intent)
        } catch (_: RuntimeException) {
            voiceStatus.text = "No pude iniciar el micrófono. Reintenta."
            scheduleConversationListen(1000L)
        }
    }

    private fun handleInAppCommand(rawText: String) {
        memoryStore.addTurn("usuario", rawText)

        val educationIntent = educationIntentEngine.parse(rawText)
        if (educationIntent != null) {
            when (educationIntent) {
                is com.lumi.android.voice.LumiEducationIntent.Tutor ->
                    educationBridge.tutor(educationIntent.text, "aprender", memoryStore.recentTurns()) { result ->
                        runOnUiThread { speakInApp(result) }
                    }
                is com.lumi.android.voice.LumiEducationIntent.Oraculo ->
                    educationBridge.oraculo(educationIntent.text, memoryStore.recentTurns()) { result ->
                        runOnUiThread { speakInApp(result) }
                    }
                com.lumi.android.voice.LumiEducationIntent.BcvRate ->
                    educationBridge.bcvRate { result -> runOnUiThread { speakInApp(result) } }
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
                LumiContextIntent.RecentNotifications -> speakInApp(notificationStore.summary())
                LumiContextIntent.ClearRecentNotifications -> {
                    notificationStore.clear()
                    speakInApp("Listo. Borré de la memoria local las notificaciones que Lumi había guardado.")
                }
            }
            return
        }

        val normalized = rawText.trim().lowercase(java.util.Locale("es", "VE"))

        if (pendingEmail != null) {
            if (normalized in setOf("sí", "si", "sí, hazlo", "si hazlo", "hazlo", "adelante", "confirmo", "confirma")) {
                val request = pendingEmail!!
                pendingEmail = null
                speakInApp(emailExecutor.send(request))
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo hagas")) {
                pendingEmail = null
                speakInApp("Listo, no envié el correo.")
                return
            }
        }

        if (pendingAgenda != null) {
            if (normalized in setOf("sí", "si", "sí, hazlo", "si hazlo", "hazlo", "adelante", "confirmo", "confirma")) {
                val request = pendingAgenda!!
                pendingAgenda = null
                if (request.type == com.lumi.android.voice.LumiAgendaRequest.Type.REMINDER) {
                    speakInApp(agendaExecutor.createReminder(request))
                } else {
                    speakInApp(agendaExecutor.openCalendar(request))
                }
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo hagas")) {
                pendingAgenda = null
                speakInApp("Listo, cancelé esa acción.")
                return
            }
        }

        if (pendingMessage != null && pendingContact != null) {
            if (normalized in setOf("sí", "si", "sí, envíalo", "si envialo", "envíalo", "envialo", "hazlo", "adelante")) {
                val request = pendingMessage!!
                val contact = pendingContact!!
                pendingMessage = null
                pendingContact = null
                speakInApp(messageExecutor.send(request, contact))
                return
            }
            if (normalized in setOf("no", "cancelar", "cancela", "no lo envíes", "no lo envies")) {
                pendingMessage = null
                pendingContact = null
                speakInApp("Listo, no envié nada.")
                return
            }
        }

        emailEngine.parse(rawText)?.let {
            pendingEmail = it
            speakInApp(emailExecutor.prepare(it))
            return
        }

        agendaEngine.parse(rawText)?.let {
            pendingAgenda = it
            speakInApp(agendaExecutor.prepare(it) + " ¿Quieres que lo haga?")
            return
        }

        messageEngine.parse(rawText)?.let {
            val (contact, confirmation) = messageExecutor.prepare(it)
            if (contact == null) speakInApp(confirmation)
            else {
                pendingMessage = it
                pendingContact = contact
                speakInApp(confirmation)
            }
            return
        }

        when (val intent = intentEngine.parse(rawText)) {
            LumiIntent.Silence -> {
                conversationActive = false
                try { speechRecognizer?.cancel() } catch (_: RuntimeException) {}
                voiceStatus.text = "Lumi está en silencio."
            }
            LumiIntent.Resume -> speakInApp("Claro. Aquí estoy. Te escucho.")
            LumiIntent.Greeting -> askEducationLumiInApp(rawText)
            LumiIntent.Thanks -> askEducationLumiInApp(rawText)
            LumiIntent.Status -> askEducationLumiInApp(rawText)
            LumiIntent.AutonomyStatus -> speakInApp(autonomy.status(this))
            LumiIntent.Privacy -> speakInApp(autonomy.privacySummary())
            is LumiIntent.OpenApp,
            is LumiIntent.WebSearch,
            is LumiIntent.PlayContent,
            LumiIntent.OpenSettings,
            LumiIntent.VolumeUp,
            LumiIntent.VolumeDown,
            LumiIntent.PlayPause -> {
                try { speechRecognizer?.cancel() } catch (_: RuntimeException) {}
                androidActionExecutor.execute(intent) { message, _ ->
                    runOnUiThread { if (message.isBlank()) scheduleConversationListen(300L) else speakInApp(message) }
                }
            }
            is LumiIntent.Conversation -> askEducationLumiInApp(rawText)
            else -> askEducationLumiInApp(rawText)
        }
    }

    private fun askEducationLumiInApp(rawText: String) {
        if (!educationBridge.isConfigured()) {
            speakInApp(personality.reply(rawText))
            return
        }
        voiceStatus.text = "Lumi está pensando…"
        educationBridge.lumiConversation(rawText, memoryStore.recentTurns()) { result ->
            runOnUiThread { speakInApp(result) }
        }
    }

    private fun executeNextAgentTask() {
        if (pendingAgentIndex >= pendingAgentTasks.size) {
            val total = pendingAgentTasks.size
            pendingAgentTasks.clear()
            pendingAgentIndex = 0
            speakInApp("Listo. Completé los $total pasos de la tarea.")
            return
        }
        when (val task = pendingAgentTasks[pendingAgentIndex]) {
            is LumiTask.SendMessage -> {
                val (contact, confirmation) = agentExecutor.prepareMessage(task)
                if (contact == null) {
                    pendingAgentTasks.clear()
                    pendingAgentIndex = 0
                    speakInApp(confirmation)
                    return
                }
                pendingMessage = task.request
                pendingContact = contact
                speakInApp("Paso " + (pendingAgentIndex + 1) + ": " + confirmation + " ¿Lo envío?")
            }
            else -> agentExecutor.executeNonMessage(task) { result ->
                runOnUiThread {
                    pendingAgentIndex++
                    if (result.isNotBlank()) speakInApp(result) else executeNextAgentTask()
                }
            }
        }
    }

    private fun speakInApp(text: String) {
        if (!conversationActive) return
        speaking = true
        listening = false
        try { speechRecognizer?.cancel() } catch (_: RuntimeException) {}
        voiceStatus.text = "Lumi está hablando…"
        voiceTranscript.text = text

        fun nativeFallback() {
            voiceManager.speak(
                text,
                onStart = { runOnUiThread { voiceStatus.text = "Lumi está hablando…" } },
                onDone = { runOnUiThread { speaking = false; if (conversationActive) { voiceStatus.text = "Lumi está escuchando…"; scheduleConversationListen(300L) } } },
                onError = { runOnUiThread { speaking = false; if (conversationActive) { voiceStatus.text = "Lumi sigue disponible."; scheduleConversationListen(500L) } } }
            )
        }

        if (educationBridge.isConfigured()) {
            educationBridge.lumiTts(
                text,
                voiceManager.educationVoiceId,
                callback = { audio, mime ->
                    runOnUiThread {
                        neuralTtsPlayer.play(
                            audio, mime,
                            onStart = { voiceStatus.text = "Lumi está hablando…" },
                            onDone = { speaking = false; if (conversationActive) { voiceStatus.text = "Lumi está escuchando…"; scheduleConversationListen(300L) } },
                            onError = { nativeFallback() }
                        )
                    }
                },
                onError = { runOnUiThread { nativeFallback() } }
            )
        } else nativeFallback()
    }

    private fun stopConversation() {
        listenGeneration++
        listenScheduled = false
        conversationActive = false
        speaking = false
        listening = false
        voiceHandler.removeCallbacksAndMessages(null)
        try { speechRecognizer?.cancel() } catch (_: RuntimeException) {}
        voiceManager.stop()
        neuralTtsPlayer.stop()
        voiceStatus.text = "Lumi está en espera."
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startConversation()
            } else {
                voiceStatus.text = "Necesito permiso de micrófono para escucharte."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::voiceSpinner.isInitialized) {
            voiceManager.onReady { runOnUiThread { updateVoiceList() } }
        }
    }

    override fun onDestroy() {
        voiceHandler.removeCallbacksAndMessages(null)
        try { speechRecognizer?.destroy() } catch (_: RuntimeException) {}
        speechRecognizer = null
        educationBridge.shutdown()
        neuralTtsPlayer.stop()
        voiceManager.shutdown()
        super.onDestroy()
    }
}