package com.lumi.android.voice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LumiTurn(val role: String, val text: String)

class LumiMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_memory", Context.MODE_PRIVATE)
    private val keyTurns = "recent_turns"
    private val maxTurns = 12

    @Synchronized
    fun addTurn(role: String, text: String) {
        val turns = recentTurns().toMutableList()
        turns.add(LumiTurn(role, text.trim()))
        val kept = turns.takeLast(maxTurns)
        val array = JSONArray()
        kept.forEach { turn ->
            array.put(JSONObject().put("role", turn.role).put("text", turn.text))
        }
        prefs.edit().putString(keyTurns, array.toString()).apply()
    }

    @Synchronized
    fun recentTurns(): List<LumiTurn> {
        val raw = prefs.getString(keyTurns, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(LumiTurn(item.optString("role"), item.optString("text")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun contextSummary(): String {
        return recentTurns().takeLast(6).joinToString(" | ") { turn ->
            turn.role + ": " + turn.text
        }
    }

    fun clear() {
        prefs.edit().remove(keyTurns).apply()
    }
}
