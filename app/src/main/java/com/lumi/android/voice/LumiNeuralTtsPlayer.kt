package com.lumi.android.voice

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream

class LumiNeuralTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var audioFile: File? = null

    fun play(
        audio: ByteArray,
        mimeType: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: () -> Unit
    ) {
        stop()
        try {
            val ext = if (mimeType.contains("mpeg") || mimeType.contains("mp3")) ".mp3" else ".audio"
            val file = File.createTempFile("lumi_neural_", ext, appContext.cacheDir)
            FileOutputStream(file).use { it.write(audio) }
            audioFile = file
            val mp = MediaPlayer()
            player = mp
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                onStart()
                it.start()
            }
            mp.setOnCompletionListener {
                cleanup()
                onDone()
            }
            mp.setOnErrorListener { _, _, _ ->
                cleanup()
                onError()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            cleanup()
            onError()
        }
    }

    fun stop() {
        try { player?.stop() } catch (_: Exception) {}
        cleanup()
    }

    private fun cleanup() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { audioFile?.delete() } catch (_: Exception) {}
        audioFile = null
    }
}
