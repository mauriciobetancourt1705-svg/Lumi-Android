package com.lumi.android.voice

import java.util.Locale

sealed interface LumiContextIntent {
    data object RecentNotifications : LumiContextIntent
    data object ClearRecentNotifications : LumiContextIntent
}

class LumiContextEngine {
    fun parse(text: String): LumiContextIntent? {
        val t = text.trim().lowercase(Locale("es", "VE"))
        return when {
            t.contains("qué notificaciones") ||
                t.contains("que notificaciones") ||
                t.contains("mis notificaciones") ||
                t.contains("notificaciones recientes") ||
                t.contains("qué me llegó") ||
                t.contains("que me llego") -> LumiContextIntent.RecentNotifications

            t.contains("borra las notificaciones") ||
                t.contains("limpia las notificaciones") ||
                t.contains("olvida las notificaciones") -> LumiContextIntent.ClearRecentNotifications

            else -> null
        }
    }
}
