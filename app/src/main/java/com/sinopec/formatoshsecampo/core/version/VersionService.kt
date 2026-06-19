package com.sinopec.formatoshsecampo.core.version

import com.sinopec.formatoshsecampo.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Control de caducidad/trial igual a la lógica base de Supervisión Segura.
 * Primero valida caducidad local y después, si existe URL, control remoto.
 */
class VersionService {
    fun check(callback: (allowed: Boolean, message: String) -> Unit) {
        Thread {
            if (!isBeforeOrSame(BuildConfig.LOCAL_EXPIRES_AT)) {
                callback(false, "La versión ${BuildConfig.VERSION_NAME} caducó el ${BuildConfig.LOCAL_EXPIRES_AT}.")
                return@Thread
            }
            if (BuildConfig.VERSION_CONTROL_URL.isBlank()) {
                callback(true, "")
                return@Thread
            }
            try {
                val conn = URL(BuildConfig.VERSION_CONTROL_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val enabled = obj.optBoolean("enabled", true)
                val minCode = obj.optInt("min_version_code", 1)
                val message = obj.optString("message", "Esta versión ya no está vigente.")
                callback(enabled && BuildConfig.VERSION_CODE >= minCode, message)
            } catch (_: Exception) {
                callback(true, "")
            }
        }.start()
    }

    private fun isBeforeOrSame(date: String): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return today <= date
    }
}
