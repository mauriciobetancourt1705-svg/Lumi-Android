package com.lumi.android.voice

class LumiTaskExecutor(
    private val messageExecutor: LumiMessageActionExecutor,
    private val androidExecutor: LumiAndroidActionExecutor
) {
    fun execute(task: LumiTask, onResult: (String) -> Unit) {
        when (task) {
            is LumiTask.Say -> onResult(task.text)
            is LumiTask.OpenApp ->
                androidExecutor.execute(LumiIntent.OpenApp(task.query)) { message, _ -> onResult(message) }
            is LumiTask.WebSearch ->
                androidExecutor.execute(LumiIntent.WebSearch(task.query)) { message, _ -> onResult(message) }
            is LumiTask.SendMessage ->
                onResult("Este paso necesita confirmación antes de enviarse.")
        }
    }

    fun prepareMessage(task: LumiTask.SendMessage): Pair<LumiContact?, String> =
        messageExecutor.prepare(task.request)
}
