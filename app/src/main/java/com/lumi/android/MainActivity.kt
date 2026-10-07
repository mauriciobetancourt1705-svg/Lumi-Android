package com.lumi.android

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lumi.android.voice.LumiVoiceManager

class MainActivity : AppCompatActivity() {
    private lateinit var voiceManager: LumiVoiceManager
    private lateinit var voiceSpinner: Spinner
    private lateinit var pitchValue: TextView
    private lateinit var rateValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = LumiVoiceManager(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 42, 32, 32) }
        root.addView(TextView(this).apply { text = "Lumi Android · Voz"; textSize = 26f })
        root.addView(TextView(this).apply { text = "Elige la voz que más te guste. Las opciones dependen de las voces instaladas en tu Android."; textSize = 16f; setPadding(0, 12, 0, 20) })
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
        root.addView(Button(this).apply { text = "Ajustes de asistente"; setOnClickListener { startActivity(android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)) } }, lp())
        setContentView(root)
        updateVoiceList()
        updateLabels()
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
    override fun onDestroy() { voiceManager.shutdown(); super.onDestroy() }
}
