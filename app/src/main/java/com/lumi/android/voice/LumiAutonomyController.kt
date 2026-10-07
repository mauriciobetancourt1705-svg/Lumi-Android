package com.lumi.android.voice

import android.content.Context

class LumiAutonomyController(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_autonomy", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()

    var backgroundListening: Boolean
        get() = prefs.getBoolean("background_listening", false)
        set(value) = prefs.edit().putBoolean("background_listening", value).apply()

    fun status(context: Context): String {
        val mic = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val notifications = android.os.Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return when {
            !enabled -> "Lumi 24/7 está desactivado."
            !mic -> "Lumi 24/7 está activado, pero falta permiso de micrófono."
            !notifications -> "Lumi 24/7 está activado, pero falta permiso de notificaciones."
            backgroundListening -> "Lumi 24/7 está activado con escucha en segundo plano autorizada."
            else -> "Lumi está disponible como asistente del sistema; la escucha en segundo plano no está activada."
        }
    }

    fun privacySummary(): String =
        "La escucha en segundo plano solo se activa cuando tú la autorizas. Lumi no activa el micrófono de forma silenciosa."
}
