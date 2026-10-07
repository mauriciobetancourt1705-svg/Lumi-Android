package com.lumi.android

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lumi.android.education.LumiEducationBridge
import com.lumi.android.autonomy.LumiAutonomyController
import com.lumi.android.personality.LumiPersonalityEngine
import com.lumi.android.voice.LumiVoiceManager
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var voiceManager: LumiVoiceManager
    private lateinit var educationBridge: LumiEducationBridge
    private lateinit var autonomy: LumiAutonomyController
    private lateinit var personality: LumiPersonalityEngine
    private lateinit var educationUrl: EditText
    private lateinit var educationToken: EditText
    private lateinit var educationStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = LumiVoiceManager(this)
        educationBridge = LumiEducationBridge(this)
        autonomy = LumiAutonomyController(this)
        personality = LumiPersonalityEngine(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.lumi_logo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 260
            )
        })

        root.addView(TextView(this).apply {
            text = "Lumi · Agente Asistente de IA"
            textSize = 25f
            setPadding(0, 12, 0, 16)
        })

        root.addView(TextView(this).apply {
            text = "Asistente de voz para ayudarte con acciones, información y acompañamiento."
            textSize = 15f
            setPadding(0, 0, 0, 16)
        })

        val voiceButton = Button(this).apply {
            text = "Hablar con Lumi"
            setOnClickListener { voiceManager.startListening() }
        }
        root.addView(voiceButton)

        root.addView(TextView(this).apply {
            text = "Autonomía y privacidad"
            textSize = 21f
            setPadding(0, 28, 0, 8)
        })

        val autonomyStatus = TextView(this).apply {
            text = autonomy.status(this@MainActivity)
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        root.addView(autonomyStatus)

        root.addView(Button(this).apply {
            text = if (autonomy.backgroundListening) "Desactivar escucha en segundo plano"
            else "Autorizar escucha en segundo plano"
            setOnClickListener {
                if (!autonomy.enabled) autonomy.enabled = true
                autonomy.backgroundListening = !autonomy.backgroundListening
                text = if (autonomy.backgroundListening) "Desactivar escucha en segundo plano"
                else "Autorizar escucha en segundo plano"
                autonomyStatus.text = autonomy.status(this@MainActivity)
            }
        })

        root.addView(TextView(this).apply {
            text = autonomy.privacySummary()
            textSize = 13f
            setPadding(0, 8, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "Conexión con Education"
            textSize = 21f
            setPadding(0, 28, 0, 8)
        })

        educationUrl = EditText(this).apply {
            hint = "URL de Education (https://...)"
            setSingleLine(true)
            setText(educationBridge.config().baseUrl)
        }
        root.addView(educationUrl)

        educationToken = EditText(this).apply {
            hint = "Token Bearer de Education"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(educationBridge.config().token)
        }
        root.addView(educationToken)

        educationStatus = TextView(this).apply {
            text = "Education: sin configurar"
            textSize = 14f
            setPadding(0, 8, 0, 4)
        }
        root.addView(educationStatus)

        root.addView(Button(this).apply {
            text = "Guardar conexión"
            setOnClickListener {
                educationBridge.saveConfig(
                    educationUrl.text.toString(),
                    educationToken.text.toString()
                )
                educationStatus.text = "Education: configuración guardada"
            }
        })

        root.addView(Button(this).apply {
            text = "Probar conexión con Education"
            setOnClickListener {
                educationBridge.saveConfig(
                    educationUrl.text.toString(),
                    educationToken.text.toString()
                )
                educationStatus.text = "Education: comprobando…"
                educationBridge.health { result ->
                    runOnUiThread {
                        educationStatus.text = "Education: $result"
                    }
                }
            }
        })

        root.addView(TextView(this).apply {
            text = "Capa 13 · Personalidad y bienestar"
            textSize = 21f
            setPadding(0, 28, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "Lumi puede acercarse una vez al día para preguntarte cómo estás. Tú decides cuándo y puedes ignorar el aviso."
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })

        root.addView(Button(this).apply {
            text = "Programar check-in diario de Lumi"
            setOnClickListener {
                scheduleDailyWellbeingCheckIn()
                voiceManager.speak("Listo. Te recordaré una vez al día para saber cómo estás.")
            }
        })

        root.addView(Button(this).apply {
            text = "Ajustes de asistente"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            }
        })

        setContentView(root)

        if (intent?.action == "com.lumi.android.CHECK_IN") {
            root.postDelayed({
                voiceManager.speak(
                    personality.greeting(
                        moment = LumiPersonalityEngine.Moment.CHECK_IN
                    )
                )
            }, 350L)
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
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(this, LumiDailyCheckInReceiver::class.java).apply {
            action = "com.lumi.android.CHECK_IN"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1900,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(AlarmManager::class.java)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun updateVoiceList() {}
    private fun updateLabels() {}

    override fun onDestroy() {
        voiceManager.shutdown()
        super.onDestroy()
    }
}
