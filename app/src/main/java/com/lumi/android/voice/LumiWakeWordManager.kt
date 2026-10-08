package com.lumi.android.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.openwakeword.OpenWakeWord

/**
 * Detector de palabra de activación completamente local.
 *
 * Importante: OpenWakeWord captura el micrófono internamente. Por eso Lumi
 * detiene este detector antes de iniciar SpeechRecognizer y lo reactiva
 * después de terminar la interacción.
 *
 * El modelo incluido por la librería es "Hey Jarvis". La arquitectura queda
 * encapsulada para poder sustituirlo posteriormente por un modelo "Lumi"
 * entrenado para openWakeWord sin cambiar el resto del servicio.
 */
class LumiWakeWordManager(
    context: Context,
    private val onWakeWord: (score: Float) -> Unit
) {
    private val appContext = context.applicationContext
    private var detector: OpenWakeWord? = null
    private var running = false

    fun isAvailable(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun start(): Boolean {
        if (running || !isAvailable()) return false

        return try {
            val instance = OpenWakeWord.Builder(appContext)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(0.5f)
                .setDebounceMs(2000L)
                .build()

            detector = instance
            running = true
            instance.start { score ->
                if (running) onWakeWord(score)
            }
            true
        } catch (_: Throwable) {
            running = false
            detector?.release()
            detector = null
            false
        }
    }

    fun stop() {
        if (!running && detector == null) return
        running = false
        try { detector?.stop() } catch (_: Throwable) {}
    }

    fun release() {
        running = false
        try { detector?.stop() } catch (_: Throwable) {}
        try { detector?.release() } catch (_: Throwable) {}
        detector = null
    }

    fun isRunning(): Boolean = running
}
