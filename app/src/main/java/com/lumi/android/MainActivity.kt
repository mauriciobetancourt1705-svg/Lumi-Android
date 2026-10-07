package com.lumi.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Lumi Android · Bloque 1"
            textSize = 24f
            setPadding(32, 48, 32, 24)
        }
        val status = TextView(this).apply {
            text = "Base nativa preparada. Activa el micrófono y selecciona Lumi como asistente del sistema para probar la capa de voz."
            textSize = 16f
            setPadding(32, 0, 32, 24)
        }
        val mic = Button(this).apply {
            text = "Conceder micrófono"
            setOnClickListener { requestMicrophone() }
        }
        val assistant = Button(this).apply {
            text = "Abrir ajustes de asistente"
            setOnClickListener { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(mic)
            addView(assistant)
        })
    }

    private fun requestMicrophone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), requestCode)
        }
    }
}
