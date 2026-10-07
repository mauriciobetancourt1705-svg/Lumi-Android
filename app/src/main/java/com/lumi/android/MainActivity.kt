package com.lumi.android

import android.os.Bundle
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
import com.lumi.android.wellbeing.LumiPersonalityEngine

class MainActivity : AppCompatActivity() {
    private lateinit var voiceManager: LumiVoiceManager
    private lateinit var voiceSpinner: Spinner
    private lateinit var pitchValue: TextView
    private lateinit var rateValue: TextView
    private lateinit var educationBridge: LumiEducationBridge
    private lateinit var educationStatus: TextView
    private lateinit var educationUrl: EditText
    private lateinit var educationToken: EditText
    private lateinit var autonomy: LumiAutonomyController
    private lateinit var autonomyStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = LumiVoiceManager(this)
        val personality = LumiPersonalityEngine()
        educationBridge = LumiEducationBridge(this)
        scheduleDailyWellbeingCheckIn()
        autonomy = LumiAutonomyController(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 42, 32, 32) }
        root.addView(ImageView(this).apply {
            setImageResource(com.lumi.android.R.drawable.lumi_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(0, 0, 0, 16)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220))
        root.addView(TextView(this).apply { text = "Lumi · Agente Asistente de IA"; textSize = 26f })
        root.addView(TextView(this).apply { text = "Voz, acciones Android y conexión opcional con Education."; textSize = 16f; setPadding(0, 12, 0, 20) })
        voiceSpinner = Spinner(this)
        root.addView(voiceSpinner, lp())
        pitchValue = TextView(this).apply { textSize = 15f }
        root.addView(pitchValue)
        root.addView(SeekBar(this).apply {
            max = 200
            progress = ((voiceManager.pitch - 0.5f) * 100f).toInt().coerceIn(0, 200)
            setOnSeekBarChangeListener(seekListener { value -> voiceManager.pitch = 0.5f + value / 100f; updateLabels() })
        }, lp())
        rateValue = TextView(this).apply { textSize = 15f }
        root.addView(rateValue)
        root.addView(SeekBar(this).apply {
            max = 150
            progress = ((voiceManager.speechRate - 0.5f) * 100f).toInt().coerceIn(0, 150)
            setOnSeekBarChangeListener(seekListener { value -> voiceManager.speechRate = 0.5f + value / 100f; updateLabels() })
        }, lp())
        root.addView(Button(this).apply { text = "Probar voz de Lumi"; setOnClickListener { voiceManager.speak("Hola, soy Lumi. Esta es la voz que has elegido.") } }, lp())
        root.addView(Button(this).apply { text = "Conceder micrófono"; setOnClickListener { requestMicrophone() } }, lp())
        root.addView(Button(this).apply { text = "Conceder acceso a contactos"; setOnClickListener { requestContacts() } }, lp())
        root.addView(Button(this).apply { text = "Conceder notificaciones"; setOnClickListener { requestNotifications() } }, lp())
        root.addView(Button(this).apply { text = "Activar acceso contextual"; setOnClickListener { startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) } }, lp())
        root.addView(TextView(this).apply { text = "Lumi 24/7 · Seguridad y autonomía"; textSize = 21f; setPadding(0, 28, 0, 8) })
        autonomyStatus = TextView(this).apply { textSize = 14f; text = autonomy.status(this@MainActivity) }
        root.addView(autonomyStatus)
        root.addView(Button(this).apply {
            text = if (autonomy.enabled) "Desactivar Lumi 24/7" else "Activar Lumi 24/7"
            setOnClickListener {
                autonomy.enabled = !autonomy.enabled
                text = if (autonomy.enabled) "Desactivar Lumi 24/7" else "Activar Lumi 24/7"
                autonomyStatus.text = autonomy.status(this@MainActivity)
            }
        }, lp())
        root.addView(Button(this).apply {
            text = if (autonomy.backgroundListening) "Desactivar escucha en segundo plano" else "Autorizar escucha en segundo plano"
            setOnClickListener {
                if (!autonomy.enabled) autonomy.enabled = true
                autonomy.backgroundListening = !autonomy.backgroundListening
                text = if (autonomy.backgroundListening) "Desactivar escucha en segundo plano" else "Autorizar escucha en segundo plano"
                autonomyStatus.text = autonomy.status(this@MainActivity)
            }
        }, lp())
        root.addView(TextView(this).apply { text = autonomy.privacySummary(); textSize = 13f; setPadding(0, 8, 0, 8) })
        root.addView(TextView(this).apply { text = "Conexión con Education"; textSize = 21f; setPadding(0, 28, 0, 8) })
        educationUrl = EditText(this).apply {
            hint = "URL de Education (https://...)"
            singleLine = true
            setText(educationBridge.config().baseUrl)
        }
        root.addView(educationUrl, lp())
        educationToken = EditText(this).apply {
            hint = "Token Bearer de Education"
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(educationBridge.config().token)
        }
        root.addView(educationToken, lp())
        educationStatus = TextView(this).apply { text = "Education: sin configurar"; textSize = 14f; setPadding(0, 8, 0, 4) }
        root.addView(educationStatus)
        root.addView(Button(this).apply {
            text = "Guardar conexión"
            setOnClickListener {
                educationBridge.saveConfig(educationUrl.text.toString(), educationToken.text.toString())
                educationStatus.text = "Education: configuración guardada"
            }
        }, lp())
        root.addView(Button(this).apply {
            text = "Probar conexión con Education"
            setOnClickListener {
                educationBridge.saveConfig(educationUrl.text.toString(), educationToken.text.toString())
                educationStatus.text = "Education: comprobando…"
                educationBridge.health { result -> runOnUiThread { educationStatus.text = "Education: $result" } }
            }
        }, lp())
        root.addView(TextView(this).apply { text = "Capa 13 · Personalidad y bienestar"; textSize = 21f; setPadding(0, 28, 0, 8) })
        root.addView(TextView(this).apply { text = "Lumi puede acercarse una vez al día para preguntarte cómo estás. Tú decides cuándo y puedes ignorar el aviso."; textSize = 14f; setPadding(0, 0, 0, 8) })
        root.addView(Button(this).apply { text = "Programar check-in diario de Lumi"; setOnClickListener { scheduleDailyWellbeingCheckIn(); voiceManager.speak("Listo. Te recordaré una vez al día para saber cómo estás.") } }, lp())
        root.addView(Button(this).apply { text = "Ajustes de asistente"; setOnClickListener { startActivity(android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)) } }, lp())
        setContentView(root)
        if (intent?.action == "com.lumi.android.CHECK_IN") {
            root.postDelayed { voiceManager.speak(personality.greeting(moment = LumiPersonalityEngine.Moment.CHECK_IN)) }, 350L)
        }
        updateVoiceList()
        updateLabels()
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
    private fun requestContacts() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), 1002)
    }
    private fun requestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1003)
    }
    private fun requestMicrophone() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1001)
    }
    override fun onDestroy() {
        educationBridge.shutdown()
        voiceManager.shutdown()
        super.onDestroy()
    }
}