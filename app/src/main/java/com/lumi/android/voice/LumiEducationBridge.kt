package com.lumi.android.voice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class LumiEducationConfig(val baseUrl: String, val token: String)

class LumiEducationBridge(context: Context) {
    companion object {
        const val DEFAULT_BASE_URL = "https://education-production-d993.up.railway.app"
    }

    private val prefs = context.getSharedPreferences("lumi_education_bridge", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    fun config() = LumiEducationConfig(
        prefs.getString("base_url", DEFAULT_BASE_URL)?.trim()?.trimEnd('/') ?: DEFAULT_BASE_URL,
        prefs.getString("token", "")?.trim() ?: ""
    )

    fun saveConfig(baseUrl: String, token: String) {
        prefs.edit()
            .putString("base_url", baseUrl.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL })
            .putString("token", token.trim())
            .apply()
    }

    fun isConfigured() = config().baseUrl.isNotBlank() && config().token.isNotBlank()

    fun health(callback: (String) -> Unit) {
        request("GET", "/api/v1/health", null, false, callback) { callback("Education está conectado.") }
    }

    /**
     * Envía la conversación al MISMO cerebro de Lumi que utiliza Education.
     * Android aporta voz y acciones; no recrea localmente la personalidad de Lumi.
     */
    fun lumiConversation(
        message: String,
        history: List<LumiTurn>,
        callback: (String) -> Unit
    ) {
        val body = JSONObject().apply {
            put("message", message)
            put("mode", "bienestar")
            put("history", JSONArray().apply {
                history.takeLast(12).forEach { turn ->
                    put(JSONObject().apply {
                        put("role", if (turn.role == "lumi" || turn.role == "model") "model" else "user")
                        put("text", turn.text)
                    })
                }
            })
        }
        request("POST", "/api/v1/ai/tutor", body, true, callback) { json ->
            callback(json.optString("reply", "Education no devolvió una respuesta para Lumi."))
        }
    }

    fun tutor(
        message: String,
        mode: String = "aprender",
        history: List<LumiTurn> = emptyList(),
        callback: (String) -> Unit
    ) {
        val body = JSONObject().apply {
            put("message", message)
            put("mode", mode)
            if (history.isNotEmpty()) {
                put("history", JSONArray().apply {
                    history.takeLast(12).forEach { turn ->
                        put(JSONObject().apply {
                            put("role", if (turn.role == "lumi" || turn.role == "model") "model" else "user")
                            put("text", turn.text)
                        })
                    }
                })
            }
        }
        request("POST", "/api/v1/ai/tutor", body, true, callback) { json ->
            callback(json.optString("reply", "Education no devolvió una respuesta."))
        }
    }

    fun oraculo(message: String, history: List<LumiTurn> = emptyList(), callback: (String) -> Unit) {
        val body = JSONObject().apply {
            put("message", message)
            if (history.isNotEmpty()) {
                put("history", JSONArray().apply {
                    history.takeLast(8).forEach { turn ->
                        put(JSONObject().apply {
                            put("role", if (turn.role == "lumi" || turn.role == "model") "model" else "user")
                            put("text", turn.text)
                        })
                    }
                })
            }
        }
        request("POST", "/api/v1/ai/oraculo", body, true, callback) { json ->
            callback(json.optString("reply", "Oráculo no devolvió una respuesta."))
        }
    }



    /**
     * TTS neural de Lumi usado por Education.
     * Devuelve MP3 generado por /api/v1/ai/lumi-tts.
     */
    fun lumiTts(
        text: String,
        voice: String = "Gacrux",
        callback: (audio: ByteArray, mimeType: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val clean = text.replace(Regex("[*#_]"), "").replace(Regex("\\s+"), " ").trim().take(5000)
        if (clean.isBlank()) {
            onError("text_required")
            return
        }
        val body = JSONObject().apply {
            put("text", clean)
            put("voice", voice)
        }
        requestBytes("POST", "/api/v1/ai/lumi-tts", body, true, callback, onError)
    }

    fun bcvRate(callback: (String) -> Unit) {
        request("GET", "/api/v1/finance/bcv-rate", null, true, callback) { json ->
            val usd = json.optDouble("usd", Double.NaN)
            callback(
                if (usd.isNaN()) "Education no devolvió una tasa válida."
                else "La tasa oficial disponible es %.2f bolívares por dólar.".format(usd)
            )
        }
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject?,
        auth: Boolean,
        callback: (String) -> Unit,
        success: (JSONObject) -> Unit
    ) {
        val cfg = config()
        if (cfg.baseUrl.isBlank()) {
            callback("Configura la conexión con Education en los ajustes de Lumi.")
            return
        }
        if (auth && cfg.token.isBlank()) {
            callback("Lumi necesita una sesión de Education. Configura el token de Education en los ajustes de Lumi.")
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(cfg.baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8000
                    readTimeout = 30000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Lumi-Android/Education-Bridge")
                    if (auth) setRequestProperty("Authorization", "Bearer " + cfg.token)
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.use {
                            it.write(body.toString().toByteArray(Charsets.UTF_8))
                        }
                    }
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }

                if (code in 200..299) {
                    success(json)
                } else {
                    callback(
                        when (code) {
                            401 -> "La sesión de Education no es válida o expiró. Vuelve a conectar Lumi con Education."
                            404 -> "No encontré el motor de Education en esa URL."
                            429 -> "Education está recibiendo muchas solicitudes. Dame un momento y vuelve a intentarlo."
                            else -> json.optString("error")
                                .ifBlank { "Education respondió con HTTP $code." }
                        }
                    )
                }
            } catch (e: Exception) {
                callback("No pude conectar con el cerebro de Education: ${e.message ?: "error de red"}.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun requestBytes(
        method: String,
        path: String,
        body: JSONObject?,
        auth: Boolean,
        callback: (ByteArray, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cfg = config()
        if (cfg.baseUrl.isBlank()) { onError("education_url_missing"); return }
        if (auth && cfg.token.isBlank()) { onError("education_token_missing"); return }
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(cfg.baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8000
                    readTimeout = 30000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Lumi-Android/Education-Bridge")
                    if (auth) setRequestProperty("Authorization", "Bearer " + cfg.token)
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(responseText) }.getOrElse { JSONObject() }
                if (code in 200..299) {
                    val encoded = json.optString("audioBase64", "")
                    if (encoded.isBlank()) { onError("tts_empty_response"); return@execute }
                    val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
                    callback(bytes, json.optString("mimeType", "audio/mpeg"))
                } else {
                    onError(json.optString("error").ifBlank { "education_http_$code" })
                }
            } catch (e: Exception) {
                onError(e.message ?: "education_network_error")
            } finally { connection?.disconnect() }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
