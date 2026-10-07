package com.lumi.android.voice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LumiNotification(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long
)

class LumiNotificationStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_notifications", Context.MODE_PRIVATE)
    private val key = "recent"
    private val maxItems = 20

    @Synchronized
    fun add(item: LumiNotification) {
        val array = JSONArray(prefs.getString(key, "[]"))
        val next = JSONArray()
        next.put(JSONObject().apply {
            put("packageName", item.packageName)
            put("appName", item.appName)
            put("title", item.title)
            put("text", item.text)
            put("postedAt", item.postedAt)
        })
        for (i in 0 until array.length()) {
            if (next.length() >= maxItems) break
            next.put(array.optJSONObject(i) ?: JSONObject())
        }
        prefs.edit().putString(key, next.toString()).apply()
    }

    @Synchronized
    fun recent(limit: Int = 8): List<LumiNotification> {
        val array = JSONArray(prefs.getString(key, "[]"))
        return buildList {
            for (i in 0 until minOf(limit, array.length())) {
                val o = array.optJSONObject(i) ?: continue
                add(LumiNotification(
                    packageName = o.optString("packageName"),
                    appName = o.optString("appName"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    postedAt = o.optLong("postedAt")
                ))
            }
        }
    }

    fun summary(limit: Int = 5): String {
        val items = recent(limit)
        if (items.isEmpty()) return "No tengo notificaciones recientes disponibles."
        return items.joinToString(separator = " ") { item ->
            val body = listOf(item.appName, item.title, item.text)
                .filter { it.isNotBlank() }
                .joinToString(": ")
            body.take(180)
        }
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(key).apply()
    }
}
