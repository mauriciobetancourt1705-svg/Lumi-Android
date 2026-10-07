package com.lumi.android.voice

sealed class LumiTask {
    data class SendMessage(val request: LumiMessageRequest) : LumiTask()
    data class OpenApp(val query: String) : LumiTask()
    data class WebSearch(val query: String) : LumiTask()
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
            Regex("\\s+(?:y luego|después|despues|y después|y despues)\\s+"),
            limit = 2
        )
        if (parts.size == 2) {
            val first = planSingle(parts[0])
            val second = planSingle(parts[1])
            if (first != null && second != null) {
                return LumiTaskPlan(normalized, first + second)
            }
        }
        val single = planSingle(normalized) ?: return null
        return LumiTaskPlan(normalized, single)
    }

    private fun planSingle(text: String): List<LumiTask>? {
        val message = messageEngine.parse(text)
        if (message != null) return listOf(LumiTask.SendMessage(message))

        val lower = text.lowercase()
        if (lower.startsWith("abre ")) {
            return listOf(LumiTask.OpenApp(text.substringAfter("abre ").trim()))
        }
        if (lower.startsWith("busca ")) {
            return listOf(LumiTask.WebSearch(text.substringAfter("busca ").trim()))
        }
        return null
    }
}
