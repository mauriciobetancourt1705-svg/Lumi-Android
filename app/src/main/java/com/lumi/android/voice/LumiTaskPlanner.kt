package com.lumi.android.voice

sealed class LumiTask {
    data class SendMessage(val request: LumiMessageRequest) : LumiTask()
    data class OpenApp(val query: String) : LumiTask()
    data class WebSearch(val query: String) : LumiTask()
    data class PlayContent(val query: String) : LumiTask()
    data class Say(val text: String) : LumiTask()
}

data class LumiTaskPlan(
    val originalRequest: String,
    val tasks: List<LumiTask>
) {
    val requiresConfirmation: Boolean
        get() = tasks.any { task -> task is LumiTask.SendMessage }
}

class LumiTaskPlanner(private val messageEngine: LumiMessageEngine) {
    fun plan(text: String): LumiTaskPlan? {
        val normalized = text.trim()
        val parts = normalized.split(
            Regex("\\s+(?:y luego|después|despues|y después|y despues|luego)\\s+")
        )
        if (parts.size > 1) {
            val planned = parts.map { planSingle(it) }
            if (planned.all { it != null }) {
                return LumiTaskPlan(normalized, planned.flatMap { it.orEmpty() })
            }
        }
        val single = planSingle(normalized) ?: return null
        return LumiTaskPlan(normalized, single)
    }

    private fun planSingle(text: String): List<LumiTask>? {
        val message = messageEngine.parse(text)
        if (message != null) return listOf(LumiTask.SendMessage(message))

        val lower = text.lowercase()
        val openPrefixes = listOf("abre ", "abrir ", "quiero abrir ", "quiero ir a ", "ir a ", "ve a ", "ve al ", "llévame a ", "llevame a ")
        openPrefixes.firstOrNull { lower.startsWith(it) }?.let {
            return listOf(LumiTask.OpenApp(text.substring(it.length).trim()))
        }
        val playPrefixes = listOf("ponme ", "pon ", "reproduce ", "reprodúceme ", "reproduceme ", "quiero escuchar ", "quiero oír ", "quiero oir ")
        playPrefixes.firstOrNull { lower.startsWith(it) }?.let {
            return listOf(LumiTask.PlayContent(text.substring(it.length).trim()))
        }
        if (lower.startsWith("busca ")) {
            return listOf(LumiTask.WebSearch(text.substringAfter("busca ").trim()))
        }
        return null
    }
}
