package com.lumi.android.voice

class LumiAgentExecutor(
    private val taskExecutor: LumiTaskExecutor,
    private val messageExecutor: LumiMessageActionExecutor,
    private val contactResolver: LumiContactResolver
) {
    data class RunResult(val completed: Int, val total: Int, val message: String)

    fun prepareMessage(task: LumiTask.SendMessage): Pair<LumiContact?, String> =
        taskExecutor.prepareMessage(task)

    fun executeNonMessage(task: LumiTask, onDone: (String) -> Unit) {
        taskExecutor.execute(task, onDone)
    }

    fun sendMessage(task: LumiTask.SendMessage, contact: LumiContact): String =
        messageExecutor.send(task.request, contact)
}
