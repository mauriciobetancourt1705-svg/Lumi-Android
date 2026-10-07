package com.lumi.android.voice

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class LumiEducationConfig(val baseUrl: String, val token: String)

class LumiEducationBridge(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_education_bridge", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    fun config() = LumiEducationConfig(
        prefs.getString("base_url", "")?.trim()?.trimEnd('/') ?: "",
        prefs.getString("token", "")?.trim() ?: ""
    )

    fun saveConfig(baseUrl: String, token: String) {
        prefs.edit().putString("base_url", baseUrl.trim().trimEnd('/')).putString("token", token.trim()).apply()
    }

    fun isConfigured() = config().baseUrl.isNotBlank() && config().token.isNotBlank()

    fun health(callback: (String) -> Unit) {
        request("GET", "/api/v1/health", null, false, callback) { callback("Education está conectado.") }
    }

    fun tutor(message: String, mode: String = "aprender", callback: (String) -> Unit) {
        val body = JSONObject().apply { put("message", message); put("mode", mode) }
        request("POST", "/api/v1/ai/tutor", body, true, callback) { json -> callback(json.optString("reply", "Education no devolvió una respuesta.")) }
    }

    fun oraculo(message: String, callback: (String) -> Unit) {
        val body = JSONObject().apply { put("message", message) }
        request("POST", "/api/v1/ai/oraculo", body, true, callback) { json -> callback(json.optString("reply", "Oráculo no devolvió una respuesta.")) }
    }

    fun bcvRate(callback: (String) -> Unit) {
        request("GET", "/api/v1/finance/bcv-rate", null, true, callback) { json ->
            val usd = json.optDouble("usd", Double.NaN)
            callback(if (usd.isNaN()) "Education no devolvió una tasa válida." else "La tasa oficial disponible es %.2f bolívares por dólar.".format(usd))
        }
    }

    private fun request(method: String, path: String, body: JSONObject?, auth: Boolean, callback: (String) -> Unit, success: (JSONObject) -> Unit) {
        val cfg = config()
        if (cfg.baseUrl.isBlank()) { callback("Configura primero la URL de Education en los ajustes de Lumi."); return }
        if (auth && cfg.token.isBlank()) { callback("Falta el token de Education. Configúralo en los ajustes de Lumi."); return }
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(cfg.baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json")
                    if (auth) setRequestProperty("Authorization", "Bearer " + cfg.token)
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                if (code in 200..299) success(json)
                else callback(when (code) {
                    401 -> "Education rechazó la sesión. Revisa el token."
                    404 -> "No encontré el endpoint de Education en esa URL."
                    else -> json.optString("error").ifBlank { "Education respondió con HTTP " + code + "." }
                })
            } catch (e: Exception) {
                callback("No pude conectar con Education: " + (e.message ?: "error de red") + ".")
            } finally { connection?.disconnect() }
        }
    }

    fun shutdown() { executor.shutdownNow() }
}
