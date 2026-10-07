package com.lumi.android.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import java.text.Normalizer
import java.util.Locale

class LumiAndroidActionExecutor(private val context: Context) {
    private val packageManager = context.packageManager
    private val locale = Locale("es", "VE")

    fun execute(intent: LumiIntent, onResult: (String, Boolean) -> Unit) {
        when (intent) {
            is LumiIntent.OpenApp -> openApp(intent.query, onResult)
            is LumiIntent.WebSearch -> webSearch(intent.query, onResult)
            is LumiIntent.PlayContent -> playContent(intent.query, onResult)
            LumiIntent.OpenSettings -> openSettings(onResult)
            LumiIntent.VolumeUp -> changeVolume(AudioManager.ADJUST_RAISE, onResult)
            LumiIntent.VolumeDown -> changeVolume(AudioManager.ADJUST_LOWER, onResult)
            LumiIntent.PlayPause -> mediaPlayPause(onResult)
            else -> onResult("", false)
        }
    }

    private fun openApp(query: String, onResult: (String, Boolean) -> Unit) {
        val normalizedQuery = normalize(query)
        val candidates = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        val match = candidates.firstOrNull { info ->
            val label = normalize(info.loadLabel(packageManager).toString())
            label == normalizedQuery || label.contains(normalizedQuery) || normalizedQuery.contains(label)
        }
        if (match == null) {
            onResult("No encontré una aplicación llamada " + query + " en este teléfono.", false)
            return
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName)
        if (launchIntent == null) {
            onResult("Encontré " + query + ", pero Android no permite abrirla desde aquí.", false)
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
            onResult("Listo, abrí " + match.loadLabel(packageManager) + ".", true)
        } catch (_: SecurityException) {
            onResult("Android no me permitió abrir esa aplicación.", false)
        } catch (_: RuntimeException) {
            onResult("No pude abrir esa aplicación.", false)
        }
    }

    private fun webSearch(query: String, onResult: (String, Boolean) -> Unit) {
        if (query.isBlank()) {
            onResult("Dime qué quieres que busque.", false)
            return
        }
        val url = "https://www.google.com/search?q=" + Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            onResult("Listo. Buscando " + query + ".", true)
        } catch (_: RuntimeException) {
            onResult("No pude abrir el buscador.", false)
        }
    }

    private fun playContent(query: String, onResult: (String, Boolean) -> Unit) {
        if (query.isBlank()) {
            onResult("Dime qué quieres que reproduzca.", false)
            return
        }
        val youtubeUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
        val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(youtubeIntent)
            onResult("Listo. Buscando " + query + " en YouTube.", true)
        } catch (_: RuntimeException) {
            webSearch(query, onResult)
        }
    }

    private fun openSettings(onResult: (String, Boolean) -> Unit) {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            onResult("Listo. Abrí los ajustes del teléfono.", true)
        } catch (_: RuntimeException) {
            onResult("No pude abrir los ajustes.", false)
        }
    }

    private fun changeVolume(direction: Int, onResult: (String, Boolean) -> Unit) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        onResult(if (direction == AudioManager.ADJUST_RAISE) "Subí el volumen." else "Bajé el volumen.", true)
    }

    private fun mediaPlayPause(onResult: (String, Boolean) -> Unit) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        @Suppress("DEPRECATION")
        audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        onResult("Listo. Controlé la reproducción.", true)
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(locale), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
}
